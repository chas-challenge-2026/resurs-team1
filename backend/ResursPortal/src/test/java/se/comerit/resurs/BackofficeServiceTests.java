package se.comerit.resurs;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;
import se.comerit.resurs.dto.AuditEventDTO;
import se.comerit.resurs.dto.backoffice.BackOfficeListsDTO;
import se.comerit.resurs.dto.backoffice.CreditApplicationDetails;
import se.comerit.resurs.enums.ApplicationStatus;
import se.comerit.resurs.enums.AuditAction;
import se.comerit.resurs.persistence.AuditEventRepository;
import se.comerit.resurs.persistence.CompanyRepository;
import se.comerit.resurs.persistence.CreditApplicationRepository;
import se.comerit.resurs.persistence.DocumentRepository;
import se.comerit.resurs.persistence.model.AuditEvent;
import se.comerit.resurs.persistence.model.Company;
import se.comerit.resurs.persistence.model.CreditApplication;
import se.comerit.resurs.persistence.model.Document;
import se.comerit.resurs.service.BackofficeService;
import se.comerit.resurs.dto.backoffice.ReviewInfo;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tester för BackofficeService.
 * <p>
 * Verifierar handläggarflödet: beslut, listor och detaljvy. Att en audit-post
 * skrivs kontrolleras här, men postens innehåll testas i AuditEventServiceTest.
 */
@Testcontainers
@ActiveProfiles("test")
@SpringBootTest
class BackofficeServiceTests {

    private static final String WORKER_EMAIL = "karin@resurs.se";
    private static final String WORKER_NAME = "test-worker";

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
    private BackofficeService backofficeService;

    @Autowired
    private CreditApplicationRepository creditRepo;

    @Autowired
    private DocumentRepository documentRepo;

    @Autowired
    private CompanyRepository companyRepo;

    @Autowired
    private AuditEventRepository auditEventRepo;

    @BeforeEach
    void cleanDatabase() {
        // audit_events har FK mot applications och måste tömmas först
        auditEventRepo.deleteAll();
        documentRepo.deleteAll();
        creditRepo.deleteAll();
        companyRepo.deleteAll();
    }

    @Test
    void applicationDecision_shouldApproveApplication() {
        CreditApplication saved =
                creditRepo.save(createApplication(ApplicationStatus.UNDER_REVIEW));

        backofficeService.application_decision(
                saved.getId(),
                ApplicationStatus.APPROVED,
                WORKER_EMAIL,
                WORKER_NAME,
                "Application looks good"
        );

        CreditApplication updated =
                creditRepo.findById(saved.getId()).orElseThrow();

        assertThat(updated.getStatus()).isEqualTo(ApplicationStatus.APPROVED);
        assertThat(updated.getDecision()).isEqualTo("APPROVED");
        assertThat(updated.getUpdatedAt()).isNotNull();
    }

    @Test
    void applicationDecision_shouldRejectApplication() {
        CreditApplication saved =
                creditRepo.save(createApplication(ApplicationStatus.UNDER_REVIEW));

        backofficeService.application_decision(
                saved.getId(),
                ApplicationStatus.REJECTED,
                WORKER_EMAIL,
                WORKER_NAME,
                "Insufficient score"
        );

        CreditApplication updated =
                creditRepo.findById(saved.getId()).orElseThrow();

        assertThat(updated.getStatus()).isEqualTo(ApplicationStatus.REJECTED);
        assertThat(updated.getDecision()).isEqualTo("REJECTED");
        assertThat(updated.getUpdatedAt()).isNotNull();
    }

    @Test
    void applicationDecision_shouldWriteAuditEvent() {
        CreditApplication saved =
                creditRepo.save(createApplication(ApplicationStatus.UNDER_REVIEW));

        backofficeService.application_decision(
                saved.getId(),
                ApplicationStatus.APPROVED,
                WORKER_EMAIL,
                WORKER_NAME,
                "Application looks good"
        );

        assertThat(auditEventRepo.findByApplicationIdOrderBySequenceNumberAsc(saved.getId()))
                .extracting(AuditEvent::getAction)
                .containsExactly(AuditAction.MANUAL_DECISION);
    }

