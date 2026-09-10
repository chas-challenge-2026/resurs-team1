package se.comerit.resurs.controller;

import org.apache.coyote.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import se.comerit.resurs.dto.CreditApplicationDTO;
import se.comerit.resurs.dto.DocumentDTO;
import se.comerit.resurs.dto.application.ApplicationShortDTO;
import se.comerit.resurs.dto.application.ApplicationWithDocumentsDTO;
import se.comerit.resurs.dto.application.NewApplicationDTO;
import se.comerit.resurs.enums.ApplicationStatus;
import se.comerit.resurs.persistence.model.CreditApplication;
import se.comerit.resurs.service.ApplicationService;
import se.comerit.resurs.service.CompanyService;
import se.comerit.resurs.service.DocumentService;
import se.comerit.resurs.service.ScoringService;

import java.math.BigDecimal;
import java.net.URI;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * ApplicationController – Hanterar kreditansökningar.
 *
 * VARNING: Denna klass innehåller avsiktliga anti-patterns för pedagogiskt syfte.
 * Se docs/known-bugs.md för fullständig lista.
 *
 * Anti-patterns inkluderar:
 *  - JdbcTemplate direkt i kontrollern (ingen service/repository-lager)
 *  - Inline scoring-logik (800+ rader i en metod)
 *  - Audit log som JSON-blob i en kolumn
 *  - Ingen transaktion vid ansökningsskapande
 *  - PII i klartext
 *  - Session-check copy-pasteat i varje metod
 *  - Magic numbers spridda i scoring-logiken
 */
@RestController
@RequestMapping("api/application")
public class ApplicationController {

    private final DocumentService documentService;
    private final ApplicationService appService;
    private final CompanyService companyService; //Swap to CompanyService later
    private final ScoringService creditScoreService;

    public ApplicationController(DocumentService documentService, ApplicationService appService, CompanyService companyService, ScoringService creditScoreService) {
        this.documentService = documentService;
        this.appService = appService;
        this.companyService = companyService;
        this.creditScoreService = creditScoreService;
    }

    // ============================================================
    // GET /apply — visa ansökningsformulär
    // ============================================================
    @GetMapping("/apply")
    public String showApplyForm(HttpSession session, Model model) {
        // Session check copy-pasted in every method — should be an interceptor
        if (session.getAttribute("userId") == null) return "redirect:/login";
        if (!"company".equals(session.getAttribute("role"))) return "redirect:/login";

        model.addAttribute("companyName", session.getAttribute("companyName"));
        model.addAttribute("orgNumber", session.getAttribute("orgNumber"));
        return "apply";
    }

    // ============================================================
    // POST /apply — skapa ansökan + kör scoring inline
    // ============================================================
    @PostMapping("/apply")
    public ResponseEntity<CreditApplicationDTO> submitApplication(
            @RequestParam("orgNumber") String orgNumber,
            @RequestParam("companyName") String companyName,
            @RequestParam("authorizedSignatory") String authorizedSignatory,
            @RequestParam("egetKapital") String egetKapitalStr,
            @RequestParam("totaltKapital") String totaltKapitalStr,
            @RequestParam("omsattningstillgangar") String omsattningstillgangarStr,
            @RequestParam("kortfristigaSkulder") String kortfristigaSkulderStr,
            @RequestParam("totalaSkulder") String totalaSkulderStr,
            @RequestParam("rorelseresultat") String rorelseresultatStr,
            @RequestParam("nettoomsattning") String nettoomsattningStr,
            @RequestParam("requestedAmount") String requestedAmountStr,
            @RequestParam("purpose") String purpose,
            @RequestParam(value = "operativtKassaflode", defaultValue = "") String operativtKassaflodeStr,
            @RequestParam(value = "investeringsKassaflode", defaultValue = "") String investeringsKassaflodeStr,
            @RequestParam(value = "ranteKostnader", defaultValue = "") String ranteKostnaderStr,
            @RequestParam(value = "bransch", defaultValue = "") String bransch,
            HttpSession session) {

        // Session check copy-pasted in every method — should be an interceptor
        if (session.getAttribute("userId") == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!"company".equals(session.getAttribute("role"))) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        // TODO: encrypt PII before go-live
        // PII stored in plaintext: companyName, orgNumber, authorizedSignatory
        // No validation or sanitization of inputs

        // ---- Parse financial inputs (no proper error handling) ----
        double egetKapital = 0;
        double totaltKapital = 0;
        double omsattningstillgangar = 0;
        double kortfristigaSkulder = 0;
        double totalaSkulder = 0;
        double rorelseresultat = 0;
        double nettoomsattning = 0;
        BigDecimal requestedAmount = BigDecimal.ZERO;

        try {
            egetKapital = Double.parseDouble(egetKapitalStr.replace(",", ".").trim());
            totaltKapital = Double.parseDouble(totaltKapitalStr.replace(",", ".").trim());
            omsattningstillgangar = Double.parseDouble(omsattningstillgangarStr.replace(",", ".").trim());
            kortfristigaSkulder = Double.parseDouble(kortfristigaSkulderStr.replace(",", ".").trim());
            totalaSkulder = Double.parseDouble(totalaSkulderStr.replace(",", ".").trim());
            rorelseresultat = Double.parseDouble(rorelseresultatStr.replace(",", ".").trim());
            nettoomsattning = Double.parseDouble(nettoomsattningStr.replace(",", ".").trim());
            requestedAmount = new BigDecimal(requestedAmountStr.replace(",", ".").trim());
        } catch (NumberFormatException e) {
            /*model.addAttribute("error", "Ogiltiga numeriska värden. Kontrollera dina inmatningar.");
            model.addAttribute("companyName", companyName);
            model.addAttribute("orgNumber", orgNumber);*/
            ResponseEntity.badRequest().build();
        }

        NewApplicationDTO scoredApplication = creditScoreService.ScoringEngine(
                operativtKassaflodeStr,
                investeringsKassaflodeStr,
                ranteKostnaderStr,
                totaltKapital,
                egetKapital,
                kortfristigaSkulder,
                omsattningstillgangar,
                totalaSkulder,
                nettoomsattning,
                rorelseresultat,
                requestedAmount,
                bransch,
                orgNumber,
                companyName,
                authorizedSignatory,
                purpose
        );


        // ===========================================================
        // INSERT 2: Skapa ansökan — ingen transaktion, tre separata INSERTs
        // TODO: wrap in @Transactional
        // ===========================================================
        CreditApplicationDTO application =  appService.saveApplication(scoredApplication);



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

        return  ResponseEntity.created( location).body(application);
    }

