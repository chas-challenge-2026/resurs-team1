import { describe, expect, it } from "vitest";
import { getDefaultRedirectPath } from "./auth";
import type { User } from "../types/user";

const location = "/url"

describe("authUtils", () => {
  describe("getDefaultRedirectPath", () => {
    it("returns '/' when user is undefined", () => {
      expect(getDefaultRedirectPath(undefined, location)).toBe("/")
    })

    it("/returns '/oversikt' when user has company role", () => {
      const mockUser = { role: "company" } as User
      expect(getDefaultRedirectPath(mockUser, location)).toBe("/oversikt")
    })

    it("/returns '/arenden' when user has caseworker role", () => {
      const mockUser = { role: "caseWorker" } as User
      expect(getDefaultRedirectPath(mockUser, location)).toBe("/arenden")
    })
  })
})