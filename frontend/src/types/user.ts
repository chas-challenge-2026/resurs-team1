// same as back end. 
export type UserRole = "company" | "caseWorker"

export interface CompanyUser {
  userId: number;
  role: "company";
  orgNumber: string;
  displayName: string;
  name?: string;
}

export interface CaseWorkerUser {
  userId: number;
  role: "caseWorker";
  name: string;
  email: string;
}

export type User = CompanyUser | CaseWorkerUser