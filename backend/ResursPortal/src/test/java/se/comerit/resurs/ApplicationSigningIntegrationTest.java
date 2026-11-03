package se.comerit.resurs;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;
import se.comerit.resurs.dto.ContactDetails;
import se.comerit.resurs.dto.CreditApplicationDataObject;
import se.comerit.resurs.dto.application.NewApplicationDTO;
import se.comerit.resurs.dto.companyvalidation.CompanyValidationApiDTO;
import se.comerit.resurs.enums.ApplicationStatus;
import se.comerit.resurs.enums.SigningRight;
import se.comerit.resurs.exception.companyvalidation.CompanyValidationFailedException;
import se.comerit.resurs.persistence.ApplicationSignatureRepository;
import se.comerit.resurs.persistence.AuditEventRepository;
import se.comerit.resurs.persistence.CompanyRepository;
import se.comerit.resurs.persistence.CreditApplicationRepository;
import se.comerit.resurs.persistence.DocumentRepository;
import se.comerit.resurs.persistence.SignatoryRepository;
import se.comerit.resurs.persistence.model.CreditApplication;
import se.comerit.resurs.service.ApplicationService;
import se.comerit.resurs.service.ApplicationSigningService;
import se.comerit.resurs.service.BackofficeService;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tester för firmateckning i förening.
 * <p>
 * Bolaget 556000-2222 finns i den mockade registerdatan och har två firmatecknare som tecknar i förening.
 * En ansökan som annars blivit APPROVED ska vänta i PENDING_CO_SIGNATURE tills båda har signerat.
 */
@Testcontainers
@ActiveProfiles("test")
@SpringBootTest
class ApplicationSigningIntegrationTest {

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

    private static final String JOINT_ORG_NUMBER = "556000-2222";
    private static final String ALONE_ORG_NUMBER = "556000-1234";
    private static final String WORKER_EMAIL = "karin@resurs.se";
    private static final String WORKER_NAME = "Karin";
    private static final ContactDetails CONTACTS = new ContactDetails("Sara Lind", "sara@duo.se", "0701234567");

    private static final CompanyValidationApiDTO.Signatory SARA =
            new CompanyValidationApiDTO.Signatory("790505-2222", "Sara Lind", "Styrelseordförande", SigningRight.JOINTLY);
    private static final String PETER_PERSONAL_NUMBER = "810808-2222";
    private static final CompanyValidationApiDTO.Signatory ANDERS =
            new CompanyValidationApiDTO.Signatory("750312-1234", "Anders Karlsson", "Styrelseordförande", SigningRight.ALONE);

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private ApplicationSigningService signingService;

    @Autowired
    private BackofficeService backofficeService;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private ApplicationSignatureRepository signatureRepository;

    @Autowired
    private SignatoryRepository signatoryRepository;

    @Autowired
    private CreditApplicationRepository applicationRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @BeforeEach
    void setUp() {
        auditEventRepository.deleteAll();
        documentRepository.deleteAll();
        signatureRepository.deleteAll();
        signatoryRepository.deleteAll();
        applicationRepository.deleteAll();
        companyRepository.deleteAll();
    }

    // ============================================================
    // saveApplication()
    // ============================================================

    @Test
    void saveApplication_whenSignsAlone_keepsApprovedStatus() {
        CreditApplicationDataObject saved = submit(ALONE_ORG_NUMBER, ApplicationStatus.APPROVED, ANDERS);

        assertThat(saved.status()).isEqualTo(ApplicationStatus.APPROVED);
        assertThat(signatureRepository.countByApplicationId(saved.id())).isEqualTo(1);
    }

    @Test
    void saveApplication_whenSignsJointlyAndApproved_waitsForCoSignature() {
        CreditApplicationDataObject saved = submit(JOINT_ORG_NUMBER, ApplicationStatus.APPROVED, SARA);

        CreditApplication fromDatabase = applicationRepository.findById(saved.id()).orElseThrow();
        assertThat(fromDatabase.getStatus()).isEqualTo(ApplicationStatus.PENDING_CO_SIGNATURE);
        assertThat(fromDatabase.isRequiresCoSignature()).isTrue();
        assertThat(signatureRepository.countByApplicationId(saved.id())).isEqualTo(1);
    }

    @Test
    void saveApplication_whenSignsJointlyAndUnderReview_keepsUnderReview() {
        CreditApplicationDataObject saved = submit(JOINT_ORG_NUMBER, ApplicationStatus.UNDER_REVIEW, SARA);

        assertThat(saved.status()).isEqualTo(ApplicationStatus.UNDER_REVIEW);
    }

    @Test
    void saveApplication_whenSignsJointlyAndRejected_isRejectedDirectly() {
        CreditApplicationDataObject saved = submit(JOINT_ORG_NUMBER, ApplicationStatus.REJECTED, SARA);

        assertThat(saved.status()).isEqualTo(ApplicationStatus.REJECTED);
    }

    @Test
    void saveApplication_whenSameSignatorySubmitsTwice_reusesSignatory() {
        submit(JOINT_ORG_NUMBER, ApplicationStatus.UNDER_REVIEW, SARA);
        submit(JOINT_ORG_NUMBER, ApplicationStatus.UNDER_REVIEW, SARA);

        assertThat(signatoryRepository.count()).isEqualTo(1);
        assertThat(signatureRepository.count()).isEqualTo(2);
    }

    // ============================================================
    // coSign()
    // ============================================================

