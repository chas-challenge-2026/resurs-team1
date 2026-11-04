import { describe, expect, it } from "vitest";
import { formatFilename, formatReferenceNumber, getPurposeLabel } from "./domainFormatters";

describe("domainFormatters", () => {
  describe("formatReferenceNumber", () => {
    it("prefixes a numeric ID with 'REF-'", () => {
      expect(formatReferenceNumber(123)).toBe("REF-123")
    })
  })

  describe("formatFileName", () => {
    it("removes leading ID digits and underscore from filename", () => {
      expect(formatFilename("123_arsredovisning.pdf")).toBe("arsredovisning.pdf")
    })

    it("preserves numbers that appear later in the filename", () => {
      expect(formatFilename("123_rapport_2024_final.pdf")).toBe("rapport_2024_final.pdf")
    })
  })

  describe("getPurposeLabel", () => {
    it("returns the correct label for a valid purpose value", () => {
      expect(getPurposeLabel("workingCapital")).toBe("Rörelsekapital")
    })

    it("returns the input value as fallback if purpose is not found in options", () => {
      expect(getPurposeLabel("unknownPurpose")).toBe("unknownPurpose")
    })
  })
})