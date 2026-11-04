import { describe, expect, it } from "vitest";
import { format10DigitNumber, formatCurrency, formatDate, formatNumberWithSpaces } from "./formatters";

describe("formatters", () => {
  describe("formatCurrency", () => {
    it("returns currency with spaces and defaults unit to 'kr'", () => {
      const result = formatCurrency(3000000)

      // Replace all kinds of blank space with regular blank space
      const cleanResult = result.replace(/\s/g, " ")

      expect(cleanResult).toBe("3 000 000 kr")
    })

    it("returns given unit if given", () => {
      expect(formatCurrency(500, "EUR")).toBe("500 EUR")
    })
  })

  describe("formatNumberWithSpaces", () => {
    it("formats a raw numeric string with thousand separators", () => {
      const result = formatNumberWithSpaces("3000000")
      const cleanResult = result.replace(/\s/g, " ")
      expect(cleanResult).toBe("3 000 000")
    })

    it("strips non-digit characters like letters before formatting", () => {
      const result = formatNumberWithSpaces("30fe0h0")
      const cleanResult = result.replace(/\s/g, " ")
      expect(cleanResult).toBe("3 000")
    })

    it("returns an empty string when input contains no digits", () => {
      expect(formatNumberWithSpaces("abc")).toBe("")
      expect(formatNumberWithSpaces("")).toBe("")
    })
  })

  describe("formatDate", () => {
    it("formats a valid ISO timestamp string to YYYY-MM-DD format", () => {
      expect(formatDate("2026-10-15T12:00:00Z")).toBe("2026-10-15")
    })

    it("formats a simple date-only ISO string correctly", () => {
      expect(formatDate("2026-10-15")).toBe("2026-10-15")
    })

    it("returns '-' when given an empty string", () => {
      expect(formatDate("")).toBe("-")
    })
  })

  describe("format10DigitNumber", () => {
    it("returns raw digits without hyphen if length is 6 or less", () => {
      expect(format10DigitNumber("556000")).toBe("556000")
    })

    it("appends a hyphen after the 6th digit when a 7th digit is present", () => {
      expect(format10DigitNumber("5560001")).toBe("556000-1")
      expect(format10DigitNumber("5560001234")).toBe("556000-1234")
    })

    it("strips non-digit characters before formatting", () => {
      expect(format10DigitNumber("556000-1234")).toBe("556000-1234")
      expect(format10DigitNumber("a55600bc0123d4")).toBe("556000-1234")
    })

    it("set input to maximum 10 digits", () => {
      expect(format10DigitNumber("5560001234567")).toBe("556000-1234")
    })

    it("returns an empty string if input contains no digits", () => {
      expect(format10DigitNumber("")).toBe("")
      expect(format10DigitNumber("abc")).toBe("")
    })
  })
})