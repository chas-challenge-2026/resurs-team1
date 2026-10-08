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
import se.comerit.resurs.dto.backoffice.BackOfficeListsDTO;
import se.comerit.resurs.dto.backoffice.CreditApplicationDetails;
import se.comerit.resurs.dto.backoffice.ReviewInfo;
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

import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tester för BackofficeService.
 *
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
                PageRequest.of(0,20),PageRequest.of(0,20));

        assertThat(result).isNotNull();
        assertThat(result.reviewApplications().content()).hasSize(2);
        assertThat(result.decidedApplications().content()).hasSize(2);
    }

    // Ansökningar som väntar på scoring ska synas i granskningslistan så att handläggaren kan köra om scoringen
    @Test
    void applicationsForReview_shouldIncludePendingScoringInReviewList() {
        creditRepo.save(createApplication(ApplicationStatus.UNDER_REVIEW));
        creditRepo.save(createApplication(ApplicationStatus.PENDING_SCORING));
        creditRepo.save(createApplication(ApplicationStatus.APPROVED));

        BackOfficeListsDTO result = backofficeService.applicationsForReview(
                PageRequest.of(0,20),PageRequest.of(0,20));

        assertThat(result.reviewApplications().content())
                .extracting(ReviewInfo::status)
                .containsExactlyInAnyOrder(ApplicationStatus.UNDER_REVIEW, ApplicationStatus.PENDING_SCORING);
        assertThat(result.decidedApplications().content()).hasSize(1);
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


}
