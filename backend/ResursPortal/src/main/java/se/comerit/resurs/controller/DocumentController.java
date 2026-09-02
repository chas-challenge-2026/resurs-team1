package se.comerit.resurs.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpSession;
import se.comerit.resurs.service.DocumentService;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * DocumentController – Hanterar dokumentuppladdning.
 *
 * VARNING: PDF sparas men parsas INTE.
 * TODO: implement PDF parsing in v2 (see native/README.md)
 *
 * Anti-patterns:
 *  - JdbcTemplate direkt i kontrollern
 *  - Filer sparas i /tmp/uploads — rensas vid omstart
 *  - Ingen validering av filtyp (accepterar vad som helst) -> validateFileType() finns och fungerar
 *  - Audit log uppdateras via JSON string manipulation -> logiken är flyttad till DocumentService.appendAuditLog()
 *  - Session check copy-pasteat
 */
@Controller
public class DocumentController {

    @Autowired
    private DocumentService documentService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/documents/{applicationId}")
    public String showDocumentsPage(@PathVariable("applicationId") Long applicationId,
                                    HttpSession session,
                                    Model model) {
        // Session check copy-pasted in every method — should be an interceptor
        if (session.getAttribute("userId") == null) return "redirect:/login";

        List<Map<String, Object>> apps = jdbcTemplate.queryForList(
            "SELECT a.*, c.company_name FROM applications a JOIN companies c ON a.company_id = c.id WHERE a.id = ?",
            applicationId
        );
        if (apps.isEmpty()) {
            return "redirect:/applications";
        }
        model.addAttribute("application", apps.get(0));
        model.addAttribute("documents", documentService.findByApplicationId(applicationId));
        model.addAttribute("applicationId", applicationId);
        return "documents";
    }

    @PostMapping("/document/upload")
    public String uploadDocument(@RequestParam("applicationId") Long applicationId,
                                 @RequestParam("docType") String docType,
                                 @RequestParam("file") MultipartFile file,
                                 HttpSession session,
                                 Model model) {
        // Session check copy-pasted in every method — should be an interceptor
        if (session.getAttribute("userId") == null) return "redirect:/login";

        try {
            documentService.uploadDocument(applicationId, docType, file);
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
        } catch (IOException e) {
            model.addAttribute("error", "Uppladdning misslyckades: " + e.getMessage());
        }
        return "redirect:/documents/" + applicationId;
    }

    @GetMapping("/document/{id}")
    public ResponseEntity<Resource> downloadDocument(@PathVariable("id") Long documentId,
                                                     HttpSession session) {
        // Session check copy-pasted in every method — should be an interceptor
        if (session.getAttribute("userId") == null) {
            return ResponseEntity.status(302).header("Location", "/login").build();
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
