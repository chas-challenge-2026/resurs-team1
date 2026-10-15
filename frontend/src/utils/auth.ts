import type { User } from "../types/user"

//Get default redirect path
export const getDefaultRedirectPath = (user: User | undefined): string => {
  if (!user) return "/"

  switch (user.role) {
    case 'company':
      return '/oversikt'
    case 'caseWorker':
      return '/arenden'
    default:
      return '/'
  }
}