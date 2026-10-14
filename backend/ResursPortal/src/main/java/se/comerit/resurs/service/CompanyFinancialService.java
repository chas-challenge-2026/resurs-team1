package se.comerit.resurs.service;

import org.springframework.stereotype.Service;
import se.comerit.resurs.client.companyvalidation.CompanyValidationClient;
import se.comerit.resurs.client.companyvalidation.RegistryResponseValidator;
import se.comerit.resurs.dto.companyvalidation.CompanyFinancialApiDTO;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class CompanyFinancialService {

   private final CompanyValidationClient client;
   private final RegistryResponseValidator responseValidator;

    public CompanyFinancialService(CompanyValidationClient client, RegistryResponseValidator responseValidator) {
        this.client = client;
        this.responseValidator = responseValidator;
    }

    public Optional<CompanyFinancialApiDTO> fetchLatestAnnualReport(String orgNumber) {
        return client.fetchLatestAnnualReport(orgNumber)
                .map(report -> responseValidator.validate(report, orgNumber))
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
