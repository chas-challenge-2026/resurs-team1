package se.comerit.resurs.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
 *  - Ingen validering av filtyp (accepterar vad som helst)
 *  - Audit log uppdateras via JSON string manipulation
 *  - Session check copy-pasteat
 */
@Controller
public class DocumentController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // Uploads dir — /tmp rensas vid omstart, ingen persistent lagring
    // TODO: använd ett persistent filsystem eller S3 i v2
    private static final String UPLOAD_DIR = "/tmp/uploads/";

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

        List<Map<String, Object>> docs = jdbcTemplate.queryForList(
            "SELECT * FROM documents WHERE application_id = ? ORDER BY uploaded_at DESC",
            applicationId
        );

        model.addAttribute("application", apps.get(0));
        model.addAttribute("documents", docs);
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

        if (file.isEmpty()) {
            model.addAttribute("error", "Ingen fil vald.");
            return "redirect:/documents/" + applicationId;
        }

        // No file type validation — accepts anything, not just PDF
        // TODO: validate that uploaded file is actually a PDF
        String originalFilename = file.getOriginalFilename();
        String storedFilename = applicationId + "_" + originalFilename;

        File uploadDir = new File(UPLOAD_DIR);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        File destination = new File(UPLOAD_DIR + storedFilename);

        try {
            file.transferTo(destination);
        } catch (IOException e) {
            model.addAttribute("error", "Uppladdning misslyckades: " + e.getMessage());
            return "redirect:/documents/" + applicationId;
        }

        // Store filename in DB — file path is /tmp which is not persistent
        // TODO: implement PDF parsing in v2 (see native/README.md)
        // The file is saved but its contents are never read or validated
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                "INSERT INTO documents (application_id, filename, doc_type) VALUES (?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS
            );
            ps.setLong(1, applicationId);
            ps.setString(2, storedFilename);
            ps.setString(3, docType);
            return ps;
        }, keyHolder);

        // Update audit log JSON blob — same string manipulation pattern as ApplicationController
        // TODO: skapa separat audit_log-tabell med index
        String newEntry = "{\"ts\":\"" + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            + "\",\"action\":\"DOCUMENT_UPLOADED\",\"filename\":\"" + originalFilename
            + "\",\"docType\":\"" + docType + "\"}";

        String currentLog = jdbcTemplate.queryForObject(
            "SELECT audit_log FROM applications WHERE id = ?",
            String.class,
            applicationId
        );

        String updatedLog;
        if (currentLog == null || currentLog.equals("[]")) {
            updatedLog = "[" + newEntry + "]";
        } else {
            updatedLog = currentLog.substring(0, currentLog.lastIndexOf("]")) + "," + newEntry + "]";
        }

        jdbcTemplate.update(
            "UPDATE applications SET audit_log = ?, updated_at = NOW() WHERE id = ?",
            updatedLog,
            applicationId
        );

        // Update application status from PENDING_DOCS to UNDER_REVIEW if årsredovisning uploaded
        // No business rules validation — just check docType string
        if ("arsredovisning".equals(docType) || "årsredovisning".equals(docType)) {
            jdbcTemplate.update(
                "UPDATE applications SET status = 'UNDER_REVIEW', updated_at = NOW() " +
                "WHERE id = ? AND status = 'PENDING_DOCS'",
                applicationId
            );
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

        List<Map<String, Object>> docs = jdbcTemplate.queryForList(
            "SELECT * FROM documents WHERE id = ?", documentId
        );

        if (docs.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> doc = docs.get(0);
        String filename = (String) doc.get("filename");
        File file = new File(UPLOAD_DIR + filename);

        if (!file.exists()) {
            // File was in /tmp and got cleared on server restart
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(resource);
    }
}