    @Test
    void coSign_whenPendingCoSignature_approvesApplication() {
        CreditApplicationDataObject saved = submit(JOINT_ORG_NUMBER, ApplicationStatus.APPROVED, SARA);

        CreditApplicationDataObject signed = signingService.coSign(saved.id(), PETER_PERSONAL_NUMBER);

        assertThat(signed.status()).isEqualTo(ApplicationStatus.APPROVED);
        assertThat(applicationRepository.findById(saved.id()).orElseThrow().getStatus())
                .isEqualTo(ApplicationStatus.APPROVED);
        assertThat(signatureRepository.countByApplicationId(saved.id())).isEqualTo(2);
    }

    @Test
    void coSign_whenUnderReview_recordsSignatureWithoutChangingStatus() {
        CreditApplicationDataObject saved = submit(JOINT_ORG_NUMBER, ApplicationStatus.UNDER_REVIEW, SARA);

        CreditApplicationDataObject signed = signingService.coSign(saved.id(), PETER_PERSONAL_NUMBER);

        assertThat(signed.status()).isEqualTo(ApplicationStatus.UNDER_REVIEW);
        assertThat(signatureRepository.countByApplicationId(saved.id())).isEqualTo(2);
    }

    @Test
    void coSign_whenSameSignatorySignsAgain_throws() {
        CreditApplicationDataObject saved = submit(JOINT_ORG_NUMBER, ApplicationStatus.APPROVED, SARA);

        assertThatThrownBy(() -> signingService.coSign(saved.id(), SARA.personalNumber()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(signatureRepository.countByApplicationId(saved.id())).isEqualTo(1);
    }

    @Test
    void coSign_whenPersonIsNotSignatory_throws() {
        CreditApplicationDataObject saved = submit(JOINT_ORG_NUMBER, ApplicationStatus.APPROVED, SARA);

        assertThatThrownBy(() -> signingService.coSign(saved.id(), ANDERS.personalNumber()))
                .isInstanceOf(CompanyValidationFailedException.class);
        assertThat(signatureRepository.countByApplicationId(saved.id())).isEqualTo(1);
    }

    @Test
    void coSign_whenRejected_throws() {
        CreditApplicationDataObject saved = submit(JOINT_ORG_NUMBER, ApplicationStatus.REJECTED, SARA);

        assertThatThrownBy(() -> signingService.coSign(saved.id(), PETER_PERSONAL_NUMBER))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void coSign_whenAlreadyFullySigned_throws() {
        CreditApplicationDataObject saved = submit(JOINT_ORG_NUMBER, ApplicationStatus.APPROVED, SARA);
        signingService.coSign(saved.id(), PETER_PERSONAL_NUMBER);

        assertThatThrownBy(() -> signingService.coSign(saved.id(), PETER_PERSONAL_NUMBER))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void coSign_whenSignsAlone_throws() {
        CreditApplicationDataObject saved = submit(ALONE_ORG_NUMBER, ApplicationStatus.APPROVED, ANDERS);

        assertThatThrownBy(() -> signingService.coSign(saved.id(), ANDERS.personalNumber()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ============================================================
    // application_decision()
    // ============================================================

    @Test
    @WithMockUser(authorities = {"ROLE_CASEWORKER"})
    void applicationDecision_whenApprovedWithoutCoSignature_waitsForCoSignature() {
        CreditApplicationDataObject saved = submit(JOINT_ORG_NUMBER, ApplicationStatus.UNDER_REVIEW, SARA);

        backofficeService.application_decision(saved.id(), ApplicationStatus.APPROVED, WORKER_EMAIL, WORKER_NAME, null);

        assertThat(applicationRepository.findById(saved.id()).orElseThrow().getStatus())
                .isEqualTo(ApplicationStatus.PENDING_CO_SIGNATURE);
    }

    @Test
    @WithMockUser(authorities = {"ROLE_CASEWORKER"})
    void applicationDecision_whenApprovedAfterCoSignature_approvesDirectly() {
        CreditApplicationDataObject saved = submit(JOINT_ORG_NUMBER, ApplicationStatus.UNDER_REVIEW, SARA);
        signingService.coSign(saved.id(), PETER_PERSONAL_NUMBER);

        backofficeService.application_decision(saved.id(), ApplicationStatus.APPROVED, WORKER_EMAIL, WORKER_NAME, null);

        assertThat(applicationRepository.findById(saved.id()).orElseThrow().getStatus())
                .isEqualTo(ApplicationStatus.APPROVED);
    }

    @Test
    @WithMockUser(authorities = {"ROLE_CASEWORKER"})
    void applicationDecision_whenPendingCoSignatureAndApprovedAgain_throws() {
        CreditApplicationDataObject saved = submit(JOINT_ORG_NUMBER, ApplicationStatus.APPROVED, SARA);

        assertThatThrownBy(() -> backofficeService.application_decision(
                saved.id(), ApplicationStatus.APPROVED, WORKER_EMAIL, WORKER_NAME, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @WithMockUser(authorities = {"ROLE_CASEWORKER"})
    void applicationDecision_whenPendingCoSignatureAndRejected_rejects() {
        CreditApplicationDataObject saved = submit(JOINT_ORG_NUMBER, ApplicationStatus.APPROVED, SARA);

        backofficeService.application_decision(saved.id(), ApplicationStatus.REJECTED, WORKER_EMAIL, WORKER_NAME, null);

        assertThat(applicationRepository.findById(saved.id()).orElseThrow().getStatus())
                .isEqualTo(ApplicationStatus.REJECTED);
    }

    private CreditApplicationDataObject submit(String orgNumber, ApplicationStatus status,
                                        CompanyValidationApiDTO.Signatory signatory) {
        NewApplicationDTO application = new NewApplicationDTO(
                new BigDecimal("250000"),
                "Expansion",
                12,
                status,
                status.name(),
                "Test decision reason",
                "Test scoring result",
                "Duo Konsult AB",
                orgNumber,
                signatory.name(),
                0
        );
        return applicationService.saveApplication(application, CONTACTS, false, signatory);
    }
}
