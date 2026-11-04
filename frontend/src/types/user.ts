// same as back end. 
export type UserRole = "COMPANY" | "CASEWORKER" | "CERTIFIED" | "ADMIN"

export interface CompanyUser {
  userId: number;
  roles: UserRole[];
  orgNumber: string;
  displayName: string;
  name?: string;
  companyName?: string; //Backend sends companyname instead of displayname on login endpoint
}

export interface CaseWorkerUser {
  userId: number;
  roles: UserRole[];
  displayName: string;
  name?: string; //Backend sends name instead of displayname on login endpoint
}

export type User = CompanyUser | CaseWorkerUser