package se.comerit.resurs.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;
import se.comerit.resurs.model.Document;
import se.comerit.resurs.repository.DocumentRepository;
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
    private JdbcTemplate jdbcTemplate;

    private Long pendingDocsApplicationId;

    // Fresh PENDING_DOCS-ansökan per test, oberoende av seed.sql:s färdiga UNDER_REVIEW-ansökan (id=1)
    @BeforeEach
    void createPendingApplication() {
        pendingDocsApplicationId = jdbcTemplate.queryForObject(
                "INSERT INTO applications (company_id, requested_amount, purpose, status, audit_log) " +
                        "VALUES (1, 100000.00, 'Testansökan', 'PENDING_DOCS', '[]') RETURNING id",
                Long.class);
    }

    @Test
    void showDocumentsPage_utanSession_omdirigerarTillLogin() throws Exception {
        mockMvc.perform(get("/documents/{id}", pendingDocsApplicationId))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void showDocumentsPage_giltigAnsokan_visarDocumentsMedModell() throws Exception {
        mockMvc.perform(get("/documents/{id}", pendingDocsApplicationId).sessionAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(view().name("documents"))
                .andExpect(model().attributeExists("application"))
                .andExpect(model().attribute("applicationId", pendingDocsApplicationId))
                .andExpect(model().attribute("documents", List.of()));
    }

    @Test
    void showDocumentsPage_okandAnsokan_omdirigerarTillApplications() throws Exception {
        mockMvc.perform(get("/documents/{id}", 999_999L).sessionAttr("userId", 1L))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/applications"));
    }

    @Test
    void uploadDocument_utanSession_omdirigerarTillLogin() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf",
                "application/pdf", "dummy".getBytes());

        mockMvc.perform(multipart("/document/upload")
                        .file(file)
                        .param("applicationId", String.valueOf(pendingDocsApplicationId))
                        .param("docType", "balansrakning"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        assertThat(documentRepository.findByApplicationIdOrderByUploadedAtDesc(pendingDocsApplicationId)).isEmpty();
    }

    @Test
    void uploadDocument_giltigPdf_sparasOchOmdirigerar() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "balansrakning.pdf",
                "application/pdf", "dummy".getBytes());

        mockMvc.perform(multipart("/document/upload")
                        .file(file)
                        .param("applicationId", String.valueOf(pendingDocsApplicationId))
                        .param("docType", "balansrakning")
                        .sessionAttr("userId", 1L))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/documents/" + pendingDocsApplicationId));

        List<Document> saved = documentRepository.findByApplicationIdOrderByUploadedAtDesc(pendingDocsApplicationId);
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getDocType()).isEqualTo("balansrakning");
        assertThat(saved.get(0).getFilename()).isEqualTo(pendingDocsApplicationId + "_balansrakning.pdf");

        String auditLog = jdbcTemplate.queryForObject(
                "SELECT audit_log FROM applications WHERE id = ?", String.class, pendingDocsApplicationId);
        assertThat(auditLog).contains("DOCUMENT_UPLOADED").contains("balansrakning.pdf");
    }

    @Test
    void uploadDocument_ejPdf_visarFelOchSparasInte() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "balansrakning.txt",
                "text/plain", "dummy".getBytes());

        mockMvc.perform(multipart("/document/upload")
                        .file(file)
                        .param("applicationId", String.valueOf(pendingDocsApplicationId))
                        .param("docType", "balansrakning")
                        .sessionAttr("userId", 1L))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/documents/" + pendingDocsApplicationId));

        assertThat(documentRepository.findByApplicationIdOrderByUploadedAtDesc(pendingDocsApplicationId)).isEmpty();
    }

    @Test
    void uploadDocument_arsredovisning_flyttarStatusTillUnderReview() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "arsredovisning.pdf",
                "application/pdf", "dummy".getBytes());

        mockMvc.perform(multipart("/document/upload")
                        .file(file)
                        .param("applicationId", String.valueOf(pendingDocsApplicationId))
                        .param("docType", "arsredovisning")
                        .sessionAttr("userId", 1L))
                .andExpect(status().is3xxRedirection());

        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM applications WHERE id = ?", String.class, pendingDocsApplicationId);
        assertThat(status).isEqualTo("UNDER_REVIEW");
    }

    @Test
    void uploadDocument_annanDoctype_paverkarInteStatus() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "balansrakning.pdf",
                "application/pdf", "dummy".getBytes());

        mockMvc.perform(multipart("/document/upload")
                        .file(file)
                        .param("applicationId", String.valueOf(pendingDocsApplicationId))
                        .param("docType", "balansrakning")
                        .sessionAttr("userId", 1L))
                .andExpect(status().is3xxRedirection());

        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM applications WHERE id = ?", String.class, pendingDocsApplicationId);
        assertThat(status).isEqualTo("PENDING_DOCS");
    }

    @Test
    void downloadDocument_utanSession_omdirigerarTillLogin() throws Exception {
        mockMvc.perform(get("/document/{id}", 1L))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "/login"));
    }

    @Test
    void downloadDocument_okantId_ger404() throws Exception {
        mockMvc.perform(get("/document/{id}", 999_999L).sessionAttr("userId", 1L))
                .andExpect(status().isNotFound());
    }

    @Test
    void downloadDocument_uppladdadFil_returnerarInnehallOchHeaders() throws Exception {
        MockMultipartFile upload = new MockMultipartFile("file", "balansrakning.pdf",
                "application/pdf", "dummy innehall".getBytes());
        mockMvc.perform(multipart("/document/upload")
                .file(upload)
                .param("applicationId", String.valueOf(pendingDocsApplicationId))
                .param("docType", "balansrakning")
                .sessionAttr("userId", 1L));

        Long documentId = documentRepository.findByApplicationIdOrderByUploadedAtDesc(pendingDocsApplicationId)
                .get(0).getId();

        mockMvc.perform(get("/document/{id}", documentId).sessionAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"" + pendingDocsApplicationId + "_balansrakning.pdf\""))
                .andExpect(content().bytes("dummy innehall".getBytes()));
    }
}