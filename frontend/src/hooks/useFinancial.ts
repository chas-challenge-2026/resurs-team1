import { useQuery } from "@tanstack/react-query"
import { getFinancials, type FinancialData } from "../api/financialApi"
import type { ApiErrorPayload } from "../api/client"

export const useGetFinancial = () => {
  return useQuery<FinancialData, ApiErrorPayload>({
    queryKey: ["financial"],
    queryFn: getFinancials,
  })
}