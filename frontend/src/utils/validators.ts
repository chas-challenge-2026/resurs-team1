import { isValidPhoneNumber } from "libphonenumber-js"

export const isValidOrgNumber = (orgNumber: string): boolean => {
  const trimmed = orgNumber.trim()
  const orgRegex = /^\d{6}-\d{4}$/
  return orgRegex.test(trimmed)
}

// numbers without a country code are read as swedish, +358 etc still works
export const isValidPhone = (phoneNumber: string): boolean =>
  isValidPhoneNumber(phoneNumber, "SE")
