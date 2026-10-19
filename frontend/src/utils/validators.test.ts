import { describe, expect, it } from "vitest";
import { isValidOrgNumber } from "./validators";

describe("validators", () => {
  it("returns true for valid 10-digit org numbers with hyphen", () => {
    expect(isValidOrgNumber("556000-1234")).toBe(true)
  })

  it("trims leading and trailing whitespace before validating", () => {
    expect(isValidOrgNumber("  556000-1234  ")).toBe(true)
  })

  it("returns false if the hyphen is missing", () => {
    expect(isValidOrgNumber("5560001234")).toBe(false)
  })

  it("returns false for incorrect number of digits or letters", () => {
    expect(isValidOrgNumber("55600-1234")).toBe(false)
    expect(isValidOrgNumber("556000-123")).toBe(false)
    expect(isValidOrgNumber("abc56000-1234")).toBe(false)
    expect(isValidOrgNumber("")).toBe(false)
  })
})