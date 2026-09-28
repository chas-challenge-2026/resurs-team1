package se.comerit.resurs.controller;

import org.apache.coyote.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import se.comerit.resurs.dto.CreditApplicationDTO;
import se.comerit.resurs.dto.DocumentDTO;
import se.comerit.resurs.dto.application.ApplicationShortDTO;
import se.comerit.resurs.dto.application.ApplicationSubmission;
import se.comerit.resurs.dto.application.ApplicationWithDocumentsDTO;
import se.comerit.resurs.dto.application.NewApplicationDTO;
import se.comerit.resurs.dto.companyvalidation.CompanyFinancialApiDTO;
import se.comerit.resurs.dto.companyvalidation.CompanyValidationApiDTO;
import se.comerit.resurs.security.CompanyPrincipal;
import se.comerit.resurs.service.*;

import java.math.BigDecimal;
import java.net.URI;
import java.util.*;

/**
 * ApplicationController – Hanterar kreditansökningar.
 * VARNING: Denna klass innehåller avsiktliga anti-patterns för pedagogiskt syfte.
 * Se docs/known-bugs.md för fullständig lista.
 */
@RestController
@RequestMapping("api/application")
public class ApplicationController {


    private final CompanyValidationService validationService;
    private final CompanyFinancialService financialService;
    private final DocumentService documentService;
    private final ApplicationService appService;
    private final CompanyService companyService; //Swap to CompanyService later
    private final ScoringService creditScoreService;

    public ApplicationController(CompanyValidationService validationService, CompanyFinancialService financialService, DocumentService documentService, ApplicationService appService, CompanyService companyService, ScoringService creditScoreService) {
        this.validationService = validationService;
        this.financialService = financialService;
        this.documentService = documentService;
        this.appService = appService;
        this.companyService = companyService;
        this.creditScoreService = creditScoreService;
    }

    // ============================================================
    // POST /apply — skapa ansökan + kör scoring inline
    // ============================================================
    @PostMapping("/apply")
    public ResponseEntity<CreditApplicationDTO> submitApplication(
            @RequestBody ApplicationSubmission submission,
            @AuthenticationPrincipal CompanyPrincipal principal) {


        // TODO: encrypt PII before go-live
        // PII stored in plaintext: companyName, orgNumber, authorizedSignatory
        // No validation or sanitization of inputs

        //hämtar mockad information som matchar "bolagsApi" som i sin tur hämtar ifrån bolagsverket.
        String personalNumber = principal.personalNumber();
        CompanyValidationApiDTO company = validationService.validateCompanyExists(principal.orgNumber());
        CompanyValidationApiDTO.Signatory signatory = validationService.validateSignatory(company, personalNumber);

        CompanyFinancialApiDTO financials = financialService.fetchLatestAnnualReport(principal.orgNumber())
                .orElseThrow();

        //#TODO CHANGE BRANCH TO BE FETCHED FROM VALIDATION SERVICE
        NewApplicationDTO scoredApplication = creditScoreService.scoreFromFinancialObject(financials, submission.requestedAmount(), "bransch", principal.orgNumber(), company.companyName(), signatory.name(), submission.purpose(), submission.durationMonths());

        CreditApplicationDTO application = appService.saveApplication(scoredApplication, submission.contactDetails());
        //I moved this to Application service, it does not fetch the log and update it as its unneccesary when we create the log either way.
        // TODOs are found in the corresponding lines
        // ===========================================================
        // INSERT 3: Uppdatera audit log med scoring-resultat
        // ===========================================================

        // End of INSERT 3 — still no transaction around all three operations

        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/application/{id}")
                .buildAndExpand(application.id())
                .toUri();

        return ResponseEntity.created(location).body(application);
    }


    // ============================================================
    // GET /application/{id} — visa enskild ansökan
    // ============================================================
    @GetMapping("/{id}")
    public ResponseEntity<ApplicationWithDocumentsDTO> viewApplication(@PathVariable("id") Long id,
                                                                       @AuthenticationPrincipal CompanyPrincipal principal) {
        CreditApplicationDTO app = appService.findApplicationByID(id);

        if (!principal.orgNumber().equals(app.orgNumber())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // Fetch documents for this application
        List<DocumentDTO> docs = documentService.findByApplicationId(id);
        return ResponseEntity.ok(new ApplicationWithDocumentsDTO(app,docs));
    }

    // ============================================================
    // GET /applications — lista alla ansökningar för företaget
    // ============================================================
    @GetMapping()
    public ResponseEntity<List<CreditApplicationDTO>> listApplications(@AuthenticationPrincipal CompanyPrincipal principal) {
            // Get companyId via orgNumber — no caching, hits DB every time
            Long companyID = companyService.getCompanyFromOrgNumber(principal.orgNumber()).id();

        List<CreditApplicationDTO> apps = appService.readApplicationsByCompanyDesc(companyID);

        return ResponseEntity.ok(apps);

    }

    // ============================================================
    // GET /dashboard — startsida för inloggad företagsanvändare
    // ============================================================
    @GetMapping("/dashboard")
    public ResponseEntity<List<ApplicationShortDTO>> dashboard(@AuthenticationPrincipal CompanyPrincipal principal) {

        Long companyID = companyService.getCompanyFromOrgNumber(principal.orgNumber()).id();

        // Count applications by status
        Pageable limit = PageRequest.of(0,5);
        List<CreditApplicationDTO> apps = appService.readApplicationsByCompanyDesc(companyID,limit);

        return ResponseEntity.ok(apps.stream().map(ApplicationShortDTO::new).toList());
    }
}
