// same as back end. 
export type UserRole = "company" | "caseWorker"

// TODO: add more later as needed
export interface User {
  id: string
  name: string
  role: UserRole
}
