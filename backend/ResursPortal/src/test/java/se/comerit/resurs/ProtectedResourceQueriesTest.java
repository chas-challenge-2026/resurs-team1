package se.comerit.resurs;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
import se.comerit.resurs.persistence.model.Document;
import se.comerit.resurs.util.HMACHasher;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tester för repository-metoderna som filtrerar på protectedResource (issue #385).
 * Skyddade ärenden ska hoppas över i databasfrågan, inte efter att de har hämtats.
 */
@Testcontainers
@ActiveProfiles("test")
@SpringBootTest
class ProtectedResourceQueriesTest {

    private static final String ORG_NUMBER = "556000-0001";

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
    private CreditApplicationRepository creditRepo;

    @Autowired
    private CompanyRepository companyRepo;

    @Autowired
    private DocumentRepository documentRepo;

    @Autowired
    private AuditEventRepository auditEventRepo;

    @Autowired
    private HMACHasher hasher;

    private Company company;

    @BeforeEach
    void cleanDatabase() {
        // audit_events och documents har FK mot applications och måste tömmas först
        auditEventRepo.deleteAll();
        documentRepo.deleteAll();
        creditRepo.deleteAll();
        companyRepo.deleteAll();

        company = new Company();
        company.setOrg_number(ORG_NUMBER);
        company.setCompany_name("Test Company");
        company.setAuthorized_signatory("Test Signatory");
        company = companyRepo.save(company);
    }

    @Test
    void findByStatusIn_shouldSkipProtectedApplications() {
        CreditApplication normal = save(ApplicationStatus.UNDER_REVIEW, false);
        save(ApplicationStatus.UNDER_REVIEW, true);

        Page<CreditApplication> result =
                creditRepo.findByStatusInAndProtectedResourceFalseOrderByCreatedAtAsc(
                        List.of(ApplicationStatus.UNDER_REVIEW), PageRequest.of(0, 10));

        assertThat(result.getContent()).extracting(CreditApplication::getId)
                .containsExactly(normal.getId());
    }

    // Filtret ska ligga i databasfrågan, annars blir sidorna kortare än sidstorleken
    @Test
    void findByStatusIn_shouldKeepPagingCorrectWhenProtectedApplicationsExist() {
        save(ApplicationStatus.UNDER_REVIEW, false);
        save(ApplicationStatus.UNDER_REVIEW, true);
        save(ApplicationStatus.UNDER_REVIEW, false);
        save(ApplicationStatus.UNDER_REVIEW, true);
        save(ApplicationStatus.UNDER_REVIEW, false);

        Page<CreditApplication> firstPage =
                creditRepo.findByStatusInAndProtectedResourceFalseOrderByCreatedAtAsc(
                        List.of(ApplicationStatus.UNDER_REVIEW), PageRequest.of(0, 2));

        assertThat(firstPage.getContent()).hasSize(2);
        assertThat(firstPage.getTotalElements()).isEqualTo(3);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
    }

    @Test
    void findByOrgNumber_shouldSkipProtectedApplications() {
        CreditApplication normal = save(ApplicationStatus.UNDER_REVIEW, false);
        save(ApplicationStatus.UNDER_REVIEW, true);

        List<CreditApplication> result =
                creditRepo.findByCompany_SearchKeyOrgNumberAndProtectedResourceFalseOrderByCreatedAtDesc(
                        hasher.encode(ORG_NUMBER), PageRequest.of(0, 10));

        assertThat(result).extracting(CreditApplication::getId)
                .containsExactly(normal.getId());
    }

    @Test
    void existsByIdAndProtectedResourceTrue_shouldOnlyBeTrueForProtectedApplication() {
        CreditApplication normal = save(ApplicationStatus.UNDER_REVIEW, false);
        CreditApplication protectedApplication = save(ApplicationStatus.UNDER_REVIEW, true);

        assertThat(creditRepo.existsByIdAndProtectedResourceTrue(protectedApplication.getId())).isTrue();
        assertThat(creditRepo.existsByIdAndProtectedResourceTrue(normal.getId())).isFalse();
        assertThat(creditRepo.existsByIdAndProtectedResourceTrue(999_999L)).isFalse();
    }

    @Test
    void auditEvents_shouldBeEmptyForProtectedApplicationAndPresentForNormalOne() {
        CreditApplication normal = save(ApplicationStatus.UNDER_REVIEW, false);
        CreditApplication protectedApplication = save(ApplicationStatus.UNDER_REVIEW, true);
        auditEventRepo.save(auditEvent(normal));
        auditEventRepo.save(auditEvent(protectedApplication));

        assertThat(auditEventRepo
                .findByApplicationIdAndApplicationProtectedResourceFalseOrderBySequenceNumberAsc(normal.getId()))
                .hasSize(1);
        assertThat(auditEventRepo
                .findByApplicationIdAndApplicationProtectedResourceFalseOrderBySequenceNumberAsc(
                        protectedApplication.getId()))
                .isEmpty();
    }

    @Test
    void documents_shouldBeEmptyForProtectedApplicationAndPresentForNormalOne() {
        CreditApplication normal = save(ApplicationStatus.UNDER_REVIEW, false);
        CreditApplication protectedApplication = save(ApplicationStatus.UNDER_REVIEW, true);
        documentRepo.save(document(normal));
        documentRepo.save(document(protectedApplication));

        assertThat(documentRepo.findByApplicationIdAndApplicationProtectedResourceFalse(normal.getId()))
                .hasSize(1);
        assertThat(documentRepo.findByApplicationIdAndApplicationProtectedResourceFalse(
                protectedApplication.getId()))
                .isEmpty();
    }

    private CreditApplication save(ApplicationStatus status, boolean protectedResource) {
        CreditApplication application = new CreditApplication();
        application.setCompany(company);
        application.setRequestedAmount(new BigDecimal("10000.00"));
        application.setPurpose("Test loan");
        application.setStatus(status);
        application.setProtectedResource(protectedResource);
        return creditRepo.save(application);
    }

    private AuditEvent auditEvent(CreditApplication application) {
        return new AuditEvent(application, 1L, AuditAction.COMMENT_ADDED, "test-worker",
                null, null, null, null, null);
    }

    private Document document(CreditApplication application) {
        Document document = new Document();
        document.setApplication(application);
        document.setFilename("income.pdf");
        document.setDoc_type("INCOME_STATEMENT");
        document.setUploadedAt(Instant.now());
        return document;
    }
}