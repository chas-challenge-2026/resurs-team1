package se.comerit.resurs.client.companyvalidation;


import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import se.comerit.resurs.dto.companyvalidation.CompanyFinancialApiDTO;
import se.comerit.resurs.dto.companyvalidation.CompanyValidationApiDTO;
import se.comerit.resurs.exception.companyvalidation.CompanyRegistryUnavailableException;

import java.util.Optional;

@Component
@Primary
public class ResilientCompanyValidationClient implements CompanyValidationClient{

    private static final String LOOKUP = "company-lookup";
    private static final String ANNUAL_REPORT = "annual-report";

    private final CompanyValidationClient delegate;


    public ResilientCompanyValidationClient(@Qualifier("rawCompanyValidationClient") CompanyValidationClient delegate) {
        this.delegate = delegate;
    }

    @Override
    @Retry(name = LOOKUP)
    @CircuitBreaker(name = LOOKUP, fallbackMethod = "lookupRejected")
    public Optional<CompanyValidationApiDTO>lookup(String orgNumber){
        return delegate.lookup(orgNumber);
    }

    @Override
    @Retry(name = ANNUAL_REPORT)
    @CircuitBreaker(name = ANNUAL_REPORT, fallbackMethod = "annualReportRejected")
    public Optional<CompanyFinancialApiDTO>fetchLatestAnnualReport(String orgNumber){
        return delegate.fetchLatestAnnualReport(orgNumber);
    }
    private  Optional<CompanyValidationApiDTO>lookupRejected(String orgNumber, CallNotPermittedException ex){
        throw new CompanyRegistryUnavailableException(LOOKUP, orgNumber, ex);
    }

    private Optional<CompanyFinancialApiDTO>annualReportRejected(String orgNumber, CallNotPermittedException ex){
        throw new CompanyRegistryUnavailableException(ANNUAL_REPORT, orgNumber, ex);
    }
}
