package se.comerit.resurs.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import se.comerit.resurs.model.Document;
import se.comerit.resurs.repository.DocumentRepository;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private JdbcTemplate jdbcTemplate;

    private DocumentService documentService;

    @BeforeEach
    void setUp() {documentService = new DocumentService(documentRepository, jdbcTemplate);}

    @Test
    void uploadDocument_tomFil_kastarIllegalArgumentException() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "test.pdf",
                "application/pdf", new byte[0]);

        assertThatThrownBy(() -> documentService.uploadDocument(1L, "balansrakning", emptyFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No chosen file");

        verifyNoInteractions(documentRepository);
    }

    @Test
    void uploadDocument_felFiltyp_kastarIllegalArgumentException() {
        MockMultipartFile txtFile = new MockMultipartFile("file", "test.txt",
                "text/plain", "innehall".getBytes());

        assertThatThrownBy(() -> documentService.uploadDocument(1L, "balansrakning", txtFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only PDF-files");

        verifyNoInteractions(documentRepository);
    }

    @Test
    void uploadDocument_giltigPdf_sparasViaRepository() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "balansrakning.pdf",
                "application/pdf", "innehall".getBytes());
        when(jdbcTemplate.queryForObject(anyString(), eq(String.class), eq(1L))).thenReturn("[]");

        documentService.uploadDocument(1L, "balansrakning", file);

        ArgumentCaptor<Document> captor = ArgumentCaptor.forClass(Document.class);
        verify(documentRepository).save(captor.capture());
        assertThat(captor.getValue().getApplicationId()).isEqualTo(1L);
        assertThat(captor.getValue().getDocType()).isEqualTo("balansrakning");
        assertThat(captor.getValue().getFilename()).isEqualTo("1_balansrakning.pdf");
    }

    @Test
    void uploadDocument_arsredovisning_uppdaterarStatusTillUnderReview() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "arsredovisning.pdf",
                "application/pdf", "innehall".getBytes());
        when(jdbcTemplate.queryForObject(anyString(), eq(String.class), eq(1L))).thenReturn("[]");

        documentService.uploadDocument(1L, "arsredovisning", file);

        verify(jdbcTemplate).update(contains("status = 'UNDER_REVIEW'"), eq(1L));
    }

    @Test
    void uploadDocument_annanDoctype_paverkarInteStatus() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "balansrakning.pdf",
                "application/pdf", "innehall".getBytes());
        when(jdbcTemplate.queryForObject(anyString(), eq(String.class), eq(1L))).thenReturn("[]");

        documentService.uploadDocument(1L, "balansrakning", file);

        verify(jdbcTemplate, never()).update(contains("status = 'UNDER_REVIEW'"), anyLong());
    }

    @Test
    void resolveFileForDownload_okantId_returnerarNull() {
        when(documentRepository.findById(999L)).thenReturn(Optional.empty());

        File result = documentService.resolveFileForDownload(999L);
        assertThat(result).isNull();
    }
}