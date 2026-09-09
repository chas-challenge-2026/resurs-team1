import api from "../utils/api"

export interface CompanyLoginPayload {
  orgNumber: string
}

export interface AgentLoginPayload {
  email: string,
  password: string
}

export interface AuthCompanyResponse {
  userId: number,
  role: "company",
  orgNumber: string,
  companyName: string
}

export interface AuthAgentResponse {
  userId: number,
  role: "caseworker",
  name: string,
  email: string
}

export type AuthResponse = AuthCompanyResponse | AuthAgentResponse

export const loginCompany = async (payload: CompanyLoginPayload): Promise<AuthCompanyResponse> => {
  const response = await api.post<AuthCompanyResponse>("/auth/login/company", payload)
  return response.data
}

export const loginAgent = async (payload: AgentLoginPayload): Promise<AuthAgentResponse> => {
  const response = await api.post<AuthAgentResponse>("/auth/login/caseworker", payload)
  return response.data
}

export const logout = async ():Promise<void> => {
  await api.post("/auth/logout")
}