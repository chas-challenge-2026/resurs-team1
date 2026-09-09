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