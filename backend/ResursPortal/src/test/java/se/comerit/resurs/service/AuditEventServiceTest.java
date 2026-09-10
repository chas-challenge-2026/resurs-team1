package se.comerit.resurs.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;
import se.comerit.resurs.enums.ApplicationStatus;
import se.comerit.resurs.enums.AuditAction;
import se.comerit.resurs.persistence.AuditEventRepository;
import se.comerit.resurs.persistence.CompanyRepository;
import se.comerit.resurs.persistence.CreditApplicationRepository;
import se.comerit.resurs.persistence.DocumentRepository;
import se.comerit.resurs.persistence.model.AuditEvent;
import se.comerit.resurs.persistence.model.Company;
import se.comerit.resurs.persistence.model.CreditApplication;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tester för AuditService — den enda vägen in i audit_events.
 *
 * Anropar servicen direkt i stället för via de services som utlöser händelserna,
 * så att postens innehåll testas på ett ställe oavsett vem som skriver den.
 */
@Testcontainers
@ActiveProfiles("test")
@SpringBootTest
class AuditEventServiceTest {

    private static final String ORG_NUMBER_PREFIX = "TEST-";
    private static final String WORKER_EMAIL = "karin@resurs.se";
    private static final String WORKER_NAME = "Karin Handläggare";

    private static final Path SEED_SQL = Paths.get("").toAbsolutePath()
            .resolve("../../infra/seed.sql")
            .normalize();

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("resurs_test")
                    .withUsername("test")
                    .withPassword("test")
                    .withCopyFileToContainer(
                            MountableFile.forHostPath(SEED_SQL),
                            "/docker-entrypoint-initdb.d/seed.sql"
                    );

    @Autowired
    private AuditService auditService;

    @Autowired
    private AuditEventRepository auditEventRepo;

    @Autowired
    private CreditApplicationRepository creditRepo;

    @Autowired
    private DocumentRepository documentRepo;

    @Autowired
    private CompanyRepository companyRepo;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void cleanDatabase() {
        // audit_events har FK mot applications och måste tömmas först
        auditEventRepo.deleteAll();
        documentRepo.deleteAll();
        creditRepo.deleteAll();
        companyRepo.deleteAll();
    }

    // ============================================================
    // APPLICATION_CREATED
    // ============================================================

    @Test
    void applicationCreated_writesEventWithCompanyAsActor() {
        CreditApplication application = savedApplication(ApplicationStatus.PENDING_DOCS);

        auditService.applicationCreated(application);

        AuditEvent event = onlyEventFor(application);

        assertThat(event.getAction()).isEqualTo(AuditAction.APPLICATION_CREATED);
        assertThat(event.getActor()).isEqualTo(application.getCompany().getOrg_number());
        assertThat(dataOf(event))
                .containsEntry("actorType", "COMPANY")
                .containsEntry("purpose", "Test loan");
    }

    // ============================================================
    // SCORING_RUN
    // ============================================================

    @Test
    void scoringRun_writesEventWithSystemAsActor() {
        CreditApplication application = savedApplication(ApplicationStatus.UNDER_REVIEW);
        application.setDecision("REVIEW");

        auditService.scoringRun(application, 2);

        AuditEvent event = onlyEventFor(application);

        assertThat(event.getAction()).isEqualTo(AuditAction.SCORING_RUN);
        assertThat(event.getActor()).isEqualTo("SYSTEM");
        assertThat(dataOf(event))
                .containsEntry("actorType", "SYSTEM")
                .containsEntry("decision", "REVIEW")
                .containsEntry("newStatus", "UNDER_REVIEW")
                .containsEntry("flags", 2);
    }

    // ============================================================
    // DOCUMENT_UPLOADED
    // ============================================================

    @Test
    void documentUploaded_writesFilenameAndDocType() {
        CreditApplication application = savedApplication(ApplicationStatus.PENDING_DOCS);

        auditService.documentUploaded(application, "balansrakning.pdf", "balansrakning");

        AuditEvent event = onlyEventFor(application);

        assertThat(event.getAction()).isEqualTo(AuditAction.DOCUMENT_UPLOADED);
        assertThat(event.getActor()).isEqualTo(application.getCompany().getOrg_number());
        assertThat(dataOf(event))
                .containsEntry("actorType", "COMPANY")
                .containsEntry("filename", "balansrakning.pdf")
                .containsEntry("docType", "balansrakning");
    }

    // ============================================================
    // MANUAL_DECISION
    // ============================================================

    @Test
    void manualDecision_recordsPreviousAndNewStatus() {
        CreditApplication application = savedApplication(ApplicationStatus.UNDER_REVIEW);
        application.setStatus(ApplicationStatus.APPROVED);

        auditService.manualDecision(application, WORKER_EMAIL, WORKER_NAME,
                ApplicationStatus.UNDER_REVIEW, "Looks good");

        AuditEvent event = onlyEventFor(application);

        assertThat(event.getAction()).isEqualTo(AuditAction.MANUAL_DECISION);
        assertThat(event.getActor()).isEqualTo(WORKER_EMAIL);
        assertThat(dataOf(event))
                .containsEntry("actorType", "CASE_WORKER")
                .containsEntry("workerName", WORKER_NAME)
                .containsEntry("previousStatus", "UNDER_REVIEW")
                .containsEntry("newStatus", "APPROVED")
                .containsEntry("comment", "Looks good");
    }

