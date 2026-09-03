
package se.comerit.resurs;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;
import se.comerit.resurs.dto.status.StatusDetails;
import se.comerit.resurs.enums.ApplicationStatus;
import se.comerit.resurs.persistence.CompanyRepository;
import se.comerit.resurs.persistence.CreditApplicationRepository;
import se.comerit.resurs.persistence.DocumentRepository;
import se.comerit.resurs.persistence.model.Company;
import se.comerit.resurs.persistence.model.CreditApplication;
import se.comerit.resurs.persistence.model.Document;
import se.comerit.resurs.service.StatusService;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
class StatusServiceTests {


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
    private StatusService statusService;

    @Autowired
    private CreditApplicationRepository applicationRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private DocumentRepository documentRepository;


    @Test
    void showStatus_pendingDocs_returnsCorrectStatusAndSteps() {
        CreditApplication application =
                createApplication(ApplicationStatus.PENDING_DOCS);

        StatusDetails result =
                statusService.showStatus(application.getId());

        assertNotNull(result);

        assertEquals(
                ApplicationStatus.PENDING_DOCS,
                result.currentStatus()
        );

        assertEquals(
                application.getId(),
                result.app().id()
        );

        assertEquals(
                new BigDecimal("100000.00"),
                result.app().requested_amount()
        );

        assertEquals(
                "Working capital",
                result.app().purpose()
        );

        assertEquals(
                "Test AB",
                result.app().company_name()
        );


        assertEquals(4, result.steps().size());

        assertEquals("DONE", result.steps().get(0).status());
        assertEquals("PENDING", result.steps().get(1).status());
        assertEquals("PENDING", result.steps().get(2).status());
        assertEquals("PENDING", result.steps().get(3).status());
    }


    @Test
    void showStatus_underReview_marksCreditAssessmentAsCurrent() {
        CreditApplication application =
                createApplication(ApplicationStatus.UNDER_REVIEW);

        StatusDetails result =
                statusService.showStatus(application.getId());

        assertEquals(
                ApplicationStatus.UNDER_REVIEW,
                result.currentStatus()
        );

        assertStepStatuses(
                result,
                "DONE",
                "CURRENT",
                "PENDING"
        );
    }


    @Test
    void showStatus_approved_marksAllStepsAsDone() {
        CreditApplication application =
                createApplication(ApplicationStatus.APPROVED);

        StatusDetails result =
                statusService.showStatus(application.getId());

        assertEquals(
                ApplicationStatus.APPROVED,
                result.currentStatus()
        );

        assertStepStatuses(
                result,
                "DONE",
                "DONE",
                "DONE"
        );
    }


    @Test
    void showStatus_rejected_marksAllStepsAsDone() {
        CreditApplication application =
                createApplication(ApplicationStatus.REJECTED);

        StatusDetails result =
                statusService.showStatus(application.getId());

        assertEquals(
                ApplicationStatus.REJECTED,
                result.currentStatus()
        );

        assertStepStatuses(
                result,
                "DONE",
                "DONE",
                "DONE"
        );
    }


    @Test
    void showStatus_returnsDocumentsAsDocumentDTOs() {
        CreditApplication application =
                createApplication(ApplicationStatus.UNDER_REVIEW);

        LocalDateTime uploadedAt =
                LocalDateTime.of(2026, 9, 3, 10, 30);

        Document annualReport = new Document();
        annualReport.setApplication(application);
        annualReport.setFilename("annual-report.pdf");
        annualReport.setDoc_type("ANNUAL_REPORT");
        annualReport.setUploadedAt(uploadedAt);

        Document taxCertificate = new Document();
        taxCertificate.setApplication(application);
        taxCertificate.setFilename("f-tax-certificate.pdf");
        taxCertificate.setDoc_type("F_TAX_CERTIFICATE");
        taxCertificate.setUploadedAt(uploadedAt);

        documentRepository.saveAllAndFlush(
                List.of(annualReport, taxCertificate)
        );

        StatusDetails result =
                statusService.showStatus(application.getId());

        assertEquals(2, result.documents().size());

        assertTrue(
                result.documents().stream().anyMatch(document ->
                        document.applicationId().equals(application.getId())
                                && document.filename().equals("annual-report.pdf")
                                && document.docType().equals("ANNUAL_REPORT")
                                && document.uploadedAt().equals(uploadedAt)
                )
        );

        assertTrue(
                result.documents().stream().anyMatch(document ->
                        document.applicationId().equals(application.getId())
                                && document.filename().equals("f-tax-certificate.pdf")
                                && document.docType().equals("F_TAX_CERTIFICATE")
                                && document.uploadedAt().equals(uploadedAt)
                )
        );
    }


