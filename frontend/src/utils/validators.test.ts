import { describe, expect, it } from "vitest";
import { isValidOrgNumber, isValidPhone } from "./validators";

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

  it("accepts swedish phone numbers written in common ways", () => {
    expect(isValidPhone("070-123 45 67")).toBe(true)
    expect(isValidPhone("0701234567")).toBe(true)
    expect(isValidPhone("+46 70 123 45 67")).toBe(true)
    expect(isValidPhone("08-123 456 78")).toBe(true)
  })

  it("accepts foreign phone numbers with a country code", () => {
    expect(isValidPhone("+358 40 123 4567")).toBe(true)
    expect(isValidPhone("+47 912 34 567")).toBe(true)
  })

  it("returns false for junk and numbers of the wrong length", () => {
    expect(isValidPhone("------")).toBe(false)
    expect(isValidPhone("070-123 45 67 89 01")).toBe(false)
    expect(isValidPhone("")).toBe(false)
  })
})
