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