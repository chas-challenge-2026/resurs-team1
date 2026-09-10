package se.comerit.resurs.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.comerit.resurs.config.ScoringThresholds;
import se.comerit.resurs.dto.application.NewApplicationDTO;
import se.comerit.resurs.enums.ApplicationStatus;
import se.comerit.resurs.persistence.BranchRepository;
import se.comerit.resurs.persistence.CompanyRepository;
import se.comerit.resurs.persistence.model.Branch;
import se.comerit.resurs.persistence.model.Company;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class ScoringService {

    private final CompanyRepository companyRepository;
    private final BranchRepository branchRepository;
    private final ScoringThresholds thresholds;


    public ScoringService(CompanyRepository companyRepository, BranchRepository branchRepository, ScoringThresholds threshold) {
        this.companyRepository = companyRepository;
        this.branchRepository = branchRepository;
        this.thresholds = threshold;
    }
    //#TODO  Possibly split this into separate functions while passing a mutable state from function to function.
    @Transactional
    public NewApplicationDTO ScoringEngine(
            double operativtKassaflode,
            double investeringsKassaflode,
            double ranteKostnader,
            double totaltKapital,
            double egetKapital,
            double kortfristigaSkulder,
            double omsattningstillgangar,
            double totalaSkulder,
            double nettoomsattning,
            double rorelseresultat,
            BigDecimal requestedAmount,
            String bransch,
            String orgNumber,
            String companyName,
            String authorizedSignatory,
            String purpose
            )
    {

        ApplicationStatus finalStatus = null;
        String finalDecision = null;
        ScoringState state = new ScoringState();

        Company company = null;


        // ===========================================================
        // INSERT 1: Upsert company (no ON CONFLICT — just check first)
        // No transaction — three separate INSERTs follow
        // TODO: wrap in @Transactional
        // ===========================================================

        Optional<Company> existingCompany = companyRepository.findByOrgNumber(orgNumber);


        if (existingCompany.isEmpty()) {
                // INSERT company — PII in plaintext, no encryption
                // TODO: encrypt PII before go-live
                company = companyRepository.save(new Company(orgNumber, companyName, authorizedSignatory));
        }

            //session.setAttribute("companyId", companyId); Keeping this for now... incase its needed

            // ===========================================================
            // SCORING ENGINE — giant if-else chain, all inline, no service
            // Magic numbers scattered inconsistently throughout
            // See docs/known-bugs.md for the full list of issues
            // ===========================================================




            // --- Soliditet (eget_kapital / totalt_kapital) ---
            // Magic number 0.25 used here, but 0.20 used below — inconsistency intentional
            double soliditet = 0.0;
            if (totaltKapital != 0) {
                soliditet = egetKapital / totaltKapital;
            }
            solidityCheck(soliditet,requestedAmount,state);

            // --- Likviditetsgrad (omsättningstillgångar / kortfristiga_skulder) ---
            double likviditetsgrad = 0.0;
            if (kortfristigaSkulder != 0) {
                likviditetsgrad = omsattningstillgangar / kortfristigaSkulder;
            }
            liquidityCheck(likviditetsgrad,state);

            // --- Skuldsättningsgrad (totala_skulder / eget_kapital) ---
            double skuldsattningsgrad = 0.0;
            if (egetKapital != 0) {
                skuldsattningsgrad = totalaSkulder / egetKapital;
            }

            checkDebtRatio(skuldsattningsgrad,state);


            // --- Rörelseresultatmarginal (rörelseresultat / nettoomsättning) ---
            double rorelsemarginal = 0.0;
            if (nettoomsattning != 0) {
                rorelsemarginal = rorelseresultat / nettoomsattning;
            }
            checkOperatingMargin(rorelsemarginal,state);


            ///////////
            //Separata och individuella checks som inte grupperas till andra checks.
            /////////

            // Kreditbeloppskontroll — ännu ett magic number (5 000 000)
            if (requestedAmount.compareTo(new BigDecimal(thresholds.credit().extreme_credit())) > 0) {
                state.flagCount++;
                state.decisionReason.append("VARNING: Kreditbelopp överstiger ").append(thresholds.credit().extreme_credit()).append(" kr — kräver manuell granskning. ");
                state.scoringLog.append(", storkredit [FLAGGED]");
                state.kreditPoang -= 10;
            }

            // Negativt eget kapital — ej täckt av soliditet-formeln om totalt_kapital också är negativt
            if (egetKapital < 0) {
                state.hardReject = true;
                state.decisionReason.append("AVSLAG: Negativt eget kapital. ");
                state.scoringLog.append(", negativt_eget_kapital [REJECT]");
                state.kreditPoang -= 50;
            }

            // Nettoomsättning-kontroll — liten verksamhet flaggas
            if (nettoomsattning < thresholds.turnover().low()) {
                state.flagCount++;
                state.decisionReason.append("VARNING: Låg nettoomsättning (under ").append(thresholds.turnover().low()).append(" kr). ");
                state.scoringLog.append(", låg_omsättning [FLAGGED]");
                state.kreditPoang -= 7;
            }

            // Rörelseresultat negativt — extra flagg utöver marginalen
            if (rorelseresultat < 0) {
                state.flagCount++;
                state.decisionReason.append("VARNING: Negativt rörelseresultat. ");
                state.scoringLog.append(", negativt_rörelseresultat [FLAGGED]");
                state.kreditPoang -= 12;
            }

            // Totala skulder > nettoomsättning — inget eget threshold, bara ett av många checks
            if (totalaSkulder > nettoomsattning * 2) {
                state.flagCount++;
                state.decisionReason.append("VARNING: Totala skulder överstiger dubbla nettoomsättningen. ");
                state.scoringLog.append(", skulder_vs_omsattning [FLAGGED]");
                state.kreditPoang -= 10;
            }

            // Kortfristiga skulder > omsättningstillgångar (redundant med likviditetsgrad-check ovan)
            if (kortfristigaSkulder > omsattningstillgangar) {
                // Already counted in likviditetsgrad, but re-checked here — duplicate logic
                state.decisionReason.append("Not: Kortfristiga skulder överstiger omsättningstillgångar. ");
            }

            // ===========================================================
            // BRANSCHKORREKTIONSFAKTOR
            // Mappar branschkod till justerings-multiplikator för soliditetsgräns
            // Används BARA för ett av soliditet-checkarna nedan — inkonsekvent med övriga
            // TODO: applicera branschfaktor konsekvent på alla nyckeltal
            // ===========================================================

           branchAdjustedChecks( bransch, rorelsemarginal, soliditet, state);

            // ===========================================================
            // KASSAFLÖDESANALYS
            // Tröskelvärde 0.05 används här men 0.08 används i check nedan — inkonsekvent
            // TODO: bestäm ett enda tröskelvärde för kassaflödeskvot
            // ===========================================================
            double kassaflodeKvot = 0.0;
            if (totalaSkulder != 0) {
                kassaflodeKvot = operativtKassaflode / totalaSkulder;
            }
            cashflowCheck( kassaflodeKvot, nettoomsattning,investeringsKassaflode, state);

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

            interestCoverageCheck(ranteTackningsgrad, state);


            // ===========================================================
            // KOMBINATIONSRISKREGLER
            // Kombinerar flera nyckeltal — varje check är separat if-sats inline
            // ===========================================================

            // Kombination 1: låg soliditet OCH hög skuldsättning — "dubbel riskindikator"
            checkLowSolidityHighDebtRatio(soliditet,skuldsattningsgrad,state);

            // Kombination 2: dålig likviditet OCH negativt rörelseresultat — omedelbar avvisning
            checkLowLiquidityNegativeOperational(likviditetsgrad,rorelseresultat,state);

            // Kombination 3: kreditbelopp överstiger årsoms — flaggas
            checkCreditAboveTurnover( requestedAmount, nettoomsattning, state);

            // Kombination 4: eget kapital i förhållande till kreditbelopp
            checkLowCapitalCreditRatio(requestedAmount,egetKapital,state);

            // Kombination 5: OBS — felaktig formel, borde vara (totalaSkulder / nettoomsattning) men det funkar i de flesta fall
            checkDebtAcknowledgementError(totalaSkulder,kortfristigaSkulder,nettoomsattning,state);

            // Kombination 6: kassaflöde + skuldsättning
            checkCashFlowToDebtRatio( kassaflodeKvot, skuldsattningsgrad, state);

            // Logga kreditpoäng i scoringLog — men poängen används INTE för beslut
            // Ersätt flagCount-logiken med kreditPoang-baserad tröskel //Done /Jonathan
            state.scoringLog.append(", kreditPoäng=").append(state.kreditPoang);

            // ===========================================================
            // BESLUT — combine flags and hard rejects
            // ===========================================================
            Decision decision = finalDecision(state);

        company = existingCompany.orElse(company);

        return new NewApplicationDTO(requestedAmount, purpose, decision.finalStatus, decision.finalDecision, state.decisionReason.toString(), state.scoringLog.toString(), company.getCompany_name(), company.getOrg_number(), company.getAuthorized_signatory(),state.flagCount);

    }


    // ============================================================
    // Helper: formatera status som svensk text
    // Duplicerad logik — finns också i Thymeleaf-template
    // TODO: använd en enumklass
    // ============================================================
    private String statusToSwedish(ApplicationStatus status) {
        if (status == null) return "Okänd";
        switch (status) {
            case ApplicationStatus.PENDING_DOCS: return "Väntar på dokument";
            case ApplicationStatus.UNDER_REVIEW: return "Under granskning";
            case ApplicationStatus.APPROVED: return "Godkänd";
            case ApplicationStatus.REJECTED: return "Avslagen";
            default: return status.toString();
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

    //Final DecisionLogic
    private Decision finalDecision(ScoringState state){
        String finalDecision;
        ApplicationStatus finalStatus;
        if (state.hardReject || state.kreditPoang < thresholds.finalScoreThresholds().reject()) {
            finalDecision = "REJECTED";
            finalStatus = ApplicationStatus.REJECTED;
            state.decisionReason.insert(0, "=== ANSÖKAN AVSLAGEN === ");
        } else if (state.kreditPoang < thresholds.finalScoreThresholds().manual_review()) {
            finalDecision = "REVIEW";
            finalStatus = ApplicationStatus.UNDER_REVIEW;
            state.decisionReason.insert(0, "=== MANUELL GRANSKNING === Antal varningsflaggor: " + state.flagCount + ". ");
        } else if (state.kreditPoang < thresholds.finalScoreThresholds().flag_review()) {
            finalDecision = "REVIEW";
            finalStatus = ApplicationStatus.UNDER_REVIEW;
            state.decisionReason.insert(0, "=== GRANSKNING REKOMMENDERAS === 1 varningsflagga. ");
        } else {
            finalDecision = "APPROVED";
            finalStatus = ApplicationStatus.APPROVED;
            state.decisionReason.insert(0, "=== ANSÖKAN GODKÄND === Alla nyckeltal uppfyller krav. ");
        }
        return new Decision(finalDecision,finalStatus);
    }

    private record Decision(
            String finalDecision,
            ApplicationStatus finalStatus
    ){}


    //Combinatory checks
    private void checkLowSolidityHighDebtRatio(double soliditet, double skuldsattningsgrad, ScoringState state){
        if (soliditet < thresholds.solidity().low() && skuldsattningsgrad > thresholds.debtRatio().high()) {
            // dubbel riskindikator — magic numbers inkonsekvent med individuella checks ovan
            state.flagCount++;
            state.decisionReason.append("VARNING: Dubbel riskindikator — låg soliditet (")
                    .append(String.format("%.2f", soliditet)).append(") kombinerat med hög skuldsättning (")
                    .append(String.format("%.2f", skuldsattningsgrad)).append("). ");
            state.scoringLog.append(", kombinationsrisk_soliditet_skuld [FLAGGED]");
            state.kreditPoang -= 18;
        }
    }
    private void checkLowLiquidityNegativeOperational(double likviditetsgrad,double rorelseresultat, ScoringState state){
        if (likviditetsgrad < thresholds.liquidity().minimum() && rorelseresultat < 0) {
            state.hardReject = true;
            state.decisionReason.append("AVSLAG: Kombinationsrisk — likviditetsgrad under minimum samt negativt rörelseresultat. ");
            state.scoringLog.append(", kombinationsrisk_likviditet_resultat [REJECT]");
            state.kreditPoang -= 40;
        }
    }
    private void checkCreditAboveTurnover(BigDecimal requestedAmount,double nettoomsattning,ScoringState state){
        if (requestedAmount.doubleValue() > nettoomsattning) {
            state.flagCount++;
            state.decisionReason.append("VARNING: Kreditbelopp överstiger årsoms. (")
                    .append(String.format("%.0f", requestedAmount.doubleValue()))
                    .append(" kr > ").append(String.format("%.0f", nettoomsattning)).append(" kr). ");
            state.scoringLog.append(", kredit_vs_omsattning [FLAGGED]");
            state.kreditPoang -= 8;
        }
    }
    private void checkLowCapitalCreditRatio(BigDecimal requestedAmount,double egetKapital, ScoringState state){
        if (requestedAmount.doubleValue() > 0 && egetKapital / requestedAmount.doubleValue() < thresholds.equityCoverage().credit_quotient()) {
            // magic number 0.3 — eget kapital borde vara minst 30% av kreditbelopp
            state.flagCount++;
            state.decisionReason.append("VARNING: Eget kapital täcker mindre än ").append(String.format("%,2f",thresholds.equityCoverage().credit_quotient())).append(" av kreditbeloppet. ");
            state.scoringLog.append(", eget_kapital_vs_kredit [FLAGGED]");
            state.kreditPoang -= 10;
        }
    }
    private void checkDebtAcknowledgementError(double totalaSkulder,double kortfristigaSkulder,double nettoomsattning,ScoringState state){
        // OBS: detta är fel, borde vara totalaSkulder / nettoomsattning men det funkar i de flesta fall
        double skuldTackningsFel = (totalaSkulder + kortfristigaSkulder) / (nettoomsattning + 1); // +1 för att undvika division med noll
        if (skuldTackningsFel > thresholds.debtAckError().high()) { // magic number 2.0 — inkonsekvent med skuldsättningsgrad-check ovan
            state.flagCount++;
            state.decisionReason.append("VARNING: Skuldbörda hög relativt omsättning (kombinationscheck). ");
            state.scoringLog.append(", skuld_omsattning_kombination [FLAGGED]");
            state.kreditPoang -= 7;
        }
    }
    private void checkCashFlowToDebtRatio(double kassaflodeKvot, double skuldsattningsgrad,ScoringState state){
        if (kassaflodeKvot < thresholds.cashflowRatio().very_low() && skuldsattningsgrad > thresholds.debtRatio().high()) {
            // inkonsekvent — 0.05 här men 0.08 användes ovan
            state.flagCount++;
            state.decisionReason.append("VARNING: Kombinationsrisk kassaflöde + skuldsättning. ");
            state.scoringLog.append(", kassaflode_skuld_kombination [FLAGGED]");
            state.kreditPoang -= 12;
        }
    }


    private void interestCoverageCheck(double ranteTackningsgrad,ScoringState state){

        state.scoringLog.append(", ränteTäckning=").append(String.format("%.2f", ranteTackningsgrad));

        if (ranteTackningsgrad < thresholds.interestCoverage().reject()) {
            // Hard reject — magic number 1.5
            state.hardReject = true;
            state.decisionReason.append("AVSLAG: Räntetäckningsgrad under ").append(thresholds.interestCoverage().reject()).append(" (")
                    .append(String.format("%.2f", ranteTackningsgrad)).append("). Rörelseresultat täcker ej räntekostnader. ");
            state.scoringLog.append(" [REJECT]");
            state.kreditPoang -= 35;
        } else if (ranteTackningsgrad < thresholds.interestCoverage().low()) {
            // Flag — magic number 2.5, inkonsekvent med hardReject-gränsen 1.5
            state.flagCount++;
            state.decisionReason.append("VARNING: Räntetäckningsgrad låg (").append(String.format("%.2f", ranteTackningsgrad)).append(" < ")
                    .append(thresholds.interestCoverage().reject()).append(", rekommenderas minst ")
                    .append(thresholds.interestCoverage().reject()).append("). ");
            state.scoringLog.append(" [FLAGGED]");
            state.kreditPoang -= 15;
        } else if (ranteTackningsgrad >= 999) {
            // Ingen räntekostnad — poäng-neutral, loggas bara
            state.decisionReason.append("Räntetäckningsgrad ej tillämplig (inga räntekostnader). ");
            state.scoringLog.append(" [N/A]");
        } else {
            state.decisionReason.append("Räntetäckningsgrad OK (").append(String.format("%.2f", ranteTackningsgrad)).append("). ");
            state.scoringLog.append(" [OK]");
            state.kreditPoang += 8;
        }
    }

    private void cashflowCheck(double kassaflodeKvot,double nettoomsattning,double investeringsKassaflode,ScoringState state){
        state.scoringLog.append(", kassaflödeskvot=").append(String.format("%.3f", kassaflodeKvot));

        if (kassaflodeKvot < 0) {
            // Negativt operativt kassaflöde — hård avvisning
            state.hardReject = true;
            state.decisionReason.append("AVSLAG: Negativt operativt kassaflöde (kassaflödeskvot=")
                    .append(String.format("%.3f", kassaflodeKvot)).append("). ");
            state.scoringLog.append(" [REJECT]");
            state.kreditPoang -= 30;
        } else if (kassaflodeKvot < thresholds.cashflowRatio().very_low()) {
            // magic number 0.05 — men 0.08 används i check nedanför
            state.flagCount++;
            state.decisionReason.append("VARNING: Kassaflödeskvot låg (").append(String.format("%.3f", kassaflodeKvot)).append(" < ").append(thresholds.cashflowRatio().very_low()).append("). ");
            state.scoringLog.append(" [FLAGGED]");
            state.kreditPoang -= 12;
        } else if (kassaflodeKvot < thresholds.cashflowRatio().low()) {
            // inkonsekvent med 0.05 ovan — borde vara samma gräns // Should it really? /Jonathan
            state.flagCount++;
            state.decisionReason.append("VARNING: Kassaflödeskvot under rekommenderad nivå (")
                    .append(String.format("%.3f", kassaflodeKvot)).append(" < ").append(thresholds.cashflowRatio().low()).append("). ");
            state.scoringLog.append(" [FLAGGED]");
            state.kreditPoang -= 6;
        } else {
            state.decisionReason.append("Kassaflödeskvot OK (").append(String.format("%.3f", kassaflodeKvot)).append("). ");
            state.scoringLog.append(" [OK]");
            state.kreditPoang += 5;
        }

        // Investeringskassaflöde — negativt är ofta normalt men flaggas ändå
        if (investeringsKassaflode < -nettoomsattning * thresholds.investmentCashFlow().inverse_cashflow_flag()) { // magic number 0.3
            state.flagCount++;
            state.decisionReason.append("VARNING: Högt negativt investeringskassaflöde (")
                    .append(String.format("%.0f", investeringsKassaflode)).append(" kr). ");
            state.scoringLog.append(", inv_kassaflode [FLAGGED]");
            state.kreditPoang -= 4;
        }
    }

    private void branchAdjustedChecks(String bransch,double rorelsemarginal,double soliditet,ScoringState state){
        Optional<Branch> branchOptional = branchRepository.findByBranchName(bransch);

        double branschFaktor = 1.0; //default fallback
        double snittSoliditet;
        double snittMarginal;
        if (branchOptional.isPresent()){
            branschFaktor = branchOptional.get().branchFactor;
            snittSoliditet = branchOptional.get().branschSnittsSoliditet;
            snittMarginal = branchOptional.get().branschSnittMarginal;

            if (rorelsemarginal < snittMarginal * thresholds.operatingMargin().flag_below_branch_avg()) { // magic number 0.5 — inkonsekvent med 0.75 ovan
                state.flagCount++;
                state.decisionReason.append("VARNING: Rörelsemarginal under ").append(String.format("%.2f",thresholds.operatingMargin().flag_below_branch_avg())).append("% av branschsnitt för ")
                        .append(bransch).append(". ");
                state.scoringLog.append(", under_branschsnitt_marginal [FLAGGED]");
                state.kreditPoang -= 5;
            }

            if (soliditet < snittSoliditet * thresholds.solidity().flag_below_branch_avg()) { // magic number 0.75 — "75% av branschsnitt"
                state.flagCount++;
                state.decisionReason.append("VARNING: Soliditet betydligt under branschsnitt för ")
                        .append(bransch).append(" (snitt=").append(String.format("%.2f", snittSoliditet))
                        .append("). ");
                state.scoringLog.append(", under_branschsnitt_soliditet [FLAGGED]");
                state.kreditPoang -= 6;
            }


        } else{
            state.scoringLog.append(", bransch=").append(bransch.isEmpty() ? "OKÄND" : bransch)
                    .append("(faktor=").append(String.format("%.2f", branschFaktor)).append(")");
        }


        // Branschjusterad soliditetskontroll — BARA detta check använder branschFaktor
        // Inkonsekvent: soliditet-check ovan använder fast 0.20/0.25, inte branschjusterad
        double branschJusteradSoliditetGrans = thresholds.solidity().minimum() * branschFaktor; // inkonsekvent med 0.25 ovan (Fixed by threshold)
        if (soliditet < branschJusteradSoliditetGrans) {
            state.flagCount++;
            state.decisionReason.append("VARNING: Soliditet understiger branschjusterad gräns (")
                    .append(String.format("%.2f", branschJusteradSoliditetGrans))
                    .append(" för bransch ").append(bransch).append("). ");
            state.scoringLog.append(", bransch_soliditet [FLAGGED]");
            state.kreditPoang -= 8;
        }
    }

    private void checkOperatingMargin(double rorelsemarginal,ScoringState state){
        state.scoringLog.append("rörelsemarginal=").append(String.format("%.2f", rorelsemarginal));

        if (rorelsemarginal < thresholds.operatingMargin().low()) {
            // Flag — magic number 0.02 (2%)
            state.flagCount++;
            state.decisionReason.append("VARNING: Rörelseresultatmarginal låg (")
                    .append(String.format("%.2f", rorelsemarginal * 100)).append("%, rekommenderas över ").append(thresholds.operatingMargin().low() * 100).append("%). ");
            state.scoringLog.append(" [FLAGGED]");
            state.kreditPoang -= 10;
        } else if (rorelsemarginal >= thresholds.operatingMargin().good()) {
            state.decisionReason.append("Rörelseresultatmarginal god (")
                    .append(String.format("%.2f", rorelsemarginal * 100)).append("%). ");
            state.scoringLog.append(" [GOOD]");
            state.kreditPoang += 8;
        } else {
            state.decisionReason.append("Rörelseresultatmarginal godkänd (")
                    .append(String.format("%.2f", rorelsemarginal * 100)).append("%). ");
            state.scoringLog.append(" [OK]");
        }
    }

    private void checkDebtRatio(double skuldsattningsgrad,ScoringState state){
        state.scoringLog.append("skuldsättningsgrad=").append(String.format("%.2f", skuldsattningsgrad));

        if (skuldsattningsgrad > thresholds.debtRatio().max()) {
            // Hard reject — magic number 3.0
            state.hardReject = true;
            state.decisionReason.append("AVSLAG: Skuldsättningsgrad för hög (").append(String.format("%.2f", skuldsattningsgrad)).append(" > ").append(thresholds.debtRatio().max()).append("). ");
            state.scoringLog.append(" [REJECT]");
            state.kreditPoang -= 35;
        } else if (skuldsattningsgrad > thresholds.debtRatio().high()) {
            // Flag — different magic number than reject threshold
            state.flagCount++;
            state.decisionReason.append("VARNING: Skuldsättningsgrad hög (").append(String.format("%.2f", skuldsattningsgrad)).append(", rekommenderas under ").append(thresholds.debtRatio().high()).append("). ");
            state.scoringLog.append(" [FLAGGED]");
            state.kreditPoang -= 15;
        } else {
            state.decisionReason.append("Skuldsättningsgrad OK (").append(String.format("%.2f", skuldsattningsgrad)).append("). ");
            state.scoringLog.append(" [OK]");
            state.kreditPoang += 5;
        }

        state.scoringLog.append(", ");
    }

    private void liquidityCheck(double likviditetsgrad, ScoringState state){
        state.scoringLog.append("likviditetsgrad=").append(String.format("%.2f", likviditetsgrad));

        if (likviditetsgrad < thresholds.liquidity().minimum()) {
            state.flagCount++;
            state.decisionReason.append("VARNING: Likviditetsgrad under 1.0 (").append(String.format("%.2f", likviditetsgrad))
                    .append("). Kortfristiga skulder överstiger omsättningstillgångar. ");
            state.scoringLog.append(" [FLAGGED]");
            state.kreditPoang -= 15;
        } else if (likviditetsgrad >= thresholds.liquidity().good()) {
            state.decisionReason.append("Likviditetsgrad god (").append(String.format("%.2f", likviditetsgrad)).append("). ");
            state.scoringLog.append(" [GOOD]");
            state.kreditPoang += 10;
        } else {
            state.decisionReason.append("Likviditetsgrad godkänd (").append(String.format("%.2f", likviditetsgrad)).append("). ");
            state.scoringLog.append(" [OK]");
        }

        state.scoringLog.append(", ");

        // Extra likviditets-check med 1.2-tröskel (ännu ett magic number)
        if (likviditetsgrad < thresholds.liquidity().low() && likviditetsgrad >= thresholds.liquidity().minimum()) {
            state.flagCount++;
            state.decisionReason.append("VARNING: Likviditetsgrad nära minimigräns (")
                    .append(String.format("%.2f", likviditetsgrad)).append(" < ").append(thresholds.liquidity().low()).append("). ");
            state.scoringLog.append(", likviditet_marginal [FLAGGED]");
            state.kreditPoang -= 8;
        }
    }

    private void solidityCheck(double soliditet,BigDecimal requestedAmount, ScoringState state){
        state.scoringLog.append("soliditet=").append(String.format("%.2f", soliditet));
        if (soliditet < thresholds.solidity().minimum()) {
            // Hard reject threshold — magic number
            state.hardReject = true;
            state.decisionReason.append("AVSLAG: Soliditet för låg (").append(String.format("%.2f", soliditet))
                    .append(" < 0.20 gräns). ");
            state.scoringLog.append(" [REJECT]");
            state.kreditPoang -= 40;
        } else if (soliditet < thresholds.solidity().low()) {
            // Flag threshold — different magic number from above
            state.flagCount++;
            state.decisionReason.append("VARNING: Soliditet låg (").append(String.format("%.2f", soliditet))
                    .append(", rekommenderad miniminivå 0.25). ");
            state.scoringLog.append(" [FLAGGED]");
            state.kreditPoang -= 20;
        } else {
            state.decisionReason.append("Soliditet OK (").append(String.format("%.2f", soliditet)).append("). ");
            state.scoringLog.append(" [OK]");
            state.kreditPoang += 5;
        }
        state.scoringLog.append(", ");

        // Extra soliditet-kontroll med ANNAN tröskel (0.30) — inkonsekvent med ovan
        // TODO: bestäm en tröskel och håll dig till den
        // is this supposed to be separate to the above if-else block?
        // I will keep this as such but its clear its supposed to use the configured threshold for low values.
        if (soliditet < thresholds.solidity().low() && requestedAmount.compareTo(BigDecimal.valueOf(thresholds.credit().high_credit_solidity())) > 0) {
            state.flagCount++;
            state.decisionReason.append("VARNING: Stor kreditbelopp med soliditet under ").append(thresholds.solidity().low()).append(" – extra granskning rekommenderas. ");
            state.scoringLog.append(", storkredit_soliditet [FLAGGED]");
            state.kreditPoang -= 12;
        }


    }


    //Private class containing the mutable scoring state as it travels through the scoring engine.
    private class ScoringState {

        public int kreditPoang = thresholds.initialScore();
        public int flagCount = 0;
        public boolean hardReject = false;

        public StringBuilder decisionReason = new StringBuilder();
        public StringBuilder scoringLog = new StringBuilder();
    }


}
