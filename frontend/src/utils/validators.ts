export const isValidOrgNumber = (orgNumber: string): boolean => {
  const trimmed = orgNumber.trim()
  const orgRegex = /^\d{6}-\d{4}$/
  return orgRegex.test(trimmed)
}