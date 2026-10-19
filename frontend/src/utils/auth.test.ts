import { describe, expect, it } from "vitest";
import { getDefaultRedirectPath } from "./auth";
import type { User } from "../types/user";

describe("authUtils", () => {
  describe("getDefaultRedirectPath", () => {
    it("returns '/' when user is undefined", () => {
      expect(getDefaultRedirectPath(undefined)).toBe("/")
    })

    it("/returns '/oversikt' when user has company role", () => {
      const mockUser = { role: "company" } as User
      expect(getDefaultRedirectPath(mockUser)).toBe("/oversikt")
    })

    it("/returns '/arenden' when user has caseworker role", () => {
      const mockUser = { role: "caseWorker" } as User
      expect(getDefaultRedirectPath(mockUser)).toBe("/arenden")
    })
  })
})