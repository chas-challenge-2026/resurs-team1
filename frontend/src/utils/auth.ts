import type { User } from "../types/user"

//Get displayname
export const getUserDisplayName = (user: User): string => {
  return user.role === "company" ? user.displayName : user.name;
  
}

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