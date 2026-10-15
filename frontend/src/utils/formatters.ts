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

  return swedishCurrencyFormatter.format(Number(cleanValue))
}

export const formatDate = (isoString: string) => {
  if (!isoString) return "-"
  const date = new Date(isoString)
  return date.toISOString().split("T")[0]
}

// force the shape
// the "-" waits for a digit to follow it, otherwise backspace can never delete it
export const format10DigitNumber = (value: string) => {
  const digits = value.replace(/\D/g, "").slice(0, 10)
  return digits.length > 6 ? `${digits.slice(0, 6)}-${digits.slice(6)}` : digits // add "-"
}