    // ============================================================
    // GET /application/{id} — visa enskild ansökan
    // ============================================================
    @GetMapping("/{id}")
    public ResponseEntity<ApplicationWithDocumentsDTO> viewApplication(@PathVariable("id") Long id,
                                                                       HttpSession session,
                                                                       Model model) {
        // Session check copy-pasted in every method — should be an interceptor
        if (session.getAttribute("userId") == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        String role = (String) session.getAttribute("role");

        CreditApplicationDTO app;
        if ("caseWorker".equals(role)) {
            app = appService.findApplicationByID(id);
        } else {
            // Company can only see their own applications
            Long companyId = (Long) session.getAttribute("companyId");
            if (companyId == null) {
                // Try to find companyId from orgNumber
                String orgNumber = (String) session.getAttribute("orgNumber");

                try{
                    companyId  = companyService.getCompanyFromOrgNumber(orgNumber).id();
                } catch (NoSuchElementException e){
                    return ResponseEntity.internalServerError().build();
                }
                session.setAttribute("companyId", companyId);
            }

            try{
                app = appService.findApplicationByID(id);
            } catch (NoSuchElementException e){
                return ResponseEntity.notFound().build(); //ansökan hittades inte
            }

        }

        /*
        // Parse audit log — manual JSON string splitting, no proper parser
        String auditLogBlob = (String) app.get("audit_log");
        model.addAttribute("auditLogRaw", auditLogBlob);*/

        // Fetch documents for this application
        List<DocumentDTO> docs = documentService.findByApplicationId(id);


        ApplicationWithDocumentsDTO responseBody = new ApplicationWithDocumentsDTO(app,docs);
        return ResponseEntity.ok(responseBody);
    }

    // ============================================================
    // GET /applications — lista alla ansökningar för företaget
    // ============================================================
    @GetMapping()
    public ResponseEntity<List<CreditApplicationDTO>> listApplications(HttpSession session) {
        // Session check copy-pasted in every method — should be an interceptor
        if (session.getAttribute("userId") == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        //if (!"company".equals(session.getAttribute("role"))) return "redirect:/login";

        String orgNumber = (String) session.getAttribute("orgNumber");


        Long companyID;
        try{
            // Get companyId via orgNumber — no caching, hits DB every time
            companyID = companyService.getCompanyFromOrgNumber(orgNumber).id();
        } catch (NoSuchElementException e) {
            return ResponseEntity.ok(Collections.emptyList());
        }


        List<CreditApplicationDTO> apps = appService.readApplicationsByCompanyDesc(companyID);


        return ResponseEntity.ok(apps);

    }

    // ============================================================
    // GET /dashboard — startsida för inloggad företagsanvändare
    // ============================================================
    @GetMapping("/dashboard")
    public ResponseEntity<List<ApplicationShortDTO>> dashboard(HttpSession session) {
        // Session check copy-pasted in every method — should be an interceptor
        if (session.getAttribute("userId") == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        //if (!"company".equals(session.getAttribute("role"))) return "redirect:/backoffice"; ?? What is this ??

        String orgNumber = (String) session.getAttribute("orgNumber");

        Long companyID;
        try{
            companyID = companyService.getCompanyFromOrgNumber(orgNumber).id();
        } catch (NoSuchElementException e) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        // Count applications by status
        Pageable limit = PageRequest.of(0,5);
        List<CreditApplicationDTO> apps = appService.readApplicationsByCompanyDesc(companyID,limit);


        return ResponseEntity.ok(apps.stream().map(ApplicationShortDTO::new).toList());
    }



}
