package se.comerit.resurs.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpSession;
import se.comerit.resurs.dto.DocumentDTO;
import se.comerit.resurs.persistence.CreditApplicationRepository;
import se.comerit.resurs.service.DocumentService;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * DocumentController – Hanterar dokumentuppladdning.
 *
 * VARNING: PDF sparas men parsas INTE.
 * TODO: implement PDF parsing in v2 (see native/README.md)
 *
 * Anti-patterns:
 *  - JdbcTemplate direkt i kontrollern -> bytt till JPA
 *  - Filer sparas i /tmp/uploads — rensas vid omstart -> kvartstår, inte en del av v2 scope, se known-bugs.md #9
 *  - Ingen validering av filtyp (accepterar vad som helst) -> validateFileType() finns och fungerar
 *  - Audit log uppdateras via JSON string manipulation -> logiken är flyttad till DocumentService.appendAuditLog()
 *  - Session check copy-pasteat -> kvarstår, planerat att ersättas utav Spring Security, SecurityFilterChain.
 */
@RestController
public class DocumentController {

    private DocumentService documentService;
    private CreditApplicationRepository creditApplicationRepository;

    @Autowired
    public DocumentController(DocumentService documentService, CreditApplicationRepository creditApplicationRepository) {
        this.documentService = documentService;
        this.creditApplicationRepository = creditApplicationRepository;
    }

    @GetMapping("/documents/{applicationId}")
    public ResponseEntity<List<DocumentDTO>> listDocuments(@PathVariable("applicationId") Long applicationId,
                                                           HttpSession session) {
        if (session.getAttribute("userId") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (creditApplicationRepository.findById(applicationId).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        List<DocumentDTO> documents = documentService.findByApplicationId(applicationId).stream()
                .map(DocumentDTO::new)
                .toList();
        return ResponseEntity.ok(documents);
    }
// Old thymeleaf compatible method
// Keeping the method as documentations for what was delivered to frontend
//    @GetMapping("/documents/{applicationId}")
//    public String showDocumentsPage(@PathVariable("applicationId") Long applicationId,
//                                    HttpSession session,
//                                    Model model) {
//        // Session check copy-pasted in every method — should be an interceptor
//        if (session.getAttribute("userId") == null) return "redirect:/login";
//
//        Optional<CreditApplication> application = creditApplicationRepository.findById(applicationId);
//        if (application.isEmpty()) {
//            return "redirect:/applications";
//        }
//        model.addAttribute("application", application.get());
//        model.addAttribute("documents", documentService.findByApplicationId(applicationId));
//        model.addAttribute("applicationId", applicationId);
//        return "documents";
//    }

    @PostMapping("/document/upload")
    public ResponseEntity<Void> uploadDocument(@RequestParam("applicationId") Long applicationId,
                                               @RequestParam("docType") String docType,
                                               @RequestParam("file") MultipartFile file,
                                               HttpSession session) {
        if (session.getAttribute("userId") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            documentService.uploadDocument(applicationId, docType, file);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

        // Session check copy-pasted in every method — should be an interceptor
//        if (session.getAttribute("userId") == null) return "redirect:/login";
//        try {
//            documentService.uploadDocument(applicationId, docType, file);
//        } catch (IllegalArgumentException e) {
//            model.addAttribute("error", e.getMessage());
//        } catch (IOException e) {
//            model.addAttribute("error", "Uppladdning misslyckades: " + e.getMessage());
//        }
//        return "redirect:/documents/" + applicationId;
//    }

    @GetMapping("/document/{id}")
    public ResponseEntity<Resource> downloadDocument(@PathVariable("id") Long documentId,
                                                     HttpSession session) {
        // Session check copy-pasted in every method — should be an interceptor
//        if (session.getAttribute("userId") == null) {
//            return ResponseEntity.status(302).header("Location", "/login").build();
//        }
        if (session.getAttribute("userId") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        File file = documentService.resolveFileForDownload(documentId);
        if (file == null) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(resource);
    }
}
