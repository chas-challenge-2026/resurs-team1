package se.comerit.resurs.client.companyvalidation;

import se.comerit.resurs.dto.companyvalidation.CompanyFinancialApiDTO;
import se.comerit.resurs.dto.companyvalidation.CompanyFinancialApiDTO.CompanyBalanceSheet;
import se.comerit.resurs.dto.companyvalidation.CompanyFinancialApiDTO.CompanyCashFlowStatement;
import se.comerit.resurs.dto.companyvalidation.CompanyFinancialApiDTO.CompanyIncomeStatement;
import se.comerit.resurs.dto.companyvalidation.CompanyValidationApiDTO;
import se.comerit.resurs.dto.companyvalidation.CompanyValidationApiDTO.Signatory;
import se.comerit.resurs.enums.SigningRight;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

final class MockCompanyData {

    static final String UNAVAILABLE_ORG_NUMBER = "556000-5555";
    private static final Instant UPDATED_AT = Instant.parse("2026-08-14T10:06:04Z");

    static final Map<String, CompanyValidationApiDTO> REGISTRY = Map.of(
            "556000-1234", new CompanyValidationApiDTO("Fasen Elteknik AB","556000-1234", List.of(
                    new Signatory("750312-1234", "Anders Karlsson", "Styrelseordförande", SigningRight.ALONE)
            ), UPDATED_AT),

            "556000-5678", new CompanyValidationApiDTO("Britt Maries Ögonfransar AB", "556000-5678", List.of(
                    new Signatory("820624-5678", "Maria Svensson", "Styrelseordförande", SigningRight.ALONE),
                    new Signatory("750312-1234", "Anders Karlsson", "Styrelseledamot", SigningRight.ALONE)
            ), UPDATED_AT),

            "556000-7777", new CompanyValidationApiDTO("Gunnar Kruts Dynamit AB","556000-7777", List.of(
                    new Signatory("660930-7777", "Johan Berg", "Styrelseledamot", SigningRight.ALONE),
                    new Signatory("710214-7777", "Eva Berg", "Styrelseledamot", SigningRight.JOINTLY)
            ), UPDATED_AT),

            "556000-9999", new CompanyValidationApiDTO("Frukt och grönt Göteborg","556000-9999", List.of(
                    new Signatory("681105-9999", "Erik Lindqvist", "Styrelseordförande", SigningRight.ALONE)
            ), UPDATED_AT)
    );

    // 556000-7777 intentionally has no annual report
    static final Map<String, CompanyFinancialApiDTO> ANNUAL_REPORTS = Map.of(
            // Stable company: good equity ratio, liquidity and cash flow
            "556000-1234", new CompanyFinancialApiDTO(
                    new CompanyIncomeStatement(
                            sek("12000000"),   // revenue
                            sek("1800000"),    // operatingResult
                            sek("300000")      // interestExpenses
                    ),
                    new CompanyBalanceSheet(
                            sek("8000000"),    // equity
                            sek("3000000"),    // currentAssets
                            sek("20000000"),   // totalAssets
                            sek("1500000"),    // shortTermLiabilities
                            sek("10500000")    // longTermLiabilities
                    ),
                    new CompanyCashFlowStatement(
                            sek("2000000"),    // operatingCashFlow
                            sek("-1000000")    // investmentCashFlow
                    )
            ),

            // Borderline company: smaller company without cash flow statement
            "556000-5678", new CompanyFinancialApiDTO(
                    new CompanyIncomeStatement(
                            sek("8000000"),
                            sek("240000"),
                            sek("120000")
                    ),
                    new CompanyBalanceSheet(
                            sek("1500000"),
                            sek("3100000"),
                            sek("5000000"),
                            sek("2800000"),
                            sek("700000")
                    ),
                    null
            ),

            // Economically stable but struggling company which is likely to get stuck in review
            "556000-7777", new CompanyFinancialApiDTO(
                    new CompanyIncomeStatement(
                            sek("200000"),
                            sek("150000"),
                            sek("9000")
                    ),
                    new CompanyBalanceSheet(
                            sek("50000"),
                            sek("60000"),
                            sek("250000"),
                            sek("11000"),
                            sek("14000")
                    ),
                    new CompanyCashFlowStatement(
                            sek("100000"),
                            sek("50000")
                    )
            ),

            // Weak company: negative operating result and low equity ratio
            "556000-9999", new CompanyFinancialApiDTO(
                    new CompanyIncomeStatement(
                            sek("2000000"),
                            sek("-150000"),
                            sek("90000")
                    ),
                    new CompanyBalanceSheet(
                            sek("200000"),
                            sek("600000"),
                            sek("2500000"),
                            sek("900000"),
                            sek("1400000")
                    ),
                    new CompanyCashFlowStatement(
                            sek("-100000"),
                            sek("-50000")
                    )
            )
    );

    private MockCompanyData() {
    }

    private static BigDecimal sek(String amount) {
        return new BigDecimal(amount);
    }
}
