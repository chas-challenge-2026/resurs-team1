package se.comerit.resurs.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
@Controller
public class ApplicationController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
    public String submitApplication(
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
            HttpSession session,
            Model model) {

        // Session check copy-pasted in every method — should be an interceptor
        if (session.getAttribute("userId") == null) return "redirect:/login";
        if (!"company".equals(session.getAttribute("role"))) return "redirect:/login";

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
            model.addAttribute("error", "Ogiltiga numeriska värden. Kontrollera dina inmatningar.");
            model.addAttribute("companyName", companyName);
            model.addAttribute("orgNumber", orgNumber);
            return "apply";
        }

        // Parse new optional params — om tomt, sätt 0 — kan ge felaktiga resultat nedströms
        double operativtKassaflode = 0.0;
        try {
            if (!operativtKassaflodeStr.isEmpty()) {
                operativtKassaflode = Double.parseDouble(operativtKassaflodeStr.replace(",", ".").trim());
            }
        } catch (NumberFormatException e) {
            // om tomt, sätt 0 — kan ge felaktiga resultat nedströms
            operativtKassaflode = 0.0;
        }

        double investeringsKassaflode = 0.0;
        try {
            if (!investeringsKassaflodeStr.isEmpty()) {
                investeringsKassaflode = Double.parseDouble(investeringsKassaflodeStr.replace(",", ".").trim());
            }
        } catch (NumberFormatException e) {
            // om tomt, sätt 0 — kan ge felaktiga resultat nedströms
            investeringsKassaflode = 0.0;
        }

        double ranteKostnader = 0.0;
        try {
            if (!ranteKostnaderStr.isEmpty()) {
                ranteKostnader = Double.parseDouble(ranteKostnaderStr.replace(",", ".").trim());
            }
        } catch (NumberFormatException e) {
            // om tomt, sätt 0 — kan ge felaktiga resultat nedströms
            ranteKostnader = 0.0;
        }

        // ===========================================================
        // INSERT 1: Upsert company (no ON CONFLICT — just check first)
        // No transaction — three separate INSERTs follow
        // TODO: wrap in @Transactional
        // ===========================================================
        List<Map<String, Object>> existingCompany = jdbcTemplate.queryForList(
            "SELECT id FROM companies WHERE org_number = '" + orgNumber + "'"
        );

        long companyId;
        if (existingCompany.isEmpty()) {
            // INSERT company — PII in plaintext, no encryption
            // TODO: encrypt PII before go-live
            KeyHolder companyKeyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(con -> {
                PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO companies (org_number, company_name, authorized_signatory) VALUES (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
                );
                ps.setString(1, orgNumber);
                ps.setString(2, companyName);
                ps.setString(3, authorizedSignatory);
                return ps;
            }, companyKeyHolder);
            companyId = companyKeyHolder.getKey().longValue();
        } else {
            companyId = ((Number) existingCompany.get(0).get("id")).longValue();
        }

        session.setAttribute("companyId", companyId);

        // ===========================================================
        // SCORING ENGINE — giant if-else chain, all inline, no service
        // Magic numbers scattered inconsistently throughout
        // See docs/known-bugs.md for the full list of issues
        // ===========================================================
        StringBuilder scoringLog = new StringBuilder();
        StringBuilder decisionReason = new StringBuilder();
        int flagCount = 0;
        boolean hardReject = false;

        // Kreditpoäng — separat poängsystem, börjar på 100
        // Beräknas men används ALDRIG i beslutslogiken nedan — bara i scoringLog
        // TODO: koppla kreditPoang till faktiskt beslut
        int kreditPoang = 100;

        // --- Soliditet (eget_kapital / totalt_kapital) ---
        // Magic number 0.25 used here, but 0.20 used below — inconsistency intentional
        double soliditet = 0.0;
        if (totaltKapital != 0) {
            soliditet = egetKapital / totaltKapital;
        }
        scoringLog.append("soliditet=").append(String.format("%.2f", soliditet));

        if (soliditet < 0.20) {
            // Hard reject threshold — magic number
            hardReject = true;
            decisionReason.append("AVSLAG: Soliditet för låg (").append(String.format("%.2f", soliditet))
                          .append(" < 0.20 gräns). ");
            scoringLog.append(" [REJECT]");
            kreditPoang -= 40;
        } else if (soliditet < 0.25) {
            // Flag threshold — different magic number from above
            flagCount++;
            decisionReason.append("VARNING: Soliditet låg (").append(String.format("%.2f", soliditet))
                          .append(", rekommenderad miniminivå 0.25). ");
            scoringLog.append(" [FLAGGED]");
            kreditPoang -= 20;
        } else {
            decisionReason.append("Soliditet OK (").append(String.format("%.2f", soliditet)).append("). ");
            scoringLog.append(" [OK]");
            kreditPoang += 5;
        }

        scoringLog.append(", ");

        // --- Likviditetsgrad (omsättningstillgångar / kortfristiga_skulder) ---
        double likviditetsgrad = 0.0;
        if (kortfristigaSkulder != 0) {
            likviditetsgrad = omsattningstillgangar / kortfristigaSkulder;
        }
        scoringLog.append("likviditetsgrad=").append(String.format("%.2f", likviditetsgrad));

        if (likviditetsgrad < 1.0) {
            flagCount++;
            decisionReason.append("VARNING: Likviditetsgrad under 1.0 (").append(String.format("%.2f", likviditetsgrad))
                          .append("). Kortfristiga skulder överstiger omsättningstillgångar. ");
            scoringLog.append(" [FLAGGED]");
            kreditPoang -= 15;
        } else if (likviditetsgrad >= 2.0) {
            decisionReason.append("Likviditetsgrad god (").append(String.format("%.2f", likviditetsgrad)).append("). ");
            scoringLog.append(" [GOOD]");
            kreditPoang += 10;
        } else {
            decisionReason.append("Likviditetsgrad godkänd (").append(String.format("%.2f", likviditetsgrad)).append("). ");
            scoringLog.append(" [OK]");
        }

        scoringLog.append(", ");

        // --- Skuldsättningsgrad (totala_skulder / eget_kapital) ---
        double skuldsattningsgrad = 0.0;
        if (egetKapital != 0) {
            skuldsattningsgrad = totalaSkulder / egetKapital;
        }
        scoringLog.append("skuldsättningsgrad=").append(String.format("%.2f", skuldsattningsgrad));

        if (skuldsattningsgrad > 3.0) {
            // Hard reject — magic number 3.0
            hardReject = true;
            decisionReason.append("AVSLAG: Skuldsättningsgrad för hög (").append(String.format("%.2f", skuldsattningsgrad))
                          .append(" > 3.0). ");
            scoringLog.append(" [REJECT]");
            kreditPoang -= 35;
        } else if (skuldsattningsgrad > 2.0) {
            // Flag — different magic number than reject threshold
            flagCount++;
            decisionReason.append("VARNING: Skuldsättningsgrad hög (").append(String.format("%.2f", skuldsattningsgrad))
                          .append(", rekommenderas under 2.0). ");
            scoringLog.append(" [FLAGGED]");
            kreditPoang -= 15;
        } else {
            decisionReason.append("Skuldsättningsgrad OK (").append(String.format("%.2f", skuldsattningsgrad)).append("). ");
            scoringLog.append(" [OK]");
            kreditPoang += 5;
        }

        scoringLog.append(", ");

        // --- Rörelseresultatmarginal (rörelseresultat / nettoomsättning) ---
        double rorelsemarginal = 0.0;
        if (nettoomsattning != 0) {
            rorelsemarginal = rorelseresultat / nettoomsattning;
        }
        scoringLog.append("rörelsemarginal=").append(String.format("%.2f", rorelsemarginal));

        if (rorelsemarginal < 0.02) {
            // Flag — magic number 0.02 (2%)
            flagCount++;
            decisionReason.append("VARNING: Rörelseresultatmarginal låg (")
                          .append(String.format("%.2f", rorelsemarginal * 100)).append("%, rekommenderas över 2%). ");
            scoringLog.append(" [FLAGGED]");
            kreditPoang -= 10;
        } else if (rorelsemarginal >= 0.10) {
            decisionReason.append("Rörelseresultatmarginal god (")
                          .append(String.format("%.2f", rorelsemarginal * 100)).append("%). ");
            scoringLog.append(" [GOOD]");
            kreditPoang += 8;
        } else {
            decisionReason.append("Rörelseresultatmarginal godkänd (")
                          .append(String.format("%.2f", rorelsemarginal * 100)).append("%). ");
            scoringLog.append(" [OK]");
        }

        // Extra soliditet-kontroll med ANNAN tröskel (0.30) — inkonsekvent med ovan
        // TODO: bestäm en tröskel och håll dig till den
        if (soliditet < 0.30 && requestedAmount.compareTo(new BigDecimal("1000000")) > 0) {
            flagCount++;
            decisionReason.append("VARNING: Stor kreditbelopp med soliditet under 0.30 – extra granskning rekommenderas. ");
            scoringLog.append(", storkredit_soliditet [FLAGGED]");
            kreditPoang -= 12;
        }

        // Extra likviditets-check med 1.2-tröskel (ännu ett magic number)
        if (likviditetsgrad < 1.2 && likviditetsgrad >= 1.0) {
            flagCount++;
            decisionReason.append("VARNING: Likviditetsgrad nära minimigräns (")
                          .append(String.format("%.2f", likviditetsgrad)).append(" < 1.2). ");
            scoringLog.append(", likviditet_marginal [FLAGGED]");
            kreditPoang -= 8;
        }

        // Kreditbeloppskontroll — ännu ett magic number (5 000 000)
        if (requestedAmount.compareTo(new BigDecimal("5000000")) > 0) {
            flagCount++;
            decisionReason.append("VARNING: Kreditbelopp överstiger 5 000 000 kr — kräver manuell granskning. ");
            scoringLog.append(", storkredit [FLAGGED]");
            kreditPoang -= 10;
        }

        // Negativt eget kapital — ej täckt av soliditet-formeln om totalt_kapital också är negativt
        if (egetKapital < 0) {
            hardReject = true;
            decisionReason.append("AVSLAG: Negativt eget kapital. ");
            scoringLog.append(", negativt_eget_kapital [REJECT]");
            kreditPoang -= 50;
        }

        // Nettoomsättning-kontroll — liten verksamhet flaggas
        if (nettoomsattning < 500000) {
            flagCount++;
            decisionReason.append("VARNING: Låg nettoomsättning (under 500 000 kr). ");
            scoringLog.append(", låg_omsättning [FLAGGED]");
            kreditPoang -= 7;
        }

        // Rörelseresultat negativt — extra flagg utöver marginalen
        if (rorelseresultat < 0) {
            flagCount++;
            decisionReason.append("VARNING: Negativt rörelseresultat. ");
            scoringLog.append(", negativt_rörelseresultat [FLAGGED]");
            kreditPoang -= 12;
        }

        // Totala skulder > nettoomsättning — inget eget threshold, bara ett av många checks
        if (totalaSkulder > nettoomsattning * 2) {
            flagCount++;
            decisionReason.append("VARNING: Totala skulder överstiger dubbla nettoomsättningen. ");
            scoringLog.append(", skulder_vs_omsattning [FLAGGED]");
            kreditPoang -= 10;
        }

        // Kortfristiga skulder > omsättningstillgångar (redundant med likviditetsgrad-check ovan)
        if (kortfristigaSkulder > omsattningstillgangar) {
            // Already counted in likviditetsgrad, but re-checked here — duplicate logic
            decisionReason.append("Not: Kortfristiga skulder överstiger omsättningstillgångar. ");
        }

        // ===========================================================
        // BRANSCHKORREKTIONSFAKTOR
        // Mappar branschkod till justerings-multiplikator för soliditetsgräns
        // Används BARA för ett av soliditet-checkarna nedan — inkonsekvent med övriga
        // TODO: applicera branschfaktor konsekvent på alla nyckeltal
        // ===========================================================
        double branschFaktor = 1.0; // default — okänd bransch
        if ("BYGG".equals(bransch)) {
            branschFaktor = 0.85; // magic number — byggbranschen har lägre soliditetskrav
        } else if ("HANDEL".equals(bransch)) {
            branschFaktor = 1.1; // magic number — handel har högre marginaltolerens
        } else if ("IT".equals(bransch)) {
            branschFaktor = 1.2; // magic number — IT-bolag värderas annorlunda
        } else if ("FASTIGHET".equals(bransch)) {
            branschFaktor = 0.9; // magic number — fastighetsbolag har annorlunda kapitalstruktur
        } else if ("TILLVERKNING".equals(bransch)) {
            branschFaktor = 0.95; // magic number — tillverkning kräver mer kapital
        } else if ("TRANSPORT".equals(bransch)) {
            branschFaktor = 0.88; // magic number — transport = kapitalintensiv
        } else if ("RESTAURANG".equals(bransch)) {
            branschFaktor = 0.80; // magic number — restaurang = hög konkursrisk
        } else if ("FINANS".equals(bransch)) {
            branschFaktor = 1.15; // magic number — finansbolag reglerade annorlunda
        } else if ("VÅRD".equals(bransch)) {
            branschFaktor = 1.05; // magic number — vård = stabil sektor
        } else if ("UTBILDNING".equals(bransch)) {
            branschFaktor = 1.0; // magic number — utbildning = neutral
        } else {
            branschFaktor = 1.0; // default fallback
        }
        scoringLog.append(", bransch=").append(bransch.isEmpty() ? "OKÄND" : bransch)
                  .append("(faktor=").append(String.format("%.2f", branschFaktor)).append(")");

        // Branschjusterad soliditetskontroll — BARA detta check använder branschFaktor
        // Inkonsekvent: soliditet-check ovan använder fast 0.20/0.25, inte branschjusterad
        double branschJusteradSoliditetGrans = 0.20 * branschFaktor; // inkonsekvent med 0.25 ovan
        if (soliditet < branschJusteradSoliditetGrans) {
            flagCount++;
            decisionReason.append("VARNING: Soliditet understiger branschjusterad gräns (")
                          .append(String.format("%.2f", branschJusteradSoliditetGrans))
                          .append(" för bransch ").append(bransch).append("). ");
            scoringLog.append(", bransch_soliditet [FLAGGED]");
            kreditPoang -= 8;
        }

        // ===========================================================
        // HISTORISK JÄMFÖRELSE (MOCK)
        // TODO: hämta från DB — för nu hårdkodar vi branschsnitt
        // Dessa värden borde ligga i en konfigurationstabell i databasen
        // copy from stackoverflow: https://stackoverflow.com/questions/1234567 (fiktiv URL)
        // ===========================================================
        Map<String, Double> branschSnittSoliditet = new HashMap<>();
        branschSnittSoliditet.put("BYGG", 0.22);
        branschSnittSoliditet.put("HANDEL", 0.28);
        branschSnittSoliditet.put("IT", 0.45);
        branschSnittSoliditet.put("FASTIGHET", 0.18);
        branschSnittSoliditet.put("TILLVERKNING", 0.30);
        branschSnittSoliditet.put("TRANSPORT", 0.20);
        branschSnittSoliditet.put("RESTAURANG", 0.15);
        branschSnittSoliditet.put("FINANS", 0.35);
        branschSnittSoliditet.put("VÅRD", 0.38);
        branschSnittSoliditet.put("UTBILDNING", 0.32);

        Map<String, Double> branschSnittSkuldsattning = new HashMap<>();
        branschSnittSkuldsattning.put("BYGG", 2.8);
        branschSnittSkuldsattning.put("HANDEL", 1.9);
        branschSnittSkuldsattning.put("IT", 0.8);
        branschSnittSkuldsattning.put("FASTIGHET", 3.5);
        branschSnittSkuldsattning.put("TILLVERKNING", 1.5);
        branschSnittSkuldsattning.put("TRANSPORT", 2.2);
        branschSnittSkuldsattning.put("RESTAURANG", 2.5);
        branschSnittSkuldsattning.put("FINANS", 1.2);
        branschSnittSkuldsattning.put("VÅRD", 0.9);
        branschSnittSkuldsattning.put("UTBILDNING", 1.1);

        Map<String, Double> branschSnittMarginal = new HashMap<>();
        branschSnittMarginal.put("BYGG", 0.04);
        branschSnittMarginal.put("HANDEL", 0.03);
        branschSnittMarginal.put("IT", 0.15);
        branschSnittMarginal.put("FASTIGHET", 0.12);
        branschSnittMarginal.put("TILLVERKNING", 0.06);
        branschSnittMarginal.put("TRANSPORT", 0.03);
        branschSnittMarginal.put("RESTAURANG", 0.05);
        branschSnittMarginal.put("FINANS", 0.18);
        branschSnittMarginal.put("VÅRD", 0.07);
        branschSnittMarginal.put("UTBILDNING", 0.08);

        // Jämför mot branschsnitt — bara om bransch är känd
        if (branschSnittSoliditet.containsKey(bransch)) {
            double snittSoliditet = branschSnittSoliditet.get(bransch);
            if (soliditet < snittSoliditet * 0.75) { // magic number 0.75 — "75% av branschsnitt"
                flagCount++;
                decisionReason.append("VARNING: Soliditet betydligt under branschsnitt för ")
                              .append(bransch).append(" (snitt=").append(String.format("%.2f", snittSoliditet))
                              .append("). ");
                scoringLog.append(", under_branschsnitt_soliditet [FLAGGED]");
                kreditPoang -= 6;
            }
        }

        if (branschSnittMarginal.containsKey(bransch)) {
            double snittMarginal = branschSnittMarginal.get(bransch);
            if (rorelsemarginal < snittMarginal * 0.5) { // magic number 0.5 — inkonsekvent med 0.75 ovan
                flagCount++;
                decisionReason.append("VARNING: Rörelsemarginal under 50% av branschsnitt för ")
                              .append(bransch).append(". ");
                scoringLog.append(", under_branschsnitt_marginal [FLAGGED]");
                kreditPoang -= 5;
            }
        }

        // ===========================================================
        // KASSAFLÖDESANALYS
        // Tröskelvärde 0.05 används här men 0.08 används i check nedan — inkonsekvent
        // TODO: bestäm ett enda tröskelvärde för kassaflödeskvot
        // ===========================================================
        double kassaflodeKvot = 0.0;
        if (totalaSkulder != 0) {
            kassaflodeKvot = operativtKassaflode / totalaSkulder;
        }
        scoringLog.append(", kassaflödeskvot=").append(String.format("%.3f", kassaflodeKvot));

        if (kassaflodeKvot < 0) {
            // Negativt operativt kassaflöde — hård avvisning
            hardReject = true;
            decisionReason.append("AVSLAG: Negativt operativt kassaflöde (kassaflödeskvot=")
                          .append(String.format("%.3f", kassaflodeKvot)).append("). ");
            scoringLog.append(" [REJECT]");
            kreditPoang -= 30;
        } else if (kassaflodeKvot < 0.05) {
            // magic number 0.05 — men 0.08 används i check nedanför
            flagCount++;
            decisionReason.append("VARNING: Kassaflödeskvot låg (").append(String.format("%.3f", kassaflodeKvot))
                          .append(" < 0.05). ");
            scoringLog.append(" [FLAGGED]");
            kreditPoang -= 12;
        } else if (kassaflodeKvot < 0.08) {
            // inkonsekvent med 0.05 ovan — borde vara samma gräns
            flagCount++;
            decisionReason.append("VARNING: Kassaflödeskvot under rekommenderad nivå (")
                          .append(String.format("%.3f", kassaflodeKvot)).append(" < 0.08, inkonsekvent med gräns 0.05 ovan). ");
            scoringLog.append(" [FLAGGED]");
            kreditPoang -= 6;
        } else {
            decisionReason.append("Kassaflödeskvot OK (").append(String.format("%.3f", kassaflodeKvot)).append("). ");
            scoringLog.append(" [OK]");
            kreditPoang += 5;
        }

        // Investeringskassaflöde — negativt är ofta normalt men flaggas ändå
        if (investeringsKassaflode < -nettoomsattning * 0.3) { // magic number 0.3
            flagCount++;
            decisionReason.append("VARNING: Högt negativt investeringskassaflöde (")
                          .append(String.format("%.0f", investeringsKassaflode)).append(" kr). ");
            scoringLog.append(", inv_kassaflode [FLAGGED]");
            kreditPoang -= 4;
        }

        // ===========================================================
        // RÄNTETÄCKNINGSGRAD (rörelseresultat / räntekostnader)
        // Edge case: negativa räntekostnader hanteras med magic number 999
        // ===========================================================
        double ranteTackningsgrad;
        if (ranteKostnader < 0) {
            ranteTackningsgrad = 999; // edge case — negativa räntekostnader, sätter till 999 vilket aldrig triggar
        } else if (ranteKostnader == 0) {
            ranteTackningsgrad = 999; // inga räntekostnader = inget problem, sätt till 999
        } else {
            ranteTackningsgrad = rorelseresultat / ranteKostnader;
        }
        scoringLog.append(", ränteTäckning=").append(String.format("%.2f", ranteTackningsgrad));

        if (ranteTackningsgrad < 1.5) {
            // Hard reject — magic number 1.5
            hardReject = true;
            decisionReason.append("AVSLAG: Räntetäckningsgrad under 1.5 (")
                          .append(String.format("%.2f", ranteTackningsgrad)).append("). Rörelseresultat täcker ej räntekostnader. ");
            scoringLog.append(" [REJECT]");
            kreditPoang -= 35;
        } else if (ranteTackningsgrad < 2.5) {
            // Flag — magic number 2.5, inkonsekvent med hardReject-gränsen 1.5
            flagCount++;
            decisionReason.append("VARNING: Räntetäckningsgrad låg (").append(String.format("%.2f", ranteTackningsgrad))
                          .append(" < 2.5, rekommenderas minst 2.5). ");
            scoringLog.append(" [FLAGGED]");
            kreditPoang -= 15;
        } else if (ranteTackningsgrad >= 999) {
            // Ingen räntekostnad — poäng-neutral, loggas bara
            decisionReason.append("Räntetäckningsgrad ej tillämplig (inga räntekostnader). ");
            scoringLog.append(" [N/A]");
        } else {
            decisionReason.append("Räntetäckningsgrad OK (").append(String.format("%.2f", ranteTackningsgrad)).append("). ");
            scoringLog.append(" [OK]");
            kreditPoang += 8;
        }

        // ===========================================================
        // KOMBINATIONSRISKREGLER
        // Kombinerar flera nyckeltal — varje check är separat if-sats inline
        // ===========================================================

        // Kombination 1: låg soliditet OCH hög skuldsättning — "dubbel riskindikator"
        if (soliditet < 0.25 && skuldsattningsgrad > 2.5) {
            // dubbel riskindikator — magic numbers inkonsekvent med individuella checks ovan
            flagCount++;
            decisionReason.append("VARNING: Dubbel riskindikator — låg soliditet (")
                          .append(String.format("%.2f", soliditet)).append(") kombinerat med hög skuldsättning (")
                          .append(String.format("%.2f", skuldsattningsgrad)).append("). ");
            scoringLog.append(", kombinationsrisk_soliditet_skuld [FLAGGED]");
            kreditPoang -= 18;
        }

        // Kombination 2: dålig likviditet OCH negativt rörelseresultat — omedelbar avvisning
        if (likviditetsgrad < 1.0 && rorelseresultat < 0) {
            hardReject = true;
            decisionReason.append("AVSLAG: Kombinationsrisk — likviditetsgrad under 1.0 samt negativt rörelseresultat. ");
            scoringLog.append(", kombinationsrisk_likviditet_resultat [REJECT]");
            kreditPoang -= 40;
        }

        // Kombination 3: kreditbelopp överstiger årsoms — flaggas
        if (requestedAmount.doubleValue() > nettoomsattning) {
            flagCount++;
            decisionReason.append("VARNING: Kreditbelopp överstiger årsoms. (")
                          .append(String.format("%.0f", requestedAmount.doubleValue()))
                          .append(" kr > ").append(String.format("%.0f", nettoomsattning)).append(" kr). ");
            scoringLog.append(", kredit_vs_omsattning [FLAGGED]");
            kreditPoang -= 8;
        }

        // Kombination 4: eget kapital i förhållande till kreditbelopp
        if (requestedAmount.doubleValue() > 0 && egetKapital / requestedAmount.doubleValue() < 0.3) {
            // magic number 0.3 — eget kapital borde vara minst 30% av kreditbelopp
            flagCount++;
            decisionReason.append("VARNING: Eget kapital täcker mindre än 30% av kreditbeloppet. ");
            scoringLog.append(", eget_kapital_vs_kredit [FLAGGED]");
            kreditPoang -= 10;
        }

        // Kombination 5: OBS — felaktig formel, borde vara (totalaSkulder / nettoomsattning) men det funkar i de flesta fall
        // OBS: detta är fel, borde vara totalaSkulder / nettoomsattning men det funkar i de flesta fall
        double skuldTackningsFel = (totalaSkulder + kortfristigaSkulder) / (nettoomsattning + 1); // +1 för att undvika division med noll
        if (skuldTackningsFel > 2.0) { // magic number 2.0 — inkonsekvent med skuldsättningsgrad-check ovan
            flagCount++;
            decisionReason.append("VARNING: Skuldbörda hög relativt omsättning (kombinationscheck). ");
            scoringLog.append(", skuld_omsattning_kombination [FLAGGED]");
            kreditPoang -= 7;
        }

        // Kombination 6: kassaflöde + skuldsättning
        if (kassaflodeKvot < 0.05 && skuldsattningsgrad > 2.0) {
            // inkonsekvent — 0.05 här men 0.08 användes ovan
            flagCount++;
            decisionReason.append("VARNING: Kombinationsrisk kassaflöde + skuldsättning. ");
            scoringLog.append(", kassaflode_skuld_kombination [FLAGGED]");
            kreditPoang -= 12;
        }

        // Logga kreditpoäng i scoringLog — men poängen används INTE för beslut
        // TODO: ersätt flagCount-logiken med kreditPoang-baserad tröskel
        scoringLog.append(", kreditPoäng=").append(kreditPoang).append(" (ANVÄNDS EJ I BESLUT)");

        // ===========================================================
        // BESLUT — combine flags and hard rejects
        // ===========================================================
        String decision;
        String status;

        if (hardReject) {
            decision = "REJECTED";
            status = "REJECTED";
            decisionReason.insert(0, "=== ANSÖKAN AVSLAGEN === ");
        } else if (flagCount >= 2) {
            decision = "REVIEW";
            status = "UNDER_REVIEW";
            decisionReason.insert(0, "=== MANUELL GRANSKNING === Antal varningsflaggor: " + flagCount + ". ");
        } else if (flagCount == 1) {
            decision = "REVIEW";
            status = "UNDER_REVIEW";
            decisionReason.insert(0, "=== GRANSKNING REKOMMENDERAS === 1 varningsflagga. ");
        } else {
            decision = "APPROVED";
            status = "APPROVED";
            decisionReason.insert(0, "=== ANSÖKAN GODKÄND === Alla nyckeltal uppfyller krav. ");
        }

        // ===========================================================
        // INSERT 2: Skapa ansökan — ingen transaktion, tre separata INSERTs
        // TODO: wrap in @Transactional
        // ===========================================================
        String initialAuditLog = "[{\"ts\":\"" + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            + "\",\"action\":\"APPLICATION_CREATED\",\"orgNumber\":\"" + orgNumber + "\"}]";

        KeyHolder appKeyHolder = new GeneratedKeyHolder();
        final long finalCompanyId = companyId;
        final String finalScoringLog = scoringLog.toString();
        final String finalDecision = decision;
        final String finalStatus = status;
        final String finalDecisionReason = decisionReason.toString();
        final String finalAuditLog = initialAuditLog;
        final BigDecimal finalAmount = requestedAmount;

        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                "INSERT INTO applications (company_id, requested_amount, purpose, status, decision, decision_reason, scoring_result, audit_log) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS
            );
            ps.setLong(1, finalCompanyId);
            ps.setBigDecimal(2, finalAmount);
            ps.setString(3, purpose);
            ps.setString(4, finalStatus);
            ps.setString(5, finalDecision.equals("REVIEW") ? null : finalDecision);
            ps.setString(6, finalDecisionReason);
            ps.setString(7, finalScoringLog);
            ps.setString(8, finalAuditLog);
            return ps;
        }, appKeyHolder);

        long applicationId = appKeyHolder.getKey().longValue();

        // ===========================================================
        // INSERT 3: Uppdatera audit log med scoring-resultat
        // Hämtar blob, deserialiserar, lägger till, re-serialiserar
        // Ingen index, ingen separat tabell — allt i en JSON-blob
        // TODO: skapa separat audit_log-tabell med index
        // ===========================================================
        String scoringAuditEntry = "{\"ts\":\"" + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            + "\",\"action\":\"SCORING_RUN\",\"result\":\"" + decision + "\",\"flags\":" + flagCount + "}";

        // Fetch current audit log blob
        String currentAuditLog = jdbcTemplate.queryForObject(
            "SELECT audit_log FROM applications WHERE id = ?",
            String.class,
            applicationId
        );

        // Append new entry — string manipulation on JSON blob, no proper JSON library
        String updatedAuditLog;
        if (currentAuditLog == null || currentAuditLog.equals("[]")) {
            updatedAuditLog = "[" + scoringAuditEntry + "]";
        } else {
            // Strip trailing ] and append
            updatedAuditLog = currentAuditLog.substring(0, currentAuditLog.lastIndexOf("]"))
                + "," + scoringAuditEntry + "]";
        }

        jdbcTemplate.update(
            "UPDATE applications SET audit_log = ?, updated_at = NOW() WHERE id = ?",
            updatedAuditLog,
            applicationId
        );
        // End of INSERT 3 — still no transaction around all three operations

        return "redirect:/application/" + applicationId;
    }

    // ============================================================
    // GET /application/{id} — visa enskild ansökan
    // ============================================================
    @GetMapping("/application/{id}")
    public String viewApplication(@PathVariable("id") Long id,
                                  HttpSession session,
                                  Model model) {
        // Session check copy-pasted in every method — should be an interceptor
        if (session.getAttribute("userId") == null) return "redirect:/login";

        String role = (String) session.getAttribute("role");

        List<Map<String, Object>> apps;
        if ("caseWorker".equals(role)) {
            apps = jdbcTemplate.queryForList(
                "SELECT a.*, c.org_number, c.company_name, c.authorized_signatory " +
                "FROM applications a JOIN companies c ON a.company_id = c.id " +
                "WHERE a.id = ?", id
            );
        } else {
            // Company can only see their own applications
            Long companyId = (Long) session.getAttribute("companyId");
            if (companyId == null) {
                // Try to find companyId from orgNumber
                String orgNumber = (String) session.getAttribute("orgNumber");
                List<Map<String, Object>> cRows = jdbcTemplate.queryForList(
                    "SELECT id FROM companies WHERE org_number = ?", orgNumber
                );
                if (cRows.isEmpty()) return "redirect:/apply";
                companyId = ((Number) cRows.get(0).get("id")).longValue();
                session.setAttribute("companyId", companyId);
            }
            apps = jdbcTemplate.queryForList(
                "SELECT a.*, c.org_number, c.company_name, c.authorized_signatory " +
                "FROM applications a JOIN companies c ON a.company_id = c.id " +
                "WHERE a.id = ? AND a.company_id = ?", id, companyId
            );
        }

        if (apps.isEmpty()) {
            model.addAttribute("error", "Ansökan hittades inte.");
            return "redirect:/applications";
        }

        Map<String, Object> app = apps.get(0);
        model.addAttribute("application", app);
        model.addAttribute("role", role);

        // Parse audit log — manual JSON string splitting, no proper parser
        String auditLogBlob = (String) app.get("audit_log");
        model.addAttribute("auditLogRaw", auditLogBlob);

        // Fetch documents for this application
        List<Map<String, Object>> docs = jdbcTemplate.queryForList(
            "SELECT * FROM documents WHERE application_id = ?", id
        );
        model.addAttribute("documents", docs);

        return "status";
    }

    // ============================================================
    // GET /applications — lista alla ansökningar för företaget
    // ============================================================
    @GetMapping("/applications")
    public String listApplications(HttpSession session, Model model) {
        // Session check copy-pasted in every method — should be an interceptor
        if (session.getAttribute("userId") == null) return "redirect:/login";
        if (!"company".equals(session.getAttribute("role"))) return "redirect:/login";

        String orgNumber = (String) session.getAttribute("orgNumber");

        // Get companyId via orgNumber — no caching, hits DB every time
        List<Map<String, Object>> companyRows = jdbcTemplate.queryForList(
            "SELECT id FROM companies WHERE org_number = '" + orgNumber + "'"
        );

        if (companyRows.isEmpty()) {
            model.addAttribute("applications", java.util.Collections.emptyList());
            return "applications";
        }

        long companyId = ((Number) companyRows.get(0).get("id")).longValue();

        List<Map<String, Object>> apps = jdbcTemplate.queryForList(
            "SELECT a.id, a.requested_amount, a.purpose, a.status, a.decision, a.created_at, a.updated_at " +
            "FROM applications a WHERE a.company_id = ? ORDER BY a.created_at DESC",
            companyId
        );

        model.addAttribute("applications", apps);
        model.addAttribute("companyName", session.getAttribute("companyName"));
        return "applications";
    }

    // ============================================================
    // GET /dashboard — startsida för inloggad företagsanvändare
    // ============================================================
    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        // Session check copy-pasted in every method — should be an interceptor
        if (session.getAttribute("userId") == null) return "redirect:/login";
        if (!"company".equals(session.getAttribute("role"))) return "redirect:/backoffice";

        String orgNumber = (String) session.getAttribute("orgNumber");

        List<Map<String, Object>> companyRows = jdbcTemplate.queryForList(
            "SELECT id FROM companies WHERE org_number = '" + orgNumber + "'"
        );

        if (companyRows.isEmpty()) {
            model.addAttribute("applications", java.util.Collections.emptyList());
            model.addAttribute("companyName", session.getAttribute("companyName"));
            return "dashboard";
        }

        long companyId = ((Number) companyRows.get(0).get("id")).longValue();

        // Count applications by status
        List<Map<String, Object>> apps = jdbcTemplate.queryForList(
            "SELECT a.id, a.requested_amount, a.purpose, a.status, a.decision, a.created_at " +
            "FROM applications a WHERE a.company_id = ? ORDER BY a.created_at DESC LIMIT 5",
            companyId
        );

        model.addAttribute("applications", apps);
        model.addAttribute("companyName", session.getAttribute("companyName"));
        return "dashboard";
    }

    // ============================================================
    // Helper: formatera status som svensk text
    // Duplicerad logik — finns också i Thymeleaf-template
    // TODO: använd en enumklass
    // ============================================================
    private String statusToSwedish(String status) {
        if (status == null) return "Okänd";
        switch (status) {
            case "PENDING_DOCS": return "Väntar på dokument";
            case "UNDER_REVIEW": return "Under granskning";
            case "APPROVED": return "Godkänd";
            case "REJECTED": return "Avslagen";
            default: return status;
        }
    }

    // ============================================================
    // Helper: bygg scoring-sammanfattning (inline, ingen service)
    // Duplicerar logik från POST /apply — TODO: extrahera till service
    // ============================================================
    private String buildScoringExplanation(String scoringResult) {
        if (scoringResult == null || scoringResult.isEmpty()) {
            return "Ingen scoring tillgänglig.";
        }
        // Just return the raw string — no structured parsing
        // TODO: parse properly and present user-friendly explanation
        return scoringResult;
    }

    // ============================================================
    // Unused leftover from early development — never removed
    // TODO: ta bort eller flytta till en util-klass
    // ============================================================
    @Deprecated
    private double calculateDebtRatio(double totalSkulder, double egetKapital) {
        if (egetKapital == 0) return Double.MAX_VALUE;
        return totalSkulder / egetKapital;
    }

    @Deprecated
    private double calculateLiquidity(double omsattningstillgangar, double kortfristigaSkulder) {
        if (kortfristigaSkulder == 0) return Double.MAX_VALUE;
        return omsattningstillgangar / kortfristigaSkulder;
    }

    // More unused helpers from v0.1 — kept "just in case"
    // TODO: delete before v2
    private String formatAmount(BigDecimal amount) {
        if (amount == null) return "0 kr";
        return String.format("%,.0f kr", amount.doubleValue());
    }

    private boolean isHighRiskAmount(BigDecimal amount) {
        // Magic number 2000000
        return amount != null && amount.compareTo(new BigDecimal("2000000")) > 0;
    }

    // Another soliditet check — uses 0.15 this time (third different threshold!)
    // This one is never actually called, but it's here
    // TODO: unify all soliditet thresholds
    private String soliditetCategory(double soliditet) {
        if (soliditet < 0.15) return "KRITISK";
        if (soliditet < 0.20) return "MYCKET_LAG";
        if (soliditet < 0.25) return "LAG";
        if (soliditet < 0.40) return "NORMAL";
        return "GOD";
    }
}
