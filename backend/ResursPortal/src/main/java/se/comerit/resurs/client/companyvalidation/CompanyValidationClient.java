package se.comerit.resurs.client.companyvalidation;

import se.comerit.resurs.dto.companyvalidation.CompanyFinancialApiDTO;
import se.comerit.resurs.dto.companyvalidation.CompanyValidationApiDTO;

import java.util.Optional;

public interface CompanyValidationClient {
    Optional <CompanyValidationApiDTO> lookup(String orgNumber);
    Optional<CompanyFinancialApiDTO> fetchLatestAnnualReport(String orgNumber);
}
