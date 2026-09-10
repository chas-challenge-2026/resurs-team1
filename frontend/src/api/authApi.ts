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