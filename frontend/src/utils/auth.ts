import type { CaseWorkerUser, CompanyUser, User } from "../types/user"

//Get default redirect path
export const getDefaultRedirectPath = (user: User | undefined): string => {
  if (!user) return "/"

  if (user.roles.includes("CASEWORKER")) {
    return "/arenden";
  }

  if (user.roles.includes("COMPANY")) {
    return "/oversikt";
  }

  return "/"
}

export const isCompanyUser = (user: User | null | undefined): user is CompanyUser => {
  return user?.roles.includes("COMPANY") ?? false
}

export const isCaseWorkerUser = (user: User | null | undefined): user is CaseWorkerUser => {
  return user?.roles.includes("CASEWORKER") ?? false
}

export const isAuthenticated = (user: User | null | undefined): user is User => {
  return user !== null && user !== undefined
}