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

    private final BranchRepository branchRepository;
    private final ScoringThresholds thresholds;


    public ScoringService(CompanyRepository companyRepository, BranchRepository branchRepository, ScoringThresholds threshold) {
        this.branchRepository = branchRepository;
        this.thresholds = threshold;
    }

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
        ScoringState state = new ScoringState(thresholds.initialScore());





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
                state.incrementFlags(1);
                state.getDecisionReason().append("VARNING: Kreditbelopp överstiger ").append(thresholds.credit().extreme_credit()).append(" kr — kräver manuell granskning. ");
                state.getScoringLog().append(", storkredit [FLAGGED]");
                state.removePoints(10);
            }

            // Negativt eget kapital — ej täckt av soliditet-formeln om totalt_kapital också är negativt
            if (egetKapital < 0) {
                state.setHardReject(true);
                state.getDecisionReason().append("AVSLAG: Negativt eget kapital. ");
                state.getScoringLog().append(", negativt_eget_kapital [REJECT]");
                state.removePoints(50);
            }

            // Nettoomsättning-kontroll — liten verksamhet flaggas
            if (nettoomsattning < thresholds.turnover().low()) {
                state.incrementFlags(1);
                state.getDecisionReason().append("VARNING: Låg nettoomsättning (under ").append(thresholds.turnover().low()).append(" kr). ");
                state.getScoringLog().append(", låg_omsättning [FLAGGED]");
                state.removePoints(7);
            }

            // Rörelseresultat negativt — extra flagg utöver marginalen
            if (rorelseresultat < 0) {
                state.incrementFlags(1);
                state.getDecisionReason().append("VARNING: Negativt rörelseresultat. ");
                state.getScoringLog().append(", negativt_rörelseresultat [FLAGGED]");
                state.removePoints(12);
            }

            // Totala skulder > nettoomsättning — inget eget threshold, bara ett av många checks
            if (totalaSkulder > nettoomsattning * 2) {
                state.incrementFlags(1);
                state.getDecisionReason().append("VARNING: Totala skulder överstiger dubbla nettoomsättningen. ");
                state.getScoringLog().append(", skulder_vs_omsattning [FLAGGED]");
                state.removePoints(10);
            }

            // Kortfristiga skulder > omsättningstillgångar (redundant med likviditetsgrad-check ovan)
            if (kortfristigaSkulder > omsattningstillgangar) {
                // Already counted in likviditetsgrad, but re-checked here — duplicate logic
                state.getDecisionReason().append("Not: Kortfristiga skulder överstiger omsättningstillgångar. ");
            }

            // ===========================================================
            // BRANSCHKORREKTIONSFAKTOR
            // Mappar branschkod till justerings-multiplikator för soliditetsgräns
            // Används BARA för ett av soliditet-checkarna nedan — inkonsekvent med övriga
            // TODO: applicera branschfaktor konsekvent på alla nyckeltal
            // ===========================================================

            branchAdjustedChecks( bransch, rorelsemarginal, soliditet, state);
            checkIndustryAdjustedMinimumSolidity(soliditet,state,bransch);

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
            state.getScoringLog().append(", kreditPoäng=").append(state.getPoints());

            // ===========================================================
            // BESLUT — combine flags and hard rejects
            // ===========================================================
            Decision decision = finalDecision(state);



        return new NewApplicationDTO(requestedAmount, purpose, decision.finalStatus, decision.finalDecision, state.getDecisionReason().toString(), state.getScoringLog().toString(), companyName, orgNumber, authorizedSignatory, state.getFlagCount());

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
    Decision finalDecision(ScoringState state){
        String finalDecision;
        ApplicationStatus finalStatus;
        if (state.isHardReject() || state.getPoints() < thresholds.finalScoreThresholds().reject()) {
            finalDecision = "REJECTED";
            finalStatus = ApplicationStatus.REJECTED;
            state.getDecisionReason().insert(0, "=== ANSÖKAN AVSLAGEN === ");
        } else if (state.getPoints() < thresholds.finalScoreThresholds().manual_review()) {
            finalDecision = "REVIEW";
            finalStatus = ApplicationStatus.UNDER_REVIEW;
            state.getDecisionReason().insert(0, "=== MANUELL GRANSKNING === Antal varningsflaggor: " + state.getFlagCount() + ". ");
        } else if (state.getPoints() < thresholds.finalScoreThresholds().flag_review()) {
            finalDecision = "REVIEW";
            finalStatus = ApplicationStatus.UNDER_REVIEW;
            state.getDecisionReason().insert(0, "=== GRANSKNING REKOMMENDERAS === 1 varningsflagga. ");
        } else {
            finalDecision = "APPROVED";
            finalStatus = ApplicationStatus.APPROVED;
            state.getDecisionReason().insert(0, "=== ANSÖKAN GODKÄND === Alla nyckeltal uppfyller krav. ");
        }
        return new Decision(finalDecision,finalStatus);
    }

    record Decision(
            String finalDecision,
            ApplicationStatus finalStatus
    ){}


    //Combinatory checks
    void checkLowSolidityHighDebtRatio(double soliditet, double skuldsattningsgrad, ScoringState state){
        if (soliditet < thresholds.solidity().low() && skuldsattningsgrad > thresholds.debtRatio().high()) {
            // dubbel riskindikator — magic numbers inkonsekvent med individuella checks ovan
            state.incrementFlags(1);
            state.getDecisionReason().append("VARNING: Dubbel riskindikator — låg soliditet (")
                    .append(String.format("%.2f", soliditet)).append(") kombinerat med hög skuldsättning (")
                    .append(String.format("%.2f", skuldsattningsgrad)).append("). ");
            state.getScoringLog().append(", kombinationsrisk_soliditet_skuld [FLAGGED]");
            state.removePoints(18);
        }
    }
    void checkLowLiquidityNegativeOperational(double likviditetsgrad, double rorelseresultat, ScoringState state){
        if (likviditetsgrad < thresholds.liquidity().minimum() && rorelseresultat < 0) {
            state.setHardReject(true);
            state.getDecisionReason().append("AVSLAG: Kombinationsrisk — likviditetsgrad under minimum samt negativt rörelseresultat. ");
            state.getScoringLog().append(", kombinationsrisk_likviditet_resultat [REJECT]");
            state.removePoints(40);
        }
    }
    void checkCreditAboveTurnover(BigDecimal requestedAmount, double nettoomsattning, ScoringState state){
        if (requestedAmount.doubleValue() > nettoomsattning) {
            state.incrementFlags(1);
            state.getDecisionReason().append("VARNING: Kreditbelopp överstiger årsoms. (")
                    .append(String.format("%.0f", requestedAmount.doubleValue()))
                    .append(" kr > ").append(String.format("%.0f", nettoomsattning)).append(" kr). ");
            state.getScoringLog().append(", kredit_vs_omsattning [FLAGGED]");
            state.removePoints(8);
        }
    }
    void checkLowCapitalCreditRatio(BigDecimal requestedAmount, double egetKapital, ScoringState state){
        if (requestedAmount.doubleValue() > 0 && egetKapital / requestedAmount.doubleValue() < thresholds.equityCoverage().credit_quotient()) {
            // magic number 0.3 — eget kapital borde vara minst 30% av kreditbelopp
            state.incrementFlags(1);
            state.getDecisionReason().append("VARNING: Eget kapital täcker mindre än ").append(String.format("%,2f",thresholds.equityCoverage().credit_quotient())).append(" av kreditbeloppet. ");
            state.getScoringLog().append(", eget_kapital_vs_kredit [FLAGGED]");
            state.removePoints(10);
        }
    }
    void checkDebtAcknowledgementError(double totalaSkulder, double kortfristigaSkulder, double nettoomsattning, ScoringState state){
        // OBS: detta är fel, borde vara totalaSkulder / nettoomsattning men det funkar i de flesta fall
        double skuldTackningsFel = (totalaSkulder + kortfristigaSkulder) / (nettoomsattning + 1); // +1 för att undvika division med noll
        if (skuldTackningsFel > thresholds.debtAckError().high()) { // magic number 2.0 — inkonsekvent med skuldsättningsgrad-check ovan
            state.incrementFlags(1);
            state.getDecisionReason().append("VARNING: Skuldbörda hög relativt omsättning (kombinationscheck). ");
            state.getScoringLog().append(", skuld_omsattning_kombination [FLAGGED]");
            state.removePoints(7);
        }
    }
    void checkCashFlowToDebtRatio(double kassaflodeKvot, double skuldsattningsgrad, ScoringState state){
        if (kassaflodeKvot < thresholds.cashflowRatio().very_low() && skuldsattningsgrad > thresholds.debtRatio().high()) {
            // inkonsekvent — 0.05 här men 0.08 användes ovan
            state.incrementFlags(1);
            state.getDecisionReason().append("VARNING: Kombinationsrisk kassaflöde + skuldsättning. ");
            state.getScoringLog().append(", kassaflode_skuld_kombination [FLAGGED]");
            state.removePoints(12);
        }
    }


    void interestCoverageCheck(double ranteTackningsgrad, ScoringState state){

        state.getScoringLog().append(", ränteTäckning=").append(String.format("%.2f", ranteTackningsgrad));

        if (ranteTackningsgrad < thresholds.interestCoverage().reject()) {
            // Hard reject — magic number 1.5
            state.setHardReject(true);
            state.getDecisionReason().append("AVSLAG: Räntetäckningsgrad under ").append(thresholds.interestCoverage().reject()).append(" (")
                    .append(String.format("%.2f", ranteTackningsgrad)).append("). Rörelseresultat täcker ej räntekostnader. ");
            state.getScoringLog().append(" [REJECT]");
            state.removePoints(35);
        } else if (ranteTackningsgrad < thresholds.interestCoverage().low()) {
            // Flag — magic number 2.5, inkonsekvent med hardReject-gränsen 1.5
            state.incrementFlags(1);
            state.getDecisionReason().append("VARNING: Räntetäckningsgrad låg (").append(String.format("%.2f", ranteTackningsgrad)).append(" < ")
                    .append(thresholds.interestCoverage().low()).append(", rekommenderas minst ")
                    .append(thresholds.interestCoverage().low()).append("). ");
            state.getScoringLog().append(" [FLAGGED]");
            state.removePoints(15);
        } else if (ranteTackningsgrad >= 999) {
            // Ingen räntekostnad — poäng-neutral, loggas bara
            state.getDecisionReason().append("Räntetäckningsgrad ej tillämplig (inga räntekostnader). ");
            state.getScoringLog().append(" [N/A]");
        } else {
            state.getDecisionReason().append("Räntetäckningsgrad OK (").append(String.format("%.2f", ranteTackningsgrad)).append("). ");
            state.getScoringLog().append(" [OK]");
            state.addPoints(8);
        }
    }

    void cashflowCheck(double kassaflodeKvot, double nettoomsattning, double investeringsKassaflode, ScoringState state){
        state.getScoringLog().append(", kassaflödeskvot=").append(String.format("%.3f", kassaflodeKvot));

        if (kassaflodeKvot < 0) {
            // Negativt operativt kassaflöde — hård avvisning
            state.setHardReject(true);
            state.getDecisionReason().append("AVSLAG: Negativt operativt kassaflöde (kassaflödeskvot=")
                    .append(String.format("%.3f", kassaflodeKvot)).append("). ");
            state.getScoringLog().append(" [REJECT]");
            state.removePoints(30);
        } else if (kassaflodeKvot < thresholds.cashflowRatio().very_low()) {
            // magic number 0.05 — men 0.08 används i check nedanför
            state.incrementFlags(1);
            state.getDecisionReason().append("VARNING: Kassaflödeskvot låg (").append(String.format("%.3f", kassaflodeKvot)).append(" < ").append(thresholds.cashflowRatio().very_low()).append("). ");
            state.getScoringLog().append(" [FLAGGED]");
            state.removePoints(12);
        } else if (kassaflodeKvot < thresholds.cashflowRatio().low()) {
            // inkonsekvent med 0.05 ovan — borde vara samma gräns // Should it really? /Jonathan
            state.incrementFlags(1);
            state.getDecisionReason().append("VARNING: Kassaflödeskvot under rekommenderad nivå (")
                    .append(String.format("%.3f", kassaflodeKvot)).append(" < ").append(thresholds.cashflowRatio().low()).append("). ");
            state.getScoringLog().append(" [FLAGGED]");
            state.removePoints(6);
        } else {
            state.getDecisionReason().append("Kassaflödeskvot OK (").append(String.format("%.3f", kassaflodeKvot)).append("). ");
            state.getScoringLog().append(" [OK]");
            state.addPoints( 5);
        }

        // Investeringskassaflöde — negativt är ofta normalt men flaggas ändå
        if (investeringsKassaflode < -nettoomsattning * thresholds.investmentCashFlow().inverse_cashflow_flag()) { // magic number 0.3
            state.incrementFlags(1);
            state.getDecisionReason().append("VARNING: Högt negativt investeringskassaflöde (")
                    .append(String.format("%.0f", investeringsKassaflode)).append(" kr). ");
            state.getScoringLog().append(", inv_kassaflode [FLAGGED]");
            state.removePoints(4);
        }
    }

    void checkIndustryAdjustedMinimumSolidity(double solidity, ScoringState state,String branchName) {
        Optional<Branch> branchOptional = branchRepository.findByBranchName(branchName);

        double branschFactor = branchOptional.map(branch -> branch.branchFactor).orElse(1.0);

        double threshold =
                thresholds.solidity().minimum() * branschFactor;

        if (solidity < threshold) {
            state.incrementFlags(1);
            state.getDecisionReason()
                    .append("VARNING: Soliditet understiger branschjusterad gräns (")
                    .append(String.format("%.2f", threshold))
                    .append("). ");
            state.getScoringLog()
                    .append(", bransch_soliditet [FLAGGED]");
            state.removePoints(8);
        }
    }

    void branchAdjustedChecks(String bransch, double rorelsemarginal, double soliditet, ScoringState state){
        Optional<Branch> branchOptional = branchRepository.findByBranchName(bransch);

        double branschFaktor = 1.0;//default fallback
        double snittSoliditet;
        double snittMarginal;
        if (branchOptional.isPresent()){
            branschFaktor = branchOptional.get().branchFactor;
            snittSoliditet = branchOptional.get().branschSnittsSoliditet;
            snittMarginal = branchOptional.get().branschSnittMarginal;

            if (rorelsemarginal < snittMarginal * thresholds.operatingMargin().flag_below_branch_avg()) { // magic number 0.5 — inkonsekvent med 0.75 ovan
                state.incrementFlags(1);
                state.getDecisionReason().append("VARNING: Rörelsemarginal under ").append(String.format("%.2f",thresholds.operatingMargin().flag_below_branch_avg())).append("% av branschsnitt för ")
                        .append(bransch).append(". ");
                state.getScoringLog().append(", under_branschsnitt_marginal [FLAGGED]");
                state.removePoints(5);
            }

            if (soliditet < snittSoliditet * thresholds.solidity().flag_below_branch_avg()) { // magic number 0.75 — "75% av branschsnitt"
                state.incrementFlags(1);
                state.getDecisionReason().append("VARNING: Soliditet betydligt under branschsnitt för ")
                        .append(bransch).append(" (snitt=").append(String.format("%.2f", snittSoliditet))
                        .append("). ");
                state.getScoringLog().append(", under_branschsnitt_soliditet [FLAGGED]");
                state.removePoints(6);
            }


        } else{
            state.getScoringLog().append(", bransch=").append(bransch.isEmpty() ? "OKÄND" : bransch)
                    .append("(faktor=").append(String.format("%.2f", branschFaktor)).append(")");
        }
    }

    void checkOperatingMargin(double rorelsemarginal, ScoringState state){
        state.getScoringLog().append("rörelsemarginal=").append(String.format("%.2f", rorelsemarginal));

        if (rorelsemarginal < thresholds.operatingMargin().low()) {
            // Flag — magic number 0.02 (2%)
            state.incrementFlags(1);
            state.getDecisionReason().append("VARNING: Rörelseresultatmarginal låg (")
                    .append(String.format("%.2f", rorelsemarginal * 100)).append("%, rekommenderas över ").append(thresholds.operatingMargin().low() * 100).append("%). ");
            state.getScoringLog().append(" [FLAGGED]");
            state.removePoints(10);
        } else if (rorelsemarginal >= thresholds.operatingMargin().good()) {
            state.getDecisionReason().append("Rörelseresultatmarginal god (")
                    .append(String.format("%.2f", rorelsemarginal * 100)).append("%). ");
            state.getScoringLog().append(" [GOOD]");
            state.addPoints(8);
        } else {
            state.getDecisionReason().append("Rörelseresultatmarginal godkänd (")
                    .append(String.format("%.2f", rorelsemarginal * 100)).append("%). ");
            state.getScoringLog().append(" [OK]");
        }
    }

    void checkDebtRatio(double skuldsattningsgrad, ScoringState state){
        state.getScoringLog().append("skuldsättningsgrad=").append(String.format("%.2f", skuldsattningsgrad));

        if (skuldsattningsgrad > thresholds.debtRatio().maximum()) {
            // Hard reject — magic number 3.0
            state.setHardReject(true);
            state.getDecisionReason().append("AVSLAG: Skuldsättningsgrad för hög (").append(String.format("%.2f", skuldsattningsgrad)).append(" > ").append(thresholds.debtRatio().maximum()).append("). ");
            state.getScoringLog().append(" [REJECT]");
            state.removePoints(35);
        } else if (skuldsattningsgrad > thresholds.debtRatio().high()) {
            // Flag — different magic number than reject threshold
            state.incrementFlags(1);
            state.getDecisionReason().append("VARNING: Skuldsättningsgrad hög (").append(String.format("%.2f", skuldsattningsgrad)).append(", rekommenderas under ").append(thresholds.debtRatio().high()).append("). ");
            state.getScoringLog().append(" [FLAGGED]");
            state.removePoints(15);
        } else {
            state.getDecisionReason().append("Skuldsättningsgrad OK (").append(String.format("%.2f", skuldsattningsgrad)).append("). ");
            state.getScoringLog().append(" [OK]");
            state.addPoints(5);
        }

        state.getScoringLog().append(", ");
    }

    void liquidityCheck(double likviditetsgrad, ScoringState state){
        state.getScoringLog().append("likviditetsgrad=").append(String.format("%.2f", likviditetsgrad));

        if (likviditetsgrad < thresholds.liquidity().minimum()) {
            state.incrementFlags(1);
            state.getDecisionReason().append("VARNING: Likviditetsgrad under 1.0 (").append(String.format("%.2f", likviditetsgrad))
                    .append("). Kortfristiga skulder överstiger omsättningstillgångar. ");
            state.getScoringLog().append(" [FLAGGED]");
            state.removePoints(15);
        } else if (likviditetsgrad < thresholds.liquidity().low()) {
            state.incrementFlags(1);
            state.getDecisionReason().append("VARNING: Likviditetsgrad nära minimigräns (")
                    .append(String.format("%.2f", likviditetsgrad)).append(" < ").append(thresholds.liquidity().low()).append("). ");
            state.getScoringLog().append(", likviditet_marginal [FLAGGED]");
            state.removePoints(8);
        } else if (likviditetsgrad >= thresholds.liquidity().good()) {
            state.getDecisionReason().append("Likviditetsgrad god (").append(String.format("%.2f", likviditetsgrad)).append("). ");
            state.getScoringLog().append(" [GOOD]");
            state.addPoints(10);
        } else {
            state.getDecisionReason().append("Likviditetsgrad godkänd (").append(String.format("%.2f", likviditetsgrad)).append("). ");
            state.getScoringLog().append(" [OK]");
        }

        state.getScoringLog().append(", ");

    }

    void solidityCheck(double soliditet, BigDecimal requestedAmount, ScoringState state){
        state.getScoringLog().append("soliditet=").append(String.format("%.2f", soliditet));
        if (soliditet < thresholds.solidity().minimum()) {
            // Hard reject threshold — magic number
            state.setHardReject(true);
            state.getDecisionReason().append("AVSLAG: Soliditet för låg (").append(String.format("%.2f", soliditet))
                    .append(" < 0.20 gräns). ");
            state.getScoringLog().append(" [REJECT]");
            state.removePoints(40);
        } else if (soliditet < thresholds.solidity().low()) {
            // Flag threshold — different magic number from above
            state.incrementFlags(1);
            state.getDecisionReason().append("VARNING: Soliditet låg (").append(String.format("%.2f", soliditet))
                    .append(", rekommenderad miniminivå 0.25). ");
            state.getScoringLog().append(" [FLAGGED]");
            state.removePoints(20);
        } else {
            state.getDecisionReason().append("Soliditet OK (").append(String.format("%.2f", soliditet)).append("). ");
            state.getScoringLog().append(" [OK]");
            state.addPoints(5);
        }
        state.getScoringLog().append(", ");

        // Extra soliditet-kontroll med ANNAN tröskel (0.30) — inkonsekvent med ovan
        // TODO: bestäm en tröskel och håll dig till den
        // is this supposed to be separate to the above if-else block?
        // I will keep this as such but its clear its supposed to use the configured threshold for low values.
        if (soliditet < thresholds.solidity().low() && requestedAmount.compareTo(BigDecimal.valueOf(thresholds.credit().high_credit_solidity())) > 0) {
            state.incrementFlags(1);
            state.getDecisionReason().append("VARNING: Stor kreditbelopp med soliditet under ").append(thresholds.solidity().low()).append(" – extra granskning rekommenderas. ");
            state.getScoringLog().append(", storkredit_soliditet [FLAGGED]");
            state.removePoints(12);
        }


    }


}
