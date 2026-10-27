// same as back end. 
export type UserRole = "company" | "caseWorker"

export interface CompanyUser {
  userId: number;
  role: "company";
  orgNumber: string;
  displayName: string;
  name?: string;
  companyName?: string; //Backend sends companyname instead of displayname on login endpoint
}

export interface CaseWorkerUser {
  userId: number;
  role: "caseWorker";
  displayName: string;
  name?: string; //Backend sends name instead of displayname on login endpoint
}

export type User = CompanyUser | CaseWorkerUser