    @Test
    void applicationDecision_shouldThrowWhenApplicationDoesNotExist() {
        assertThatThrownBy(() ->
                backofficeService.application_decision(
                        999999L,
                        ApplicationStatus.APPROVED,
                        WORKER_EMAIL,
                        WORKER_NAME,
                        "Test"
                )
        ).isInstanceOf(java.util.NoSuchElementException.class);

        assertThat(auditEventRepo.count()).isZero();
    }

    @Test
    void applicationsForReview_shouldReturnCorrectApplications() {
        creditRepo.save(createApplication(ApplicationStatus.UNDER_REVIEW));
        creditRepo.save(createApplication(ApplicationStatus.APPROVED));
        creditRepo.save(createApplication(ApplicationStatus.REJECTED));
        creditRepo.save(createApplication(ApplicationStatus.UNDER_REVIEW));

        BackOfficeListsDTO result = backofficeService.applicationsForReview(
                PageRequest.of(0, 20), PageRequest.of(0, 20));

        assertThat(result).isNotNull();
        assertThat(result.reviewApplications().content()).hasSize(2);
        assertThat(result.decidedApplications().content()).hasSize(2);
    }

    @Test
    void applicationDetails_shouldReturnApplicationAndDocuments() {
        CreditApplication saved =
                creditRepo.save(createApplication(ApplicationStatus.UNDER_REVIEW));

        Document document = new Document();
        document.setApplication(saved);
        document.setFilename("income.pdf");
        document.setDoc_type("INCOME_STATEMENT");
        document.setUploadedAt(Instant.now());

        documentRepo.save(document);

        CreditApplicationDetails result =
                backofficeService.application_details(saved.getId());

        assertThat(result).isNotNull();
        assertThat(result.application()).isNotNull();
        assertThat(result.documents()).hasSize(1);

        assertThat(result.documents().getFirst().filename())
                .isEqualTo("income.pdf");

        assertThat(result.documents().getFirst().docType())
                .isEqualTo("INCOME_STATEMENT");

        assertThat(result.documents().getFirst().applicationId())
                .isEqualTo(saved.getId());
    }

    @Test
    void applicationDetails_shouldReturnEmptyDocumentsWhenNoneExist() {
        CreditApplication saved =
                creditRepo.save(createApplication(ApplicationStatus.UNDER_REVIEW));

        CreditApplicationDetails result =
                backofficeService.application_details(saved.getId());

        assertThat(result).isNotNull();
        assertThat(result.application()).isNotNull();
        assertThat(result.documents()).isEmpty();
    }


