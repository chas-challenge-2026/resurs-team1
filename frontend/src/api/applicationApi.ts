import api from "./client"

export type ApplicationStatus =
  | "PENDING_DOCS"
  | "UNDER_REVIEW"
  | "APPROVED"
  | "REJECTED"

export interface Application {
  id: number
  requested_amount: number
  purpose: string
  status: ApplicationStatus
  decision?: string | null
  decision_reason?: string | null
  scoring_result?: string | null
  auditLog?: string | null
  created_at: string
  updated_at: string
  company_name: string
  org_number: string
  authorized_signatory: string
  duration_months?: number
}

export const getApplications = async (): Promise<Application[]> => {
  const response = await api.get<Application[]>("/application")
  return response.data
}