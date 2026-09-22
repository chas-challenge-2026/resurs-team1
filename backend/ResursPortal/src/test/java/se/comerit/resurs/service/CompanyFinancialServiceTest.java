package se.comerit.resurs.service;

import org.junit.jupiter.api.Test;
import se.comerit.resurs.client.companyvalidation.CompanyValidationClient;
import se.comerit.resurs.dto.companyvalidation.CompanyFinancialApiDTO;
import se.comerit.resurs.dto.companyvalidation.CompanyFinancialApiDTO.CompanyBalanceSheet;
import se.comerit.resurs.dto.companyvalidation.CompanyFinancialApiDTO.CompanyCashFlowStatement;
import se.comerit.resurs.dto.companyvalidation.CompanyFinancialApiDTO.CompanyIncomeStatement;
import se.comerit.resurs.dto.companyvalidation.CompanyValidationApiDTO;
import se.comerit.resurs.exception.companyvalidation.CompanyRegistryUnavailableException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CompanyFinancialServiceTest testar hämtningen av årsredovisningen från den externa källan.
 *
 * Den täcker:
 * - bolaget har en årsredovisning
 * - bolaget saknar årsredovisning
 * - årsredovisning utan kassaflödesanalys -> kassaflödet sätts till noll
 * - källan svarar inte
 *
 * Servicen rör inte databasen, så testet kör utan Spring och utan Testcontainers.
 * Klienten ersätts av StubClient nedan.
 */
public class CompanyFinancialServiceTest {

    private static final String ORG_NUMBER = "556000-1234";

    private static final CompanyIncomeStatement INCOME = new CompanyIncomeStatement(
            new BigDecimal("12000000"), new BigDecimal("1800000"), new BigDecimal("300000"));

    private static final CompanyBalanceSheet BALANCE = new CompanyBalanceSheet(
            new BigDecimal("8000000"), new BigDecimal("3000000"), new BigDecimal("20000000"),
            new BigDecimal("1500000"), new BigDecimal("10500000"));

    private static final CompanyCashFlowStatement CASH_FLOW = new CompanyCashFlowStatement(
            new BigDecimal("2000000"), new BigDecimal("-1000000"));

    @Test
    void fetchLatestAnnualReport_shouldReturnReport_whenRegistryHasOne() {
        CompanyFinancialService service = new CompanyFinancialService(
                new StubClient(new CompanyFinancialApiDTO(INCOME, BALANCE, CASH_FLOW)));

        Optional<CompanyFinancialApiDTO> result = service.fetchLatestAnnualReport(ORG_NUMBER);

        assertTrue(result.isPresent());
        assertEquals(new BigDecimal("12000000"), result.get().incomeStatement().revenue());
        assertEquals(new BigDecimal("8000000"), result.get().balanceSheet().equity());
    }

    @Test
    void fetchLatestAnnualReport_shouldKeepCashFlow_whenCompanyReportsOne() {
        CompanyFinancialService service = new CompanyFinancialService(
                new StubClient(new CompanyFinancialApiDTO(INCOME, BALANCE, CASH_FLOW)));

        CompanyCashFlowStatement cashFlow = service.fetchLatestAnnualReport(ORG_NUMBER)
                .orElseThrow()
                .cashFlowStatement();

        assertEquals(new BigDecimal("2000000"), cashFlow.operatingCashFlow());
        assertEquals(new BigDecimal("-1000000"), cashFlow.investmentCashFlow());
    }

    @Test
    void fetchLatestAnnualReport_shouldDefaultCashFlowToZero_whenCompanyReportsNone() {
        CompanyFinancialService service = new CompanyFinancialService(
                new StubClient(new CompanyFinancialApiDTO(INCOME, BALANCE, null)));

        CompanyCashFlowStatement cashFlow = service.fetchLatestAnnualReport(ORG_NUMBER)
                .orElseThrow()
                .cashFlowStatement();

        assertNotNull(cashFlow);
        assertEquals(BigDecimal.ZERO, cashFlow.operatingCashFlow());
        assertEquals(BigDecimal.ZERO, cashFlow.investmentCashFlow());
    }

    @Test
    void fetchLatestAnnualReport_shouldReturnEmpty_whenCompanyHasNoAnnualReport() {
        CompanyFinancialService service = new CompanyFinancialService(new StubClient(null));

        Optional<CompanyFinancialApiDTO> result = service.fetchLatestAnnualReport(ORG_NUMBER);

        assertTrue(result.isEmpty());
    }

    @Test
    void fetchLatestAnnualReport_shouldPropagateException_whenRegistryIsUnavailable() {
        CompanyFinancialService service = new CompanyFinancialService(new UnavailableClient());

        assertThrows(CompanyRegistryUnavailableException.class,
                () -> service.fetchLatestAnnualReport(ORG_NUMBER));
    }

    /** Svarar med den årsredovisning testet skickar in, eller tomt när den är null. */
    private static class StubClient implements CompanyValidationClient {

        private final CompanyFinancialApiDTO report;

        StubClient(CompanyFinancialApiDTO report) {
            this.report = report;
        }

        @Override
        public Optional<CompanyValidationApiDTO> lookup(String orgNumber) {
            return Optional.empty();
        }

        @Override
        public Optional<CompanyFinancialApiDTO> fetchLatestAnnualReport(String orgNumber) {
            return Optional.ofNullable(report);
        }
    }

    /** Simulerar att den externa källan inte svarar. */
    private static class UnavailableClient implements CompanyValidationClient {

        @Override
        public Optional<CompanyValidationApiDTO> lookup(String orgNumber) {
            throw new CompanyRegistryUnavailableException("company-registry", orgNumber, null);
        }

        @Override
        public Optional<CompanyFinancialApiDTO> fetchLatestAnnualReport(String orgNumber) {
            throw new CompanyRegistryUnavailableException("company-registry", orgNumber, null);
        }
    }
}
