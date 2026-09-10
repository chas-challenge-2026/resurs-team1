package se.comerit.resurs.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;
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

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest
@Transactional
class DocumentServiceTest {

    private static final Path SEED_SQL = Paths.get("").toAbsolutePath()
            .resolve("../../infra/seed.sql").normalize();

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:12")
            .withCopyFileToContainer(MountableFile.forHostPath(SEED_SQL), "/docker-entrypoint-initdb.d/seed.sql");

    @Autowired
    AuditEventRepository auditEventRepository;
    @Autowired
    private DocumentService documentService;
    @Autowired
    private DocumentRepository documentRepository;
    @Autowired
    private CreditApplicationRepository creditApplicationRepository;
    @Autowired
    private CompanyRepository companyRepository;

    private CreditApplication createApplication(ApplicationStatus status) {
        Company company = companyRepository.findById(1L).orElseThrow();
        CreditApplication application = new CreditApplication();
        application.setCompany(company);
        application.setRequestedAmount(new BigDecimal("100000.00"));
        application.setPurpose("Testansökan");
        application.setStatus(status);
        return creditApplicationRepository.save(application);
    }

    @Test
    void uploadDocument_emptyFile_throwsIllegalArgumentException() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "test.pdf",
                "application/pdf", new byte[0]);

        assertThatThrownBy(() -> documentService.uploadDocument(999L, "balansrakning", emptyFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No chosen file");

        assertThat(documentRepository.count()).isZero();
    }

    @Test
    void uploadDocument_invalidFileType_throwsIllegalArgumentException() {
        MockMultipartFile txtFile = new MockMultipartFile("file", "test.txt",
                "text/plain", "innehall".getBytes());

        assertThatThrownBy(() -> documentService.uploadDocument(999L, "balansrakning", txtFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only PDF-files");

        assertThat(documentRepository.count()).isZero();
    }

    @Test
    void uploadDocument_unknownApplicationId_throwsIllegalArgumentException() {
        MockMultipartFile file = new MockMultipartFile("file", "balansrakning.pdf",
                "application/pdf", "innehall".getBytes());

        assertThatThrownBy(() -> documentService.uploadDocument(999_999L, "balansrakning", file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Application not found");
    }

    @Test
    void uploadDocument_validPdf_savesViaRepository() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "balansrakning.pdf",
                "application/pdf", "innehall".getBytes());
        CreditApplication application = createApplication(ApplicationStatus.PENDING_DOCS);

        documentService.uploadDocument(application.getId(), "balansrakning", file);

        List<Document> saved = documentRepository.findByApplicationId(application.getId());
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getDoc_type()).isEqualTo("balansrakning");
        assertThat(saved.get(0).getFilename()).isEqualTo(application.getId() + "_balansrakning.pdf");
    }

    @Test
    void uploadDocument_validPdf_writesAuditEvent() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "balansrakning.pdf",
                "application/pdf", "innehall".getBytes());
        CreditApplication application = createApplication(ApplicationStatus.PENDING_DOCS);

        documentService.uploadDocument(application.getId(), "balansrakning", file);

        List<AuditEvent> events =
                auditEventRepository.findByApplicationIdOrderBySequenceNumberAsc(application.getId());

        assertThat(events).hasSize(1);

        AuditEvent event = events.get(0);
        assertThat(event.getAction()).isEqualTo(AuditAction.DOCUMENT_UPLOADED);
        assertThat(event.getSequenceNumber()).isEqualTo(1L);
        assertThat(event.getActor()).isEqualTo(application.getCompany().getOrg_number());
        assertThat(event.getOccurredAt()).isNotNull();
        // Innehållet i data-fältet testas i AuditEventServiceTest
    }

    @Test
    void uploadDocument_annualReport_updatesStatusToUnderReview() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "arsredovisning.pdf",
                "application/pdf", "innehall".getBytes());
        CreditApplication application = createApplication(ApplicationStatus.PENDING_DOCS);

        documentService.uploadDocument(application.getId(), "arsredovisning", file);
        CreditApplication updated = creditApplicationRepository.findById(application.getId()).orElseThrow();

        assertThat(updated.getStatus()).isEqualTo(ApplicationStatus.UNDER_REVIEW);
    }

    @Test
    void uploadDocument_otherDocType_doesNotAffectStatus() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "balansrakning.pdf",
                "application/pdf", "innehall".getBytes());
        CreditApplication application = createApplication(ApplicationStatus.PENDING_DOCS);

        documentService.uploadDocument(application.getId(), "balansrakning", file);
        CreditApplication updated = creditApplicationRepository.findById(application.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(ApplicationStatus.PENDING_DOCS);
    }

    @Test
    void resolveFileForDownload_unknownId_returnsNull() {
        File result = documentService.resolveFileForDownload(999_999L);
        assertThat(result).isNull();
    }

    @Test
    void findByApplicationId_unknownApplication_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> documentService.findByApplicationId(999_999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Application not found");
    }
}