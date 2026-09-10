import type { CaseWorkerUser, CompanyUser } from "../types/user"
import api from "../utils/api"

export interface CompanyLoginPayload {
  orgNumber: string
}

export interface CaseworkerLoginPayload {
  email: string,
  password: string
}

export const loginCompany = async (payload: CompanyLoginPayload): Promise<CompanyUser> => {
  // stands in for the wait while the user signs in the BankID app
  await new Promise((resolve) => setTimeout(resolve, 4000))
  const response = await api.post<CompanyUser>("/auth/login/company", payload)
  return response.data
}

export const loginCaseWorker = async (payload: CaseworkerLoginPayload): Promise<CaseWorkerUser> => {
  const response = await api.post<CaseWorkerUser>("/auth/login/caseworker", payload)
  return response.data
}

export const logout = async ():Promise<void> => {
  await api.post("/auth/logout")
}