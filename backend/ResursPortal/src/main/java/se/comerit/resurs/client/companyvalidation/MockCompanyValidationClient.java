package se.comerit.resurs.client.companyvalidation;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import se.comerit.resurs.dto.companyvalidation.CompanyFinancialApiDTO;
import se.comerit.resurs.dto.companyvalidation.CompanyValidationApiDTO;
import se.comerit.resurs.exception.companyvalidation.CompanyRegistryUnavailableException;

import java.util.Optional;

import static se.comerit.resurs.client.companyvalidation.MockCompanyData.ANNUAL_REPORTS;
import static se.comerit.resurs.client.companyvalidation.MockCompanyData.REGISTRY;
import static se.comerit.resurs.client.companyvalidation.MockCompanyData.UNAVAILABLE_ORG_NUMBER;

@Component
@ConditionalOnProperty(prefix = "company-validation", name = "client", havingValue = "mock")
class MockCompanyValidationClient implements CompanyValidationClient {

    @Override
    public Optional<CompanyValidationApiDTO> lookup(String orgNumber) {
        throwIfUnavailable(orgNumber);
        return Optional.ofNullable(REGISTRY.get(orgNumber));
    }

    @Override
    public Optional<CompanyFinancialApiDTO> fetchLatestAnnualReport(String orgNumber) {
        throwIfUnavailable(orgNumber);
        return Optional.ofNullable(ANNUAL_REPORTS.get(orgNumber));
    }

    private void throwIfUnavailable(String orgNumber) {
        if (UNAVAILABLE_ORG_NUMBER.equals(orgNumber)) {
            throw new CompanyRegistryUnavailableException("company-registry", orgNumber,
            new ResourceAccessException("Simulated registry timeout"));
        }
    }
}
