package se.comerit.resurs.service;

import org.springframework.stereotype.Service;
import se.comerit.resurs.client.companyvalidation.CompanyValidationClient;
import se.comerit.resurs.dto.companyvalidation.CompanyFinancialApiDTO;

import java.util.Optional;

@Service
public class CompanyFinancialService {

   private final CompanyValidationClient client;

public CompanyFinancialService(CompanyValidationClient client){
    this.client = client;
}

    public Optional<CompanyFinancialApiDTO> fetchLatestAnnualReport(String orgNumber){
        return client.fetchLatestAnnualReport(orgNumber);
    }
}
