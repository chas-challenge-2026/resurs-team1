package se.comerit.resurs.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import se.comerit.resurs.dto.backoffice.BackOfficeListsDTO;
import se.comerit.resurs.dto.backoffice.CreditApplicationDetails;
import se.comerit.resurs.enums.ApplicationStatus;
import se.comerit.resurs.security.CaseWorkerPrincipal;
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
@PreAuthorize("hasRole('CASE_WORKER')")
@RestController
@RequestMapping("/api/backoffice")
public class BackofficeController {

    private final BackofficeService service;

    @Autowired
    public BackofficeController(BackofficeService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<BackOfficeListsDTO> backofficeOverview() {
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
                                       @AuthenticationPrincipal CaseWorkerPrincipal principal
                                       ) {


        //Any status other than Approved or Rejected results in a redirection.
        //Update: REST-APIs should respond with bad request. /Jonathan
        if (!ApplicationStatus.APPROVED.toString().equals(decision)
                && !ApplicationStatus.REJECTED.toString().equals(decision)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        String workerName = principal.name();
        String workerEmail = principal.email();
        ApplicationStatus newStatus = ApplicationStatus.valueOf(decision);

        service.application_decision(applicationId,newStatus,workerEmail, workerName,comment);

        // No email notification — TODO: implement email via Spring Mail in v2
        // TODO: notify company via email when decision is made

        return ResponseEntity.ok().build();
    }

    @GetMapping("/application/{id}")
    public ResponseEntity<CreditApplicationDetails> viewApplicationDetail(
            @PathVariable("id") Long id
            ) {
        CreditApplicationDetails details = service.application_details(id);
        /*
        Map<String, Object> app = apps.get(0);
        model.addAttribute("application", app);
        model.addAttribute("auditLogRaw", app.get("auditLog"));
        model.addAttribute("workerName", session.getAttribute("workerName"));
        */
        return ResponseEntity.ok(details);
    }
}
