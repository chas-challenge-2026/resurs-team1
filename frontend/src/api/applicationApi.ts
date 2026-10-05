import api from "./client"

export type ApplicationStatus =
  | "PENDING_DOCS"
  | "UNDER_REVIEW"
  | "APPROVED"
  | "REJECTED"

export type Decision = "APPROVED" | "REJECTED"

export type PurposeValue =
  | "Expansion"
  | "workingCapital"
  | "Investment"
  | "Other"

export interface ApplicationDocument {
  id: number
  applicationId: number,
  filename: string
  docType: string
  uploadedAt: string
}

// CompanyFinancialApiDTO.java
export interface CompanyFinances {
  incomeStatement: {
    revenue: number
    operatingResult: number
    interestExpenses: number
  }
  balanceSheet: {
    equity: number
    currentAssets: number
    totalAssets: number
    shortTermLiabilities: number
    longTermLiabilities: number
  }
  cashFlowStatement: {
    operatingCashFlow: number
    investmentCashFlow: number
  }
}

export interface Application {
  id: number
  requestedAmount: number
  purpose: PurposeValue
  status: ApplicationStatus
  decision?: string | null
  decisionReason?: string | null
  scoringResult?: string | null
  auditLog?: string | null
  createdAt: string
  updatedAt: string
  companyName: string
  orgNumber: string
  authorizedSignatory: string
  durationMonths: number
  documents?: ApplicationDocument[]
  companyFinances?: CompanyFinances | null // only from backoffice, null when no annual report is found
  contactDetails: { // I put as optional because mock data differs and there cold be old data in current DB
    name: string;
    email: string;
    phoneNumber: string;
  };
}

// matches "ApplicationFormData" as of september, but may change later since we could send info that was not in form, hence its own type
export interface NewApplicationPayload {
  orgNumber: string;

  contactDetails: {
    name: string;
    email: string;
    phoneNumber: string;
  };

  purpose: string;
  requestedAmount: number;
  durationMonths: number;
}

// same as ReviewInfo.java
export interface ReviewApplication {
  id: number
  requestedAmount: number
  purpose: string
  createdAt: string
  scoringResult: string | null
  decisionReason: string | null
  companyName: string
  orgNumber: string
}

// HistoricalReviewInfo.java
export interface DecidedApplication {
  id: number
  requestedAmount: number
  purpose: string
  decision: string | null
  createdAt: string
  updatedAt: string
  companyName: string
  orgNumber: string
}

// PagedResult.java
export interface PagedResult<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

// BackOfficeListsDTO.java
export interface BackofficeLists {
  reviewApplications: PagedResult<ReviewApplication>
  decidedApplications: PagedResult<DecidedApplication>
}

// one row in the caseworker sidebar, built from both lists above
export interface CaseListItem {
  id: number
  status: ApplicationStatus
  companyName: string
  orgNumber: string
  requestedAmount: number
  createdAt: string
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

export const postApplication = async (application: NewApplicationPayload): Promise<Application> => {
  const response = await api.post<Application>(`/application/apply`, application)

  return response.data
}

export const getBackofficeApplicationById = async (id: number): Promise<Application> => {
  const response = await api.get(`/backoffice/application/${id}`)
  return {
    ...response.data.appDetails.application,
    documents: response.data.appDetails.documents,
    companyFinances: response.data.companyFinances,
  }
}

export const getBackofficeApplications = async (): Promise<BackofficeLists> => {
  const response = await api.get<BackofficeLists>("/backoffice", { params: { review_size: 100, decided_size: 100 } }) // 100 is backend max, so search covers more cases
  return response.data
}

export const postDecision = async (applicationId: number, decision: Decision, comment: string): Promise<void> => {
  await api.post("/backoffice/decide", null, { params: { applicationId, decision, comment } })
}