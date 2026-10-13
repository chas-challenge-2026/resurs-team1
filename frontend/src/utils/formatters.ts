import { PURPOSE_OPTIONS } from "../constants/constants";

const swedishCurrencyFormatter = new Intl.NumberFormat("sv-SE");

/**
 * Formats a numeric value to a localized Swedish currency string.
 *
 * @param value - The numerical value to format (e.g., 3000000).
 * @param unit - Optional unit suffix (defaults to "kr").
 * @returns Formatted string (e.g., "3 000 000 kr").
 *
 * @example
 * formatCurrency(3000000) // "3 000 000 kr"
 * formatCurrency(500, "EUR") // "500 EUR"
 */
export const formatCurrency = (value: number, unit: string = "kr"): string => {
  if (isNaN(value)) return `0 ${unit}`.trim();
  
  const formatted = swedishCurrencyFormatter.format(value);
  return unit ? `${formatted} ${unit}`.trim() : formatted;
}

export const formatNumberWithSpaces = (val: string) => {
  // Remove non-numbers
  const cleanValue = val.replace(/\D/g, "")

  if (!cleanValue) return ""

  return new Intl.NumberFormat("sv-SE").format(Number(cleanValue))
}

export const formatDate = (isoString: string) => {
  if (!isoString) return "-"
  const date = new Date(isoString)
  return date.toISOString().split("T")[0]
}

export const formatReferenceNumber = (id: number) => {
  return `REF-${id}`
}

export function formatFilename(filename: string): string {
  return filename.replace(/^\d+_/, "");
}

export function getPurposeLabel(purpose: string): string {
  const option = PURPOSE_OPTIONS.find((opt) => opt.value === purpose);
  return option ? option.label : purpose;
}