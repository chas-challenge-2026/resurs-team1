import api from "../utils/api"

export interface CompanyLoginPayload {
  orgNumber: string
}

export interface CaseworkerLoginPayload {
  email: string,
  password: string
}

export interface CompanyAuthResponse {
  userId: number,
  role: "company",
  orgNumber: string,
  companyName: string
}

export interface CaseworkerAuthResponse {
  userId: number,
  role: "caseworker",
  name: string,
  email: string
}

export type AuthResponse = CompanyAuthResponse | CaseworkerAuthResponse

export const loginCompany = async (payload: CompanyLoginPayload): Promise<CompanyAuthResponse> => {
  const response = await api.post<CompanyAuthResponse>("/auth/login/company", payload)
  return response.data
}

export const loginCaseworker = async (payload: CaseworkerLoginPayload): Promise<CaseworkerAuthResponse> => {
  const response = await api.post<CaseworkerAuthResponse>("/auth/login/caseworker", payload)
  return response.data
}

export const logout = async ():Promise<void> => {
  await api.post("/auth/logout")
}