package se.comerit.resurs.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.cache.autoconfigure.CacheAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.ResourceAccessException;
import se.comerit.resurs.client.companyvalidation.CompanyValidationClient;
import se.comerit.resurs.client.companyvalidation.RegistryResponseValidator;
import se.comerit.resurs.config.CacheConfig;
import se.comerit.resurs.dto.companyvalidation.CompanyFinancialApiDTO;
import se.comerit.resurs.exception.companyvalidation.CompanyRegistryUnavailableException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {CompanyFinancialService.class, CacheConfig.class})
@ImportAutoConfiguration(CacheAutoConfiguration.class)
class CompanyFinancialServiceCacheTest {

    private static final String ORG_NUMBER = "556000-1234";
    private static final String OTHER_ORG_NUMBER = "556000-9876";

    @Autowired
    private CompanyFinancialService financialService;

    @Autowired
    private CacheManager cacheManager;

    @MockitoBean
    private CompanyValidationClient client;

    @MockitoBean
    private RegistryResponseValidator responseValidator;

    @BeforeEach
    void setUp() {
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
        when(responseValidator.validate(any(), anyString())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void fetchLatestAnnualReport_calledTwiceForSameOrgNumber_callsRegistryOnce() {
        when(client.fetchLatestAnnualReport(ORG_NUMBER)).thenReturn(Optional.of(annualReport()));

        Optional<CompanyFinancialApiDTO> first = financialService.fetchLatestAnnualReport(ORG_NUMBER);
        Optional<CompanyFinancialApiDTO> second = financialService.fetchLatestAnnualReport(ORG_NUMBER);

        assertThat(first).isPresent();
        assertThat(second).isEqualTo(first);
        verify(client, times(1)).fetchLatestAnnualReport(ORG_NUMBER);
    }

    @Test
    void fetchLatestAnnualReport_forDifferentOrgNumbers_callsRegistryForEach() {
        when(client.fetchLatestAnnualReport(anyString())).thenReturn(Optional.of(annualReport()));

        financialService.fetchLatestAnnualReport(ORG_NUMBER);
        financialService.fetchLatestAnnualReport(OTHER_ORG_NUMBER);

        verify(client, times(1)).fetchLatestAnnualReport(ORG_NUMBER);
        verify(client, times(1)).fetchLatestAnnualReport(OTHER_ORG_NUMBER);
    }

    @Test
    void fetchLatestAnnualReport_whenNoReportFound_doesNotCacheEmptyResult() {
        when(client.fetchLatestAnnualReport(ORG_NUMBER)).thenReturn(Optional.empty());

        assertThat(financialService.fetchLatestAnnualReport(ORG_NUMBER)).isEmpty();
        assertThat(financialService.fetchLatestAnnualReport(ORG_NUMBER)).isEmpty();

        verify(client, times(2)).fetchLatestAnnualReport(ORG_NUMBER);
    }

    @Test
    void fetchLatestAnnualReport_whenRegistryUnavailable_doesNotCacheFailure() {
        when(client.fetchLatestAnnualReport(ORG_NUMBER))
                .thenThrow(new CompanyRegistryUnavailableException("company-registry", ORG_NUMBER,
                        new ResourceAccessException("timeout")))
                .thenReturn(Optional.of(annualReport()));

        assertThatThrownBy(() -> financialService.fetchLatestAnnualReport(ORG_NUMBER))
                .isInstanceOf(CompanyRegistryUnavailableException.class);
        assertThat(financialService.fetchLatestAnnualReport(ORG_NUMBER)).isPresent();

        verify(client, times(2)).fetchLatestAnnualReport(ORG_NUMBER);
    }

    private CompanyFinancialApiDTO annualReport() {
        return new CompanyFinancialApiDTO(
                new CompanyFinancialApiDTO.CompanyIncomeStatement(
                        new BigDecimal("5000000"), new BigDecimal("600000"), new BigDecimal("50000")),
                new CompanyFinancialApiDTO.CompanyBalanceSheet(
                        new BigDecimal("1500000"), new BigDecimal("2000000"), new BigDecimal("4000000"),
                        new BigDecimal("1000000"), new BigDecimal("1500000")),
                new CompanyFinancialApiDTO.CompanyCashFlowStatement(
                        new BigDecimal("400000"), new BigDecimal("-200000")));
    }
}
