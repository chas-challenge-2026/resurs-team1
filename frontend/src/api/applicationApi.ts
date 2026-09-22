import api from "./client"

export type ApplicationStatus =
  | "PENDING_DOCS"
  | "UNDER_REVIEW"
  | "APPROVED"
  | "REJECTED"

export interface ApplicationDocument {
  id: number
  applicationId: number,
  filename: string
  docType: string
  uploadedAt: string
}

export interface Application {
  id: number
  requestedAmount: number
  purpose: string
  status: ApplicationStatus
  decision?: string | null
  decisionRreason?: string | null
  scoringResult?: string | null
  auditLog?: string | null
  createdAt: string
  updatedAt: string
  companyName: string
  orgNumber: string
  authorizedSignatory: string
  durationMonths?: number
  documents?: ApplicationDocument[]
}

export const getApplications = async (): Promise<Application[]> => {
  const response = await api.get<Application[]>("/application")
  return response.data
}

export const getApplicationById = async (id: number): Promise<Application> => {
  const response = await api.get(`application/${id}`)
  return {
    ...response.data.app,
    documents: response.data.documents,
  }
}

// matches "ApplicationFormData" as of september, but may change later since we could send info that was not in form, hence its own type
export interface NewApplicationPayload {
  orgNumber: string;
  companyName: string;

  contactName: string;
  email: string;
  phoneNumber: string;

  purpose: string;
  requestedAmount: number;
  durationMonths: number;
}

export const postApplication = async (application: NewApplicationPayload): Promise<Application> => {
  const response = await api.post<Application>(`/application/apply`, application)

  return response.data
}