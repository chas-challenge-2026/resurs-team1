package se.comerit.resurs.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import se.comerit.resurs.enums.ApplicationStatus;
import se.comerit.resurs.persistence.CreditApplicationRepository;
import se.comerit.resurs.persistence.DocumentRepository;
import se.comerit.resurs.persistence.model.CreditApplication;
import se.comerit.resurs.persistence.model.Document;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private CreditApplicationRepository creditApplicationRepository;

    private DocumentService documentService;

    @BeforeEach
    void setUp() {documentService = new DocumentService(documentRepository, creditApplicationRepository);}

    private CreditApplication pendingApplication() {
        CreditApplication application = new CreditApplication();
        application.setStatus(ApplicationStatus.PENDING_DOCS);
        application.setAuditLog("[]");
        return application;
    }

    @Test
    void uploadDocument_emptyFile_throwsIllegalArgumentException() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "test.pdf",
                "application/pdf", new byte[0]);

        assertThatThrownBy(() -> documentService.uploadDocument(1L, "balansrakning", emptyFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No chosen file");

        verifyNoInteractions(documentRepository);
        verifyNoInteractions(creditApplicationRepository);
    }

    @Test
    void uploadDocument_invalidFileType_throwsIllegalArgumentException() {
        MockMultipartFile txtFile = new MockMultipartFile("file", "test.txt",
                "text/plain", "innehall".getBytes());

        assertThatThrownBy(() -> documentService.uploadDocument(1L, "balansrakning", txtFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only PDF-files");

        verifyNoInteractions(documentRepository);
        verifyNoInteractions(creditApplicationRepository);
    }

    @Test
    void uploadDocument_validPdf_savesViaRepository() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "balansrakning.pdf",
                "application/pdf", "innehall".getBytes());
        when(creditApplicationRepository.findById(1L)).thenReturn(Optional.of(pendingApplication()));

        documentService.uploadDocument(1L, "balansrakning", file);

        ArgumentCaptor<Document> captor = ArgumentCaptor.forClass(Document.class);
        verify(documentRepository).save(captor.capture());
        assertThat(captor.getValue().getDoc_type()).isEqualTo("balansrakning");
        assertThat(captor.getValue().getFilename()).isEqualTo("1_balansrakning.pdf");
    }

    @Test
    void uploadDocument_validPdf_updatesAuditLog() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "balansrakning.pdf",
                "application/pdf", "innehall".getBytes());
        CreditApplication application = pendingApplication();
        when(creditApplicationRepository.findById(1L)).thenReturn(Optional.of(application));

        documentService.uploadDocument(1L, "balansrakning", file);

        assertThat(application.getAuditLog())
                .contains("DOCUMENT_UPLOADED")
                .contains("balansrakning.pdf");
    }

    @Test
    void uploadDocument_annualReport_updatesStatusToUnderReview() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "arsredovisning.pdf",
                "application/pdf", "innehall".getBytes());
        CreditApplication application = pendingApplication();
        when(creditApplicationRepository.findById(1L)).thenReturn(Optional.of(application));

        documentService.uploadDocument(1L, "arsredovisning", file);

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.UNDER_REVIEW);
    }

    @Test
    void uploadDocument_otherDocType_doesNotAffectStatus() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "balansrakning.pdf",
                "application/pdf", "innehall".getBytes());
        CreditApplication application = pendingApplication();
        when(creditApplicationRepository.findById(1L)).thenReturn(Optional.of(application));

        documentService.uploadDocument(1L, "balansrakning", file);

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.PENDING_DOCS);
    }

    @Test
    void resolveFileForDownload_unknownId_returnsNull() {
        when(documentRepository.findById(999L)).thenReturn(Optional.empty());

        File result = documentService.resolveFileForDownload(999L);
        assertThat(result).isNull();
    }
}