    @Test
    void manualDecision_emptyComment_omitsCommentField() {
        CreditApplication application = savedApplication(ApplicationStatus.UNDER_REVIEW);
        application.setStatus(ApplicationStatus.APPROVED);

        auditService.manualDecision(application, WORKER_EMAIL, WORKER_NAME,
                ApplicationStatus.UNDER_REVIEW, "");

        assertThat(dataOf(onlyEventFor(application))).doesNotContainKey("comment");
    }

    @Test
    void manualDecision_nullComment_omitsCommentField() {
        CreditApplication application = savedApplication(ApplicationStatus.UNDER_REVIEW);
        application.setStatus(ApplicationStatus.REJECTED);

        auditService.manualDecision(application, WORKER_EMAIL, WORKER_NAME,
                ApplicationStatus.UNDER_REVIEW, null);

        assertThat(dataOf(onlyEventFor(application))).doesNotContainKey("comment");
    }

    // ============================================================
    // Kedjan: sekvensnummer, id och schemaversion
    // ============================================================

    @Test
    void sequenceNumber_startsAtOneAndIncrements() {
        CreditApplication application = savedApplication(ApplicationStatus.PENDING_DOCS);

        auditService.applicationCreated(application);
        auditService.scoringRun(application, 0);
        auditService.documentUploaded(application, "arsredovisning.pdf", "arsredovisning");

        List<AuditEvent> events =
                auditEventRepo.findByApplicationIdOrderBySequenceNumberAsc(application.getId());

        assertThat(events).extracting(AuditEvent::getSequenceNumber)
                .containsExactly(1L, 2L, 3L);

        assertThat(events).extracting(AuditEvent::getAction)
                .containsExactly(
                        AuditAction.APPLICATION_CREATED,
                        AuditAction.SCORING_RUN,
                        AuditAction.DOCUMENT_UPLOADED);
    }

    @Test
    void sequenceNumber_isPerApplication() {
        CreditApplication first = savedApplication(ApplicationStatus.PENDING_DOCS);
        CreditApplication second = savedApplication(ApplicationStatus.PENDING_DOCS);

        auditService.applicationCreated(first);
        auditService.applicationCreated(second);

        assertThat(onlyEventFor(first).getSequenceNumber()).isEqualTo(1L);
        assertThat(onlyEventFor(second).getSequenceNumber()).isEqualTo(1L);
    }

    @Test
    void event_hasGeneratedIdTimestampAndSchemaVersion() {
        CreditApplication application = savedApplication(ApplicationStatus.PENDING_DOCS);

        auditService.applicationCreated(application);

        AuditEvent event = onlyEventFor(application);

        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getOccurredAt()).isNotNull();
        assertThat(event.getSchemaVersion()).isEqualTo(1);
    }

    @Test
    void signingFields_areNullUntilSigningModuleExists() {
        CreditApplication application = savedApplication(ApplicationStatus.PENDING_DOCS);

        auditService.applicationCreated(application);

        AuditEvent event = onlyEventFor(application);

        assertThat(event.getPreviousHash()).isNull();
        assertThat(event.getSigningKeyId()).isNull();
        assertThat(event.getSignature()).isNull();
    }

    // ============================================================
    // Läsning
    // ============================================================

    @Test
    void findByApplicationId_returnsOnlyThatApplicationsEvents() {
        CreditApplication first = savedApplication(ApplicationStatus.PENDING_DOCS);
        CreditApplication second = savedApplication(ApplicationStatus.PENDING_DOCS);

        auditService.applicationCreated(first);
        auditService.scoringRun(first, 1);
        auditService.applicationCreated(second);

        assertThat(auditEventRepo.findByApplicationIdOrderBySequenceNumberAsc(first.getId()))
                .hasSize(2);
        assertThat(auditEventRepo.findByApplicationIdOrderBySequenceNumberAsc(second.getId()))
                .hasSize(1);
    }

    @Test
    void findByApplicationId_unknownApplication_returnsEmptyList() {
        assertThat(auditEventRepo.findByApplicationIdOrderBySequenceNumberAsc(999999L))
                .isEmpty();
    }

    // ============================================================
    // Testdata
    // ============================================================

    private CreditApplication savedApplication(ApplicationStatus status) {
        Company company = new Company();
        company.setOrg_number(ORG_NUMBER_PREFIX + UUID.randomUUID()
                .toString()
                .substring(0, 15));
        company.setCompany_name("Test Company");
        company.setAuthorized_signatory("Test Signatory");

        Company savedCompany = companyRepo.save(company);

        CreditApplication application = new CreditApplication();
        application.setCompany(savedCompany);
        application.setRequestedAmount(new BigDecimal("10000.00"));
        application.setPurpose("Test loan");
        application.setStatus(status);

        return creditRepo.save(application);
    }

    /**
     * Postgres normaliserar jsonb — nyckelordningen ändras och mellanslag läggs
     * till. Därför parsas fältet i stället för att jämföras som text.
     */
    private Map<String, Object> dataOf(AuditEvent event) {
        try {
            return objectMapper.readValue(event.getData(), new TypeReference<>() {
            });
        } catch (JacksonException e) {
            throw new IllegalStateException("Ogiltig JSON i audit_events.data", e);
        }
    }

    private AuditEvent onlyEventFor(CreditApplication application) {
        List<AuditEvent> events =
                auditEventRepo.findByApplicationIdOrderBySequenceNumberAsc(application.getId());

        assertThat(events).hasSize(1);
        return events.get(0);
    }
}
