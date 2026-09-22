package se.comerit.resurs.service;

import org.springframework.stereotype.Service;
import se.comerit.resurs.client.companyvalidation.CompanyValidationClient;
import se.comerit.resurs.dto.companyvalidation.CompanyFinancialApiDTO;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class CompanyFinancialService {

   private final CompanyValidationClient client;

public CompanyFinancialService(CompanyValidationClient client){
    this.client = client;
}

    public Optional<CompanyFinancialApiDTO> fetchLatestAnnualReport(String orgNumber) {
        return client.fetchLatestAnnualReport(orgNumber)
                .map(this::withCashFlow);
    }

    private CompanyFinancialApiDTO withCashFlow(CompanyFinancialApiDTO report) {
        if (report.cashFlowStatement() != null) {
            return report;
        }
        return new CompanyFinancialApiDTO(report.incomeStatement(), report.balanceSheet(),
                new CompanyFinancialApiDTO.CompanyCashFlowStatement(BigDecimal.ZERO, BigDecimal.ZERO));
    }
}