    private CreditApplication createApplication(ApplicationStatus status) {
        Company company = new Company();
        company.setOrg_number("TEST-" + UUID.randomUUID()
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

        return application;
    }

    // Kontrollerar att handläggarens kommentar sparas på ärendet när ett beslut fattas
    @Test
    void applicationDecision_shouldSaveCommentOnApplication() {
        CreditApplication saved = creditRepo.save(createApplication(ApplicationStatus.UNDER_REVIEW));

        backofficeService.application_decision(saved.getId(),
                ApplicationStatus.REJECTED, WORKER_EMAIL, WORKER_NAME, "Soliditeten är för låg");
        CreditApplication updated = creditRepo.findById(saved.getId()).orElseThrow();
        assertThat(updated.getComment()).isEqualTo("Soliditeten är för låg");
    }

    // Kontrollerar att en tom kommentar (mellanslag här) sparas som null istället för tom text
    @Test
    void applicationDecision_shouldStoreNullWhenCommentIsBlank() {
        CreditApplication saved = creditRepo.save(createApplication(ApplicationStatus.UNDER_REVIEW));

        backofficeService.application_decision(saved.getId(),
                ApplicationStatus.APPROVED, WORKER_EMAIL, WORKER_NAME, "   ");
        assertThat(creditRepo.findById(saved.getId()).orElseThrow().getComment()).isNull();
    }

    // Kontroller att en kommentar sparas på ärendet och att statusen inte ändras
    @Test
    void applicationComment_shouldSaveCommentAndKeepStatus() {
        CreditApplication saved = creditRepo.save(createApplication(ApplicationStatus.UNDER_REVIEW));

        backofficeService.application_comment(saved.getId(), "Du saknar underlag", WORKER_EMAIL, WORKER_NAME);

        CreditApplication updated = creditRepo.findById(saved.getId()).orElseThrow();
        assertThat(updated.getComment()).isEqualTo("Du saknar underlag");
        assertThat(updated.getStatus()).isEqualTo(ApplicationStatus.UNDER_REVIEW);
    }

    // Kontrollerar atte n COMMENT_ADDED händelse skrivs i auditloggen när en kommentar läggs till
    @Test
    void applicationComment_shouldWriteAuditEvent() {
        CreditApplication saved = creditRepo.save(createApplication(ApplicationStatus.UNDER_REVIEW));

        backofficeService.application_comment(saved.getId(), "Hej", WORKER_EMAIL, WORKER_NAME);

        assertThat(auditEventRepo.findByApplicationIdOrderBySequenceNumberAsc(saved.getId()))
                .extracting(AuditEvent::getAction)
                .containsExactly(AuditAction.COMMENT_ADDED);
    }

    // Kontrollerar att ett ärende som inte finns ger NoSuchElementException
    @Test
    void applicationComment_shouldThrowWhenApplicationDoesNotExist() {
        assertThatThrownBy(() -> backofficeService.application_comment(
                999999L, "Hej", WORKER_EMAIL, WORKER_NAME))
                .isInstanceOf(java.util.NoSuchElementException.class);
    }

    //Ska kontrollera att hela auditloggen för ett ärende hämtas, äldsta händelsen först
    //Händelserna skapas via riktiga metoder (som @Transactional) alltså samma väg som en handläggare använder
    @Test
    void applicationAuditLog_shouldReturnEventsInOrder() {
        CreditApplication saved = creditRepo.save(createApplication(ApplicationStatus.UNDER_REVIEW));
        backofficeService.application_comment(saved.getId(), "Första", WORKER_EMAIL, WORKER_NAME);
        backofficeService.application_decision(saved.getId(), ApplicationStatus.APPROVED,
                WORKER_EMAIL, WORKER_NAME, "Andra");

        List<AuditEventDTO> log = backofficeService.application_audit_log(saved.getId());

        assertThat(log).extracting(AuditEventDTO::action)
                .containsExactly(AuditAction.COMMENT_ADDED, AuditAction.MANUAL_DECISION);
        assertThat(log).extracting(AuditEventDTO::sequenceNumber)
                .containsExactly(1L, 2L);
    }

    //Ett ärende som inte har några händelser ska ge en tom lista (inte ett fel)
    @Test
    void applicationAuditLog_shouldReturnEmptyListWhenNoEvents() {
        CreditApplication saved = creditRepo.save(createApplication(ApplicationStatus.UNDER_REVIEW));

        assertThat(backofficeService.application_audit_log(saved.getId())).isEmpty();
    }

    //Auditloggen ska bara innehålla händelserna för ärendet man frågar efter, inte andra ärenden
    @Test
    void applicationAuditLog_shouldOnlyReturnEventsForThatApplication() {
        CreditApplication first = creditRepo.save(createApplication(ApplicationStatus.UNDER_REVIEW));
        CreditApplication second = creditRepo.save(createApplication(ApplicationStatus.UNDER_REVIEW));
        backofficeService.application_comment(first.getId(), "Hej", WORKER_EMAIL, WORKER_NAME);

        assertThat(backofficeService.application_audit_log(first.getId())).hasSize(1);
        assertThat(backofficeService.application_audit_log(second.getId())).isEmpty();
    }

    //Ett ärende som inte finns ska ge NoSuchElementException (404), inte en tom lista
    @Test
    void applicationAuditLog_shouldThrowWhenApplicationDoesNotExist() {
        assertThatThrownBy(() -> backofficeService.application_audit_log(999999L))
                .isInstanceOf(java.util.NoSuchElementException.class);
    }

    //---------------
    //Kontrollerar att en begäran om komplettering sätter status PENDING_DOCS och sparar texten på ärendet
    @Test
    void requestDocuments_shouldSetPendingDocsAndSaveComment() {
        CreditApplication saved = creditRepo.save(createApplication(ApplicationStatus.UNDER_REVIEW));

        backofficeService.application_request_documents(saved.getId(),
                "Skicka kontoutdrag", WORKER_EMAIL, WORKER_NAME);

        CreditApplication updated = creditRepo.findById(saved.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(ApplicationStatus.PENDING_DOCS);
        assertThat(updated.getComment()).isEqualTo("Skicka kontoutdrag");
    }

    // Kontrollerar att en DOCUMENTS_REQUESTED-händelse skrivs i auditloggen
    @Test
    void requestDocuments_shouldWriteAuditEvent() {
        CreditApplication saved = creditRepo.save(createApplication(ApplicationStatus.UNDER_REVIEW));

        backofficeService.application_request_documents(saved.getId(),
                "Skicka kontoutdrag", WORKER_EMAIL, WORKER_NAME);

        assertThat(auditEventRepo.findByApplicationIdOrderBySequenceNumberAsc(saved.getId()))
                .extracting(AuditEvent::getAction)
                .containsExactly(AuditAction.DOCUMENTS_REQUESTED);
    }

    // Ett godkänt ärende kan inte kompletteras, och ska vara orört
    @Test
    void requestDocuments_shouldThrowWhenApplicationIsApproved() {
        CreditApplication saved = creditRepo.save(createApplication(ApplicationStatus.APPROVED));

        assertThatThrownBy(() -> backofficeService.application_request_documents(
                saved.getId(), "Skicka kontoutdrag", WORKER_EMAIL, WORKER_NAME))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already has a decision");

        assertThat(creditRepo.findById(saved.getId()).orElseThrow().getStatus())
                .isEqualTo(ApplicationStatus.APPROVED);
        assertThat(auditEventRepo.findByApplicationIdOrderBySequenceNumberAsc(saved.getId())).isEmpty();
    }

    // Samma för ett avslaget ärende
    @Test
    void requestDocuments_shouldThrowWhenApplicationIsRejected() {
        CreditApplication saved = creditRepo.save(createApplication(ApplicationStatus.REJECTED));

        assertThatThrownBy(() -> backofficeService.application_request_documents(
                saved.getId(), "Skicka kontoutdrag", WORKER_EMAIL, WORKER_NAME))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(creditRepo.findById(saved.getId()).orElseThrow().getStatus())
                .isEqualTo(ApplicationStatus.REJECTED);
    }

    // Ett ärende som inte finns ska ge NoSuchElementException (404)
    @Test
    void requestDocuments_shouldThrowWhenApplicationDoesNotExist() {
        assertThatThrownBy(() -> backofficeService.application_request_documents(
                999999L, "Skicka kontoutdrag", WORKER_EMAIL, WORKER_NAME))
                .isInstanceOf(java.util.NoSuchElementException.class);
    }

    // En ny begäran på ett ärende som redan väntar på dokument är tillåten och ersätter texten
    @Test
    void requestDocuments_shouldAllowNewRequestWhenAlreadyPendingDocs() {
        CreditApplication saved = creditRepo.save(createApplication(ApplicationStatus.PENDING_DOCS));

        backofficeService.application_request_documents(saved.getId(), "Även kontoutdrag", WORKER_EMAIL, WORKER_NAME);

        CreditApplication updated = creditRepo.findById(saved.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(ApplicationStatus.PENDING_DOCS);
        assertThat(updated.getComment()).isEqualTo("Även kontoutdrag");
    }

    // Granskningslistan ska visa både ärenden under granskning och de som väntar på dokument, men inte avgjorda
    @Test
    void applicationsForReview_shouldIncludePendingDocs() {
        creditRepo.save(createApplication(ApplicationStatus.UNDER_REVIEW));
        creditRepo.save(createApplication(ApplicationStatus.PENDING_DOCS));
        creditRepo.save(createApplication(ApplicationStatus.APPROVED));

        BackOfficeListsDTO result = backofficeService.applicationsForReview(
                PageRequest.of(0, 20), PageRequest.of(0, 20));

        assertThat(result.reviewApplications().content())
                .extracting(ReviewInfo::status)
                .containsExactlyInAnyOrder(ApplicationStatus.UNDER_REVIEW, ApplicationStatus.PENDING_DOCS);
    }
    //---------------

}
