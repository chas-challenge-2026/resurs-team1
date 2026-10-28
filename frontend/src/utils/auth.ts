import type { User } from "../types/user"

//Get default redirect path
export const getDefaultRedirectPath = (user: User | undefined, currentPath: string): string => {
  if (!user) {
    if (currentPath.startsWith("/handlaggare")) {
        return "/handlaggare"
      }
      return "/"
  }

  switch (user.role) {
    case "company":
      return "/oversikt"
    case "caseWorker":
      return "/arenden"
    default:
      return "/"
  }
}