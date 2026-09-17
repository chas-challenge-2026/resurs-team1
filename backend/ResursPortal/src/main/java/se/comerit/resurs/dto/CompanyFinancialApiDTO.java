package se.comerit.resurs.dto;

//DTO that roughly matches the structure gotten from BolagsAPI,  in the real implementation we would've built this from
//several webcalls to BolagsAPI's endpoints to get every single field
// (not including cashflow as BolagsAPI's as seen in their documentation)

public record CompanyFinancialApiDTO(
        String orgNumber,
        String name,
        String authorizedSignatory, //#todo This should be a list in later implementation, But we leave it as this
        CompanyIncomeStatement incomeStatement,
        CompanyBalanceSheet balanceSheet,
        CompanyCashFlowStatement cashFlowStatement
){

    public record CompanyIncomeStatement(
        int revenue,
        int operatingResult,
        int interest_expenses //Not available in BolagsAPI,  might be on BolagsVerket
    ){}
    public record CompanyBalanceSheet(
        int equity,
        int currentAssets,
        int totalAssets,
        int shortTermLiabilities,
        int longTermLiabilities
    ){}
    public record CompanyCashFlowStatement( //cash flow statements not available in BolagsAPI
        int OperatingCashFlow,
        int InvestmentCashFlow
    ){}


}
