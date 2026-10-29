import { describe, expect, it } from "vitest";
import { getDefaultRedirectPath, isAuthenticated, isCaseWorkerUser, isCompanyUser } from "./auth";
import type { CaseWorkerUser, CompanyUser, User } from "../types/user";

const mockCompanyUser: CompanyUser = {
  userId: 1,
  roles: ["COMPANY"],
  orgNumber: "556000-1234",
  displayName: "Fasen Elteknik AB",
};

const mockCaseWorkerUser: CaseWorkerUser = {
  userId: 2,
  roles: ["CASEWORKER"],
  displayName: "Anna Handläggare",
}

describe("authUtils", () => {
  describe("getDefaultRedirectPath", () => {
    it("returns '/' when user is undefined", () => {
      expect(getDefaultRedirectPath(undefined)).toBe("/")
    })

    it("/returns '/oversikt' when user has company role", () => {
      const mockUser = { roles: ["COMPANY"] } as User
      expect(getDefaultRedirectPath(mockUser)).toBe("/oversikt")
    })

    it("/returns '/arenden' when user has caseworker role", () => {
      const mockUser = { roles: ["CASEWORKER"] } as User
      expect(getDefaultRedirectPath(mockUser)).toBe("/arenden")
    })
  })

  describe("User Type Guards & Helpers", () => {
    describe("isCompanyUser", () => {
      it("return true for company user", () => {
        expect(isCompanyUser(mockCompanyUser)).toBe(true)
      })

      it("return false for caseworker user", () => {
        expect(isCompanyUser(mockCaseWorkerUser)).toBe(false)
      })

      it("returns false for undefined and null user", () => {
        expect(isCompanyUser(undefined)).toBe(false)
        expect(isCompanyUser(null)).toBe(false)
      })
    })

    describe("isCaseWorkerUser", () => {
      it("return false for company user", () => {
        expect(isCaseWorkerUser(mockCompanyUser)).toBe(false)
      })

      it("return true for caseworker user", () => {
        expect(isCaseWorkerUser(mockCaseWorkerUser)).toBe(true)
      })

      it("returns false for undefined and null user", () => {
        expect(isCaseWorkerUser(undefined)).toBe(false)
        expect(isCaseWorkerUser(null)).toBe(false)
      })
    })

    describe("isAuthenticated", () => {
      it("return true for signed in user", () => {
        expect(isAuthenticated(mockCompanyUser)).toBe(true)
        expect(isAuthenticated(mockCaseWorkerUser)).toBe(true)
      })

      it("returns false for undefined and null user", () => {
        expect(isAuthenticated(undefined)).toBe(false)
        expect(isAuthenticated(null)).toBe(false)
      })
    })
  })
})