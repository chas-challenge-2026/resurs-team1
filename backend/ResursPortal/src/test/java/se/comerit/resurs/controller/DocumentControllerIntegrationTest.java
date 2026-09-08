package se.comerit.resurs.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;
import se.comerit.resurs.enums.ApplicationStatus;
import se.comerit.resurs.persistence.CreditApplicationRepository;
import se.comerit.resurs.persistence.model.Company;
import se.comerit.resurs.persistence.model.CreditApplication;
import se.comerit.resurs.persistence.model.Document;
import se.comerit.resurs.persistence.CompanyRepository;
import se.comerit.resurs.persistence.DocumentRepository;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integrationstest för dokumentuppladdning: Controller -> Service -> Repository -> riktig Postgres.
 * Postgres-containern initieras med samma infra/seed.sql som docker-compose använder,
 * så schema/fixtures inte kan drifta isär från riktig miljö.
 */
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DocumentControllerIntegrationTest {

    private static final Path SEED_SQL = Paths.get("").toAbsolutePath()
            .resolve("../../infra/seed.sql").normalize();

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:12")
            .withCopyFileToContainer(MountableFile.forHostPath(SEED_SQL), "/docker-entrypoint-initdb.d/seed.sql");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private CreditApplicationRepository creditApplicationRepository;

    @Autowired
    private CompanyRepository companyRepository;

    private Long pendingDocsApplicationId;

    // Fresh PENDING_DOCS-ansökan per test, oberoende av seed.sql:s färdiga UNDER_REVIEW-ansökan (id=1)
    @BeforeEach
    void createPendingApplication() {
        Company company = companyRepository.findById(1L).orElseThrow();
        CreditApplication application = new CreditApplication();
        application.setCompany(company);
        application.setRequestedAmount(new BigDecimal("100000.00"));
        application.setPurpose("Testansökan");
        application.setStatus(ApplicationStatus.PENDING_DOCS);
        application.setAuditLog("[]");

        pendingDocsApplicationId = creditApplicationRepository.save(application).getId();
    }

    @Test
    void listDocuments_withoutSession_returns401() throws Exception {
        mockMvc.perform(get("/documents/{id}", pendingDocsApplicationId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listDocuments_validApplicationWithoutDocument_returnsEmptyList() throws Exception {
        mockMvc.perform(get("/documents/{id}", pendingDocsApplicationId).sessionAttr("userId",
                        1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void showDocumentsPage_unknownApplication_returns404() throws Exception {
        mockMvc.perform(get("/documents/{id}", 999_999L).sessionAttr("userId", 1L))
                .andExpect(status().isNotFound());
    }

    @Test
    void uploadDocument_withoutSession_returns401() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf",
                "application/pdf", "dummy".getBytes());

        mockMvc.perform(multipart("/document/upload")
                        .file(file)
                        .param("applicationId", String.valueOf(pendingDocsApplicationId))
                        .param("docType", "balansrakning"))
                .andExpect(status().isUnauthorized());

        assertThat(documentRepository.findByApplicationId(pendingDocsApplicationId)).isEmpty();
    }

    @Test
    void uploadDocument_validPdf_isSavedAndReturns201() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "balansrakning.pdf",
                "application/pdf", "dummy".getBytes());

        mockMvc.perform(multipart("/document/upload")
                        .file(file)
                        .param("applicationId", String.valueOf(pendingDocsApplicationId))
                        .param("docType", "balansrakning")
                        .sessionAttr("userId", 1L))
                .andExpect(status().isCreated());

        List<Document> saved = documentRepository.findByApplicationId(pendingDocsApplicationId);
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getDoc_type()).isEqualTo("balansrakning");
        assertThat(saved.get(0).getFilename()).isEqualTo(pendingDocsApplicationId + "_balansrakning.pdf");

        CreditApplication application = creditApplicationRepository.findById(pendingDocsApplicationId).orElseThrow();
        assertThat(application.getAuditLog()).contains("DOCUMENT_UPLOADED").contains("balansrakning.pdf");
    }

    @Test
    void uploadDocument_invalidFiletype_returns400AndDoesNotSave() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "balansrakning.txt",
                "text/plain", "dummy".getBytes());

        mockMvc.perform(multipart("/document/upload")
                        .file(file)
                        .param("applicationId", String.valueOf(pendingDocsApplicationId))
                        .param("docType", "balansrakning")
                        .sessionAttr("userId", 1L))
                .andExpect(status().isBadRequest());

        assertThat(documentRepository.findByApplicationId(pendingDocsApplicationId)).isEmpty();
    }

    @Test
    void uploadDocument_annualReport_movesStatusToUnderReview() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "arsredovisning.pdf",
                "application/pdf", "dummy".getBytes());

        mockMvc.perform(multipart("/document/upload")
                        .file(file)
                        .param("applicationId", String.valueOf(pendingDocsApplicationId))
                        .param("docType", "arsredovisning")
                        .sessionAttr("userId", 1L))
                .andExpect(status().isCreated());

        CreditApplication application = creditApplicationRepository.findById(pendingDocsApplicationId).orElseThrow();
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.UNDER_REVIEW);
    }

    @Test
    void uploadDocument_otherDoctype_doesNotAffectStatus() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "balansrakning.pdf",
                "application/pdf", "dummy".getBytes());

        mockMvc.perform(multipart("/document/upload")
                        .file(file)
                        .param("applicationId", String.valueOf(pendingDocsApplicationId))
                        .param("docType", "balansrakning")
                        .sessionAttr("userId", 1L))
                .andExpect(status().isCreated());

        CreditApplication application = creditApplicationRepository.findById(pendingDocsApplicationId).orElseThrow();
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.PENDING_DOCS);
    }

    @Test
    void downloadDocument_withoutSession_returns401() throws Exception {
        mockMvc.perform(get("/document/{id}", 1L))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void downloadDocument_unknownId_returns404() throws Exception {
        mockMvc.perform(get("/document/{id}", 999_999L).sessionAttr("userId", 1L))
                .andExpect(status().isNotFound());
    }

    @Test
    void downloadDocument_uploadedFile_returnsContentAndHeaders() throws Exception {
        MockMultipartFile upload = new MockMultipartFile("file", "balansrakning.pdf",
                "application/pdf", "dummy info".getBytes());
        mockMvc.perform(multipart("/document/upload")
                .file(upload)
                .param("applicationId", String.valueOf(pendingDocsApplicationId))
                .param("docType", "balansrakning")
                .sessionAttr("userId", 1L));

        Long documentId = documentRepository.findByApplicationId(pendingDocsApplicationId)
                .get(0).getId();

        mockMvc.perform(get("/document/{id}", documentId).sessionAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"" + pendingDocsApplicationId + "_balansrakning.pdf\""))
                .andExpect(content().bytes("dummy info".getBytes()));
    }
}