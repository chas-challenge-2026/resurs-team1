package se.comerit.resurs.controller;


import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import se.comerit.resurs.dto.CreditApplicationDetails;
import se.comerit.resurs.dto.backoffice.BackOfficeListsDTO;
import se.comerit.resurs.enums.ApplicationStatus;
import se.comerit.resurs.service.BackofficeService;


/**
 * BackofficeController – Handläggargränssnitt för manuell granskning.
 *
 * Anti-patterns:
 *  - JdbcTemplate direkt i kontrollern
 *  - Audit log uppdateras via JSON string manipulation
 *  - Ingen e-postnotifiering vid beslut
 *  - Session check copy-pasteat
 *  - Ingen pagination — hämtar ALLA ansökningar i REVIEW
 */
@RestController
@RequestMapping("/backoffice")
public class BackofficeController {

    private final BackofficeService service;

    @Autowired
    public BackofficeController(BackofficeService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<BackOfficeListsDTO> backofficeOverview(HttpSession session) {

        // Session check copy-pasted in every method — should be an interceptor
        // Im changing this temporarily to make it REST, Frontend should do the redirection /Jonathan
        if (session.getAttribute("userId") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (!"caseWorker".equals(session.getAttribute("role"))){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        BackOfficeListsDTO applicationLists = service.applicationsForReview();

        /* old thymeleaf model implementation,  kept temporarily as documentation for whats delivered to frontend
        model.addAttribute("reviewApplications", applicationLists.reviewApplications());
        model.addAttribute("decidedApplications", applicationLists.decidedApplications());
        model.addAttribute("workerName", session.getAttribute("workerName"));
        model.addAttribute("reviewCount", applicationLists.reviewApplications().size());
        return "backoffice";
        */

        return ResponseEntity.ok(applicationLists);

    }

    @PostMapping("/decide")
    public ResponseEntity<Void> decide(@RequestParam("applicationId") Long applicationId,
                         @RequestParam("decision") String decision,
                         @RequestParam(value = "comment", defaultValue = "") String comment,
                         HttpSession session) {

        // Session check copy-pasted in every method — should be an interceptor
        // Im changing this temporarily to make it REST, Frontend should do the redirection /Jonathan
        if (session.getAttribute("userId") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (!"caseWorker".equals(session.getAttribute("role"))){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        //Any status other than Approved or Rejected results in a redirection.
        //Update: REST-APIs should respond with bad request. /Jonathan
        if (!ApplicationStatus.APPROVED.toString().equals(decision)
                && !ApplicationStatus.REJECTED.toString().equals(decision)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        String workerName = (String) session.getAttribute("workerName");
        ApplicationStatus newStatus = ApplicationStatus.valueOf(decision);

        service.application_decision(applicationId,newStatus,workerName,comment);

        // No email notification — TODO: implement email via Spring Mail in v2
        // TODO: notify company via email when decision is made

        return ResponseEntity.ok().build();
    }

    @GetMapping("/application/{id}")
    public ResponseEntity<CreditApplicationDetails> viewApplicationDetail(
            @PathVariable("id") Long id,
            HttpSession session) {
        // Session check copy-pasted in every method — should be an interceptor
        // Im changing this temporarily to make it REST, Frontend should do the redirection /Jonathan
        if (session.getAttribute("userId") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (!"caseWorker".equals(session.getAttribute("role"))){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        CreditApplicationDetails details = service.application_details(id);
        /*
        Map<String, Object> app = apps.get(0);
        model.addAttribute("application", app);
        model.addAttribute("auditLogRaw", app.get("audit_log"));
        model.addAttribute("workerName", session.getAttribute("workerName"));
        */

        /* Update this once we have the document parts in place. /Jonathan
        List<Map<String, Object>> docs = jdbcTemplate.queryForList(
            "SELECT * FROM documents WHERE application_id = ? ORDER BY uploaded_at DESC",
            id
        );
        model.addAttribute("documents", docs);
        */
        return ResponseEntity.ok(details);
    }
}