    @Test
    void showStatus_doesNotReturnDocumentsFromOtherApplications() {
        CreditApplication application =
                createApplication(ApplicationStatus.UNDER_REVIEW);

        CreditApplication otherApplication =
                createApplication(ApplicationStatus.UNDER_REVIEW);

        Document ownDocument = createDocument(
                application,
                "own-document.pdf"
        );

        Document otherDocument = createDocument(
                otherApplication,
                "other-document.pdf"
        );

        documentRepository.saveAllAndFlush(
                List.of(ownDocument, otherDocument)
        );

        StatusDetails result =
                statusService.showStatus(application.getId());

        assertEquals(1, result.documents().size());

        assertEquals(
                "own-document.pdf",
                result.documents().getFirst().filename()
        );

        assertEquals(
                application.getId(),
                result.documents().getFirst().applicationId()
        );
    }


    @Test
    void showStatus_returnsAuditLog() {
        CreditApplication application =
                createApplication(ApplicationStatus.APPROVED);

        String auditLog =
                "[{\"event\":\"APPLICATION_APPROVED\"}]";

        application.setAuditLog(auditLog);

        applicationRepository.saveAndFlush(application);

        StatusDetails result =
                statusService.showStatus(application.getId());

        assertEquals(
                auditLog,
                result.app().audit_log()
        );

        assertEquals(
                auditLog,
                result.auditLogRaw()
        );
    }


    @Test
    void showStatus_throwsWhenApplicationDoesNotExist() {
        assertThrows(
                NoSuchElementException.class,
                () -> statusService.showStatus(999999L)
        );
    }


    //Saving an application for tests
    private CreditApplication createApplication(
            ApplicationStatus status
    ) {
        Company company = new Company();

        company.setOrg_number(
                UUID.randomUUID()
                        .toString()
                        .replace("-", "")
                        .substring(0, 20)
        );
        company.setCompany_name("Test AB");
        company.setAuthorized_signatory("Test Person");

        company = companyRepository.saveAndFlush(company);

        CreditApplication application =
                new CreditApplication();

        application.setCompany(company);
        application.setRequestedAmount(
                new BigDecimal("100000.00")
        );
        application.setPurpose("Working capital");
        application.setStatus(status);
        application.setAuditLog("[]");
        application.setCreatedAt(LocalDateTime.now());
        application.setUpdatedAt(LocalDateTime.now());

        return applicationRepository.saveAndFlush(application);
    }


    private Document createDocument(
            CreditApplication application,
            String filename
    ) {
        Document document = new Document();

        document.setApplication(application);
        document.setFilename(filename);
        document.setDoc_type("TEST_DOCUMENT");
        document.setUploadedAt(LocalDateTime.now());

        return document;
    }


    private void assertStepStatuses(
            StatusDetails result,
            String step2,
            String step3,
            String step4
    ) {
        assertEquals(4, result.steps().size());

        assertEquals("DONE", result.steps().get(0).status());
        assertEquals(step2, result.steps().get(1).status());
        assertEquals(step3, result.steps().get(2).status());
        assertEquals(step4, result.steps().get(3).status());
    }
}

