import api from "./client"

export interface FinancialData {
  incomeStatement: {
    revenue: number,
    operatingResult: number,
    interestExpenses: number
  },
  balanceSheet: {
    equity: number,
    currentAssets: number,
    totalAssets: number,
    shortTermLiabilities: number,
    longTermLiabilities: number
  },
  cashFlowStatement: {
    operatingCashFlow: number,
    investmentCashFlow: number
  }
}

export const getFinancials = async (): Promise<FinancialData> => {
  const response = await api.get<FinancialData>("/financial/verification")
  return response.data
}