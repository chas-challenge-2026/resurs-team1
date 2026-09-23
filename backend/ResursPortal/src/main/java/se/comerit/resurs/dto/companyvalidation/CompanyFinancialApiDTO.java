package se.comerit.resurs.dto.companyvalidation;

import java.math.BigDecimal;

public record CompanyFinancialApiDTO(
        CompanyIncomeStatement incomeStatement,
        CompanyBalanceSheet balanceSheet,
        CompanyCashFlowStatement cashFlowStatement
){

    public record CompanyIncomeStatement(
        BigDecimal revenue,
        BigDecimal operatingResult,
        BigDecimal interestExpenses
    ){}
    public record CompanyBalanceSheet(
        BigDecimal equity,
        BigDecimal currentAssets,
        BigDecimal totalAssets,
        BigDecimal shortTermLiabilities,
        BigDecimal longTermLiabilities
    ){}
    public record CompanyCashFlowStatement(
        BigDecimal operatingCashFlow,
        BigDecimal investmentCashFlow
    ){}
}
