package se.comerit.resurs.service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import se.comerit.resurs.enums.ApplicationStatus;
import se.comerit.resurs.persistence.CreditApplicationRepository;
import se.comerit.resurs.persistence.model.CreditApplication;
import se.comerit.resurs.persistence.model.Document;
import se.comerit.resurs.persistence.DocumentRepository;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class DocumentService {
    // Uploads dir — /tmp rensas vid omstart, ingen persistent lagring
    // TODO: använd ett persistent filsystem eller S3 i v2
    private static final String UPLOAD_DIR = "/tmp/uploads/"; //known bug #9, change to persistent storaging

    private final DocumentRepository documentRepository;
    private final CreditApplicationRepository creditApplicationRepository;

    @Autowired
    public DocumentService(DocumentRepository documentRepository, CreditApplicationRepository creditApplicationRepository) {
        this.documentRepository = documentRepository;
        this.creditApplicationRepository = creditApplicationRepository;
    }
    public List<Document> findByApplicationId(Long applicationId) {
        return documentRepository.findByApplicationId(applicationId);
    }

    // Utan transactional så sparas document, audit-log och status var för sig till databasen, helt oberoende
    // av varandra, om det skulle krascha mitt i så lämnas inkonsekvent data, samma bugg som i known-bugs.md #5.
    @Transactional
    public void uploadDocument(Long applicationId, String docType, MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("No chosen file.");
        }
        validateFileType(file);
        CreditApplication application = creditApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found."));

        String originalFilename = file.getOriginalFilename();
        String storedFilename = applicationId + "_" + originalFilename;

        File uploadDir = new File(UPLOAD_DIR);
        if (!uploadDir.exists()) uploadDir.mkdirs();
        file.transferTo(new File(UPLOAD_DIR + storedFilename));

        // Store filename in DB — file path is /tmp which is not persistent
        // TODO: implement PDF parsing in v2 (see native/README.md)
        // The file is saved but its contents are never read or validated
        Document document = new Document();
        document.setApplication(application);
        document.setFilename(storedFilename);
        document.setDoc_type(docType);
        document.setUploadedAt(LocalDateTime.now());
        documentRepository.save(document);

        appendAuditLog(application, originalFilename, docType);
        // Update application status from PENDING_DOCS to UNDER_REVIEW if årsredovisning uploaded
        // No business rules validation — just check docType string
        if("arsredovisning".equals(docType) || "årsredovisning".equals(docType)) {
            markUnderReview(application);
        }
    }

    public File resolveFileForDownload(Long documentId) {
        return documentRepository.findById(documentId)
                .map(document -> new File(UPLOAD_DIR + document.getFilename()))
                .filter(File::exists)
                .orElse(null);
    }

    private void validateFileType(MultipartFile file) {
        String filename = file.getOriginalFilename();
        boolean allowed = filename != null && filename.toLowerCase().endsWith(".pdf");
        if (!allowed) {
            throw new IllegalArgumentException("Only PDF-files are accepted.");
        }
    }

    // Replace with AuditService.append() when issue #102 is done
    // Update audit log JSON blob — same string manipulation pattern as ApplicationController
    // TODO: skapa separat audit_log-tabell med index
    private void appendAuditLog(CreditApplication application, String filename, String docType) {
        String newEntry = "{\"ts\":\"" +
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                + "\",\"action\":\"DOCUMENT_UPLOADED\",\"filename\":\"" + filename
                + "\",\"docType\":\"" + docType + "\"}";
        String currentLog = application.getAuditLog();
        String updatedLog = (currentLog == null || currentLog.equals("[]")) ? "[" + newEntry +
                "]" : currentLog.substring(0, currentLog.lastIndexOf("]")) + "," + newEntry + "]";

        application.setAuditLog(updatedLog);
        application.setUpdatedAt(LocalDateTime.now());
        creditApplicationRepository.save(application);
    }

    private void markUnderReview(CreditApplication application) {
        if (application.getStatus() == ApplicationStatus.PENDING_DOCS) {
            application.setStatus(ApplicationStatus.UNDER_REVIEW);
            application.setUpdatedAt(LocalDateTime.now());
            creditApplicationRepository.save(application);
        }
    }



    // Out commented old code, this is with jdbc
    //Replace with ApplicationRepository when issue #120 is done
//    private void markUnderReview(Long applicationId) {
//        jdbcTemplate.update(
//                "UPDATE applications SET status = 'UNDER_REVIEW', " +
//                        "updated_at = NOW() WHERE id = ? AND status = 'PENDING_DOCS'", applicationId);
//    }

}
