import type { ScoringStatus } from "../types/scoring"

// backend sends inconsistently formatted strings, so we pick only the necessary values to display and format.

export interface MetricDetail {
  value: number | null
  scoringStatus: ScoringStatus | null
}

export function getMetric(scoring: string, key: string): MetricDetail {

  const pattern = new RegExp(`${key}=(-?\\d+(?:\\.\\d+)?)\\s*(?:\\[([A-Z]+)\\])?`) // regex grouped as seen in parantesis () -> (1) (2), where 0 is full regex match
  const match = scoring.match(pattern)

  if (!match) {
    return { value: null, scoringStatus: null}
  }

  return {
    value: Number(match[1]),
    scoringStatus: (match[2] as MetricDetail["scoringStatus"]) ?? null
  }
}

export interface DecisionDetail {
  type: "REJECTION" | "WARNING" | "NOTE"
  title: string
  rawText: string
}

export const parseDecisionReasons = (rawString: string): DecisionDetail[] => {
  if (!rawString) return []

  // Remove "=== ANSÖKAN AVSLAGEN ===" if present
  const cleaned = rawString.replace(/^===.*?===/g, '').trim()

  // Split string at "."
  const sentences = cleaned
    .split('.')
    .map((s) => s.trim())
    .filter((s) => s.length > 0)

  return sentences.map((sentence) => {
    let type: DecisionDetail['type'] = 'NOTE'
    let title = sentence

    if (sentence.startsWith('AVSLAG:')) {
      type = 'REJECTION'
      title = sentence.replace(/^AVSLAG:\s*/, '')
    } else if (sentence.startsWith('VARNING:')) {
      type = 'WARNING'
      title = sentence.replace(/^VARNING:\s*/, '')
    } else if (sentence.startsWith('Not:')) {
      type = 'NOTE'
      title = sentence.replace(/^Not:\s*/, '')
    }

    return { type, title, rawText: sentence }
  })
}

// TODO: Check if it includes all possible variants from backend
export const getCustomerRejectionsSummary = (rawString: string): string[] => {
  if (!rawString) return []

  const mainReasons: string[] = []

  if (rawString.includes('Soliditet för låg') || rawString.includes('skuldsättning')) {
    mainReasons.push('Balansen mellan företagets kapital och skulder uppfyller inte kraven.')
  }
  
  if (rawString.includes('Likviditetsgrad') || rawString.includes('kassaflöde')) {
    mainReasons.push('Kortsiktig betalningsförmåga eller kassaflöde bedöms som otillräckligt.')
  }

  if (rawString.includes('Räntetäckningsgrad') || rawString.includes('Negativt rörelseresultat')) {
    mainReasons.push('Rörelseresultatet täcker i dagsläget inte beräknade räntekostnader.')
  }

  // Fallback
  if (mainReasons.length === 0) {
    mainReasons.push('Kreditbedömningen kunde inte godkännas utifrån företagets nuvarande siffror.')
  }

  // Show maximum 3 reasons
  return mainReasons.slice(0, 3)
}