package se.comerit.resurs.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import se.comerit.resurs.dto.CreditApplicationDTO;
import se.comerit.resurs.dto.backoffice.ApplicationWithFinancesDTO;
import se.comerit.resurs.dto.backoffice.BackOfficeListsDTO;
import se.comerit.resurs.dto.backoffice.CreditApplicationDetails;
import se.comerit.resurs.dto.companyvalidation.CompanyFinancialApiDTO;
import se.comerit.resurs.enums.ApplicationStatus;
import se.comerit.resurs.security.CaseWorkerPrincipal;
import se.comerit.resurs.service.BackofficeService;
import se.comerit.resurs.service.CompanyFinancialService;

import java.util.List;
import java.util.Optional;


/**
 * BackofficeController – Handläggargränssnitt för manuell granskning.
 *
 * Ansvarar för: att ta emot förfrågningar om att se ansökningar, fatta beslut och se detaljer,
 * samt att skicka vidare sidnummer/sidstorlek till BackofficeService.
 *
 * Anti-patterns:
 *  - JdbcTemplate direkt i kontrollern
 *  - Audit log uppdateras via JSON string manipulation
 *  - Ingen e-postnotifiering vid beslut
 *  - Session check copy-pasteat
 */
@PreAuthorize("hasRole('CASE_WORKER')")
@RestController
@RequestMapping("/api/backoffice")
public class BackofficeController {

    private final BackofficeService service;
    private final CompanyFinancialService financeService;

    @Autowired
    public BackofficeController(BackofficeService service, CompanyFinancialService financeService) {
        this.service = service;
        this.financeService = financeService;
    }

    @GetMapping
    public ResponseEntity<BackOfficeListsDTO> backofficeOverview(
            @Qualifier("review") Pageable reviewPageable, @Qualifier("decided") Pageable decidedPageable) {


        BackOfficeListsDTO applicationLists = service.applicationsForReview(
                reviewPageable, decidedPageable);

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
    public ResponseEntity<ApplicationWithFinancesDTO> viewApplicationDetail(
            @PathVariable("id") Long id
            ) {
        CreditApplicationDetails details = service.application_details(id);
        Optional<CompanyFinancialApiDTO> finances =  financeService.fetchLatestAnnualReport(details.application().orgNumber());
        /*
        Map<String, Object> app = apps.get(0);
        model.addAttribute("application", app);
        model.addAttribute("auditLogRaw", app.get("auditLog"));
        model.addAttribute("workerName", session.getAttribute("workerName"));
        */

        return ResponseEntity.ok(new ApplicationWithFinancesDTO(details,finances.orElse(null)));
    }
}
