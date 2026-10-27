import { describe, expect, it } from "vitest";
import { getCustomerRejectionsSummary, getMetric, parseDecisionReasons } from "./scoringConverter";

// Real scoring output string representing a rejected application scenario
const SCORING_RESULT =
  "soliditet=0.08 [REJECT], , storkredit_soliditet [FLAGGED]likviditetsgrad=0.67 [FLAGGED], skuldsättningsgrad=11.50 [REJECT], rörelsemarginal=-0.08 [FLAGGED], negativt_rörelseresultat [FLAGGED], bransch=bransch(faktor=1.00), bransch_soliditet [FLAGGED], kassaflödeskvot=-0.043 [REJECT], ränteTäckning=-1.67 [REJECT], kombinationsrisk_soliditet_skuld [FLAGGED], kombinationsrisk_likviditet_resultat [REJECT], eget_kapital_vs_kredit [FLAGGED], kassaflode_skuld_kombination [FLAGGED], kreditPoäng=-177"

const DECISION_REASON =
  "=== ANSÖKAN AVSLAGEN === AVSLAG: Soliditet för låg (0.08 < 0.20 gräns). VARNING: Stor kreditbelopp med soliditet under 0.25 – extra granskning rekommenderas. VARNING: Likviditetsgrad under 1.0 (0.67). Kortfristiga skulder överstiger omsättningstillgångar. AVSLAG: Skuldsättningsgrad för hög (11.50 > 3.0). VARNING: Rörelseresultatmarginal låg (-7.50%, rekommenderas över 2.0%). VARNING: Negativt rörelseresultat. Not: Kortfristiga skulder överstiger omsättningstillgångar. VARNING: Soliditet understiger branschjusterad gräns (0.20). AVSLAG: Negativt operativt kassaflöde (kassaflödeskvot=-0.043). AVSLAG: Räntetäckningsgrad under 1.5 (-1.67). Rörelseresultat täcker ej räntekostnader. VARNING: Dubbel riskindikator — låg soliditet (0.08) kombinerat med hög skuldsättning (11.50). AVSLAG: Kombinationsrisk — likviditetsgrad under minimum samt negativt rörelseresultat. VARNING: Eget kapital täcker mindre än 0.300000 av kreditbeloppet. VARNING: Kombinationsrisk kassaflöde + skuldsättning. "

describe("scoringConverter", () => {
  describe("getMetric", () => {
    it("extracts metrics with [REJECTED] status correctly", () => {
      expect(getMetric(SCORING_RESULT, "soliditet")).toEqual({
        value: 0.08,
        scoringStatus: "REJECT"
      })

      expect(getMetric(SCORING_RESULT, "skuldsättningsgrad")).toEqual({
        value: 11.5,
        scoringStatus: "REJECT",
      })
    })

    it("extracts metrics with [FLAGGED] status correctly", () => {
      expect(getMetric(SCORING_RESULT, "likviditetsgrad")).toEqual({
        value: 0.67,
        scoringStatus: "FLAGGED",
      })
    })

    it("handles negative decimal metric values correctly", () => {
      expect(getMetric(SCORING_RESULT, "rörelsemarginal")).toEqual({
        value: -0.08,
        scoringStatus: "FLAGGED",
      })
      expect(getMetric(SCORING_RESULT, "kassaflödeskvot")).toEqual({
        value: -0.043,
        scoringStatus: "REJECT",
      })
      expect(getMetric(SCORING_RESULT, "ränteTäckning")).toEqual({
        value: -1.67,
        scoringStatus: "REJECT",
      })
    })

    it("extracts numeric values without a bracketed status code", () => {
      expect(getMetric(SCORING_RESULT, "kreditPoäng")).toEqual({
        value: -177,
        scoringStatus: null,
      })
    })
  })

  describe("parseDecisionReasons", () => {
    it("parses raw string into structured decision details with correct types and titles", () => {
      const results = parseDecisionReasons(DECISION_REASON)
      expect(results.length).toBeGreaterThan(0)

      // Verify first parsed element removes header and prefix correctly
      expect(results[0]).toEqual({
        type: "REJECTION",
        title: "Soliditet för låg (0.08 < 0.20 gräns)",
        rawText: "AVSLAG: Soliditet för låg (0.08 < 0.20 gräns)",
      })

      // Verify WARNING classification
      const warningItem = results.find((r) => r.type === "WARNING")
      expect(warningItem).toBeDefined()
      expect(warningItem?.title).not.toMatch(/^VARNING:\s*/)

      // Verify NOTE classification
      const noteItem = results.find((r) => r.rawText.startsWith("Not:"))
      expect(noteItem).toEqual({
        type: "NOTE",
        title: "Kortfristiga skulder överstiger omsättningstillgångar",
        rawText: "Not: Kortfristiga skulder överstiger omsättningstillgångar",
      })
    })

    it("returns an empty array when given an empty string", () => {
      expect(parseDecisionReasons("")).toEqual([])
    })
  })

  describe("getCustomerRejectionsSummary", () => {
    it("extracts matching customer-facing rejection reasons from real decision string", () => {
      const summary = getCustomerRejectionsSummary(DECISION_REASON)

      expect(summary).toEqual([
        "Balansen mellan företagets kapital och skulder uppfyller inte kraven.",
        "Kortsiktig betalningsförmåga eller kassaflöde bedöms som otillräckligt.",
        "Rörelseresultatet täcker i dagsläget inte beräknade räntekostnader.",
      ])
    })

    it("caps the maximum number of summary reasons to 3", () => {
      const summary = getCustomerRejectionsSummary(DECISION_REASON)
      expect(summary.length).toBeLessThanOrEqual(3)
    })

    it("returns fallback message when no specific rejection keywords are found", () => {
      const summary = getCustomerRejectionsSummary("=== ANSÖKAN AVSLAGEN === AVSLAG: Allmän avvikelse.")
      expect(summary).toEqual([
        "Kreditbedömningen kunde inte godkännas utifrån företagets nuvarande siffror.",
      ])
    })
  })
})