package se.comerit.resurs.dto.companyvalidation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CompanyFinancialApiDTO(
        @NotNull @Valid CompanyIncomeStatement incomeStatement,
        @NotNull @Valid CompanyBalanceSheet balanceSheet,
        @Valid CompanyCashFlowStatement cashFlowStatement
){

    public record CompanyIncomeStatement(
        @NotNull BigDecimal revenue,
        @NotNull BigDecimal operatingResult,
        @NotNull BigDecimal interestExpenses
    ){}
    public record CompanyBalanceSheet(
        @NotNull BigDecimal equity,
        @NotNull BigDecimal currentAssets,
        @NotNull BigDecimal totalAssets,
        @NotNull BigDecimal shortTermLiabilities,
        @NotNull BigDecimal longTermLiabilities
    ){}
    public record CompanyCashFlowStatement(
        @NotNull BigDecimal operatingCashFlow,
        @NotNull BigDecimal investmentCashFlow
    ){}
}
