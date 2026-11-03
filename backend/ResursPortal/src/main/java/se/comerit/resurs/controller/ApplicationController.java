package se.comerit.resurs.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import se.comerit.resurs.dto.CreditApplicationDTO;
import se.comerit.resurs.dto.CreditApplicationDataObject;
import se.comerit.resurs.dto.DocumentDTO;
import se.comerit.resurs.dto.application.*;
import se.comerit.resurs.dto.companyvalidation.CompanyFinancialApiDTO;
import se.comerit.resurs.dto.companyvalidation.CompanyValidationApiDTO;
import se.comerit.resurs.enums.ApplicationStatus;
import se.comerit.resurs.exception.companyvalidation.CompanyRegistryUnavailableException;
import se.comerit.resurs.security.ApplicationAccessPolicy;
import se.comerit.resurs.security.CompanyPrincipal;
import se.comerit.resurs.service.*;

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
    private final ApplicationAccessPolicy accessPolicy;
    private final SparService sparService;
    private final ApplicationSigningService signingService;

    public ApplicationController(CompanyValidationService validationService,
                                 CompanyFinancialService financialService,
                                 DocumentService documentService,
                                 ApplicationService appService,
                                 CompanyService companyService,
                                 ScoringService creditScoreService,
                                 ApplicationAccessPolicy accessPolicy,
                                 SparService sparService,
                                 ApplicationSigningService signingService) {
        this.validationService = validationService;
        this.financialService = financialService;
        this.documentService = documentService;
        this.appService = appService;
        this.companyService = companyService;
        this.creditScoreService = creditScoreService;
        this.accessPolicy = accessPolicy;
        this.sparService = sparService;
        this.signingService = signingService;
    }

    // ============================================================
    // POST /apply — skapa ansökan + kör scoring inline
    // ============================================================
    @PreAuthorize("hasRole('COMPANY')")
    @PostMapping("/apply")
    public ResponseEntity<CreditApplicationDataObject> submitApplication(

           @Valid @RequestBody ApplicationSubmission submission,
           @AuthenticationPrincipal CompanyPrincipal principal) {

        accessPolicy.checkCanSubmitFor(principal, submission.orgNumber());

        // TODO: encrypt PII before go-live
        // PII stored in plaintext: companyName, orgNumber, authorizedSignatory
        // No validation or sanitization of inputs

        //hämtar mockad information som matchar "bolagsApi" som i sin tur hämtar ifrån bolagsverket.

        String personalNumber = principal.personalNumber();
        NewApplicationDTO newApplication;
            CompanyValidationApiDTO company = validationService.validateCompanyExists(submission.orgNumber());
            CompanyValidationApiDTO.Signatory signatory = validationService.validateSignatory(company, personalNumber);
        boolean signatoryProtected = sparService.hasProtectedIdentity(personalNumber);
        try {
            CompanyFinancialApiDTO financials = financialService.fetchLatestAnnualReport(submission.orgNumber())
                    .orElseThrow();

            //#TODO CHANGE BRANCH TO BE FETCHED FROM VALIDATION SERVICE
            newApplication = creditScoreService.scoreFromFinancialObject(financials, submission.requestedAmount(), "bransch", submission.orgNumber(), company.companyName(), signatory.name(), submission.purpose(), submission.durationMonths());
        } catch (CompanyRegistryUnavailableException e){

            ApplicationStatus failedScoringStatus;
            String reason;
            if(signatoryProtected){
                failedScoringStatus = ApplicationStatus.PENDING_DOCS;
                reason = "Vi kan inte säkerställa ett underlag för prövning, vänligen skicka in årsredovisning";
            } else{
                failedScoringStatus = ApplicationStatus.PENDING_SCORING;
                reason = "Vår kreditupplysningstjänst är nere eller underlag saknas, invänta handläggare eller vänta på omprövning";
            }

            newApplication = new NewApplicationDTO(submission.requestedAmount(), submission.purpose(), submission.durationMonths(),
                    failedScoringStatus, "could not score", reason, null,
                   company.companyName(), submission.orgNumber(), signatory.name(), 0);
        }
        CreditApplicationDataObject application = appService.saveApplication(newApplication, submission.contactDetails(), signatoryProtected, signatory);
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
    @PreAuthorize("hasAnyRole('COMPANY', 'CASEWORKER')")
    @GetMapping("/{id}")
    public ResponseEntity<ApplicationWithDocumentsDTO> viewApplication(@PathVariable("id") Long id,
                                                                       @AuthenticationPrincipal Authentication authentication) {
        CreditApplicationDataObject app = appService.findApplicationByID(id);
        if(!accessPolicy.canViewSensitiveData(app.orgNumber())){
            throw new NoSuchElementException();
        }




        // Fetch documents for this application
        List<DocumentDTO> docs = documentService.findByApplicationId(id);
        return ResponseEntity.ok(new ApplicationWithDocumentsDTO(app,docs));
    }

    // ============================================================
    // GET /applications — lista alla ansökningar för företaget
    // ============================================================
    @PreAuthorize("hasRole('COMPANY')")
    @GetMapping()
    public ResponseEntity<Page<CreditApplicationDataObject>> listApplications(@AuthenticationPrincipal CompanyPrincipal principal, Pageable pageable) {
            // Get companyId via orgNumber — no caching, hits DB every time
            Long companyID = companyService.getCompanyFromOrgNumber(principal.orgNumber()).id();

        Page<CreditApplicationDataObject> apps = appService.readApplicationsByCompanyDesc(companyID, pageable);

        return ResponseEntity.ok(apps);

    }
    @PreAuthorize("hasRole('CASEWORKER')")
    @GetMapping("/company")
    public ResponseEntity<Page<CreditApplicationDataObject>> listApplicationsByOrgNumber(
            @RequestParam("orgNumber") String orgNumber, Pageable pageable) {
        return ResponseEntity.ok(appService.getApplicationsByOrgNumber(orgNumber, pageable));
    }

    // ============================================================
    // GET /dashboard — startsida för inloggad företagsanvändare
    // ============================================================
    @PreAuthorize("hasRole('COMPANY')")
    @GetMapping("/dashboard")
    public ResponseEntity<Page<ApplicationShortDTO>> dashboard(
            @AuthenticationPrincipal CompanyPrincipal principal, Pageable pageable) {

        Long companyID = companyService.getCompanyFromOrgNumber(principal.orgNumber()).id();

        // Count applications by status
        Page<CreditApplicationDataObject> apps = appService.readApplicationsByCompanyDesc(companyID,pageable);

        return ResponseEntity.ok(apps.map(ApplicationShortDTO::new));
    }


    // Kör om scoring på ett ärende, bara handläggare(och i framtiden systemet själv) ska komma åt denna
    // Ingen body behövs eftersom allt som behövs ligger redan sparat på ärendet
    @PostMapping("rescore/{id}")
    @PreAuthorize("hasRole('CASEWORKER')")
    public ResponseEntity<CreditApplicationDataObject> rescore(@PathVariable("id") Long id) {
        return ResponseEntity.ok(appService.rescoreApplication(id));
    }


    @PreAuthorize("hasAnyRole('COMPANY', 'CASEWORKER')")
    @GetMapping("/{id}/comments")
    public ResponseEntity<List<ApplicationCommentDTO>> listComments(@PathVariable("id") Long id,
                                                                    @AuthenticationPrincipal Object principal) {
        CreditApplicationDataObject app = appService.findApplicationByID(id);
        accessPolicy.checkCanView(principal, app);
        return ResponseEntity.ok(appService.findComments(id));
    }
    @PreAuthorize("hasRole('COMPANY')")
    @PostMapping("/{id}/sign")
    public ResponseEntity<CreditApplicationDataObject> signApplication(@PathVariable("id") Long id,
                                                                @AuthenticationPrincipal CompanyPrincipal principal) {
        CreditApplicationDataObject app = appService.findApplicationByID(id);
        accessPolicy.checkCanView(principal, app);
        return ResponseEntity.ok(signingService.coSign(id, principal.personalNumber()));
    }
}
