package se.comerit.resurs.service;

import org.junit.jupiter.api.Test;
import se.comerit.resurs.client.companyvalidation.CompanyValidationClient;
import se.comerit.resurs.dto.companyvalidation.CompanyFinancialApiDTO;
import se.comerit.resurs.dto.companyvalidation.CompanyValidationApiDTO;
import se.comerit.resurs.dto.companyvalidation.CompanyValidationApiDTO.Signatory;
import se.comerit.resurs.enums.SigningRight;
import se.comerit.resurs.exception.companyvalidation.CompanyRegistryUnavailableException;
import se.comerit.resurs.exception.companyvalidation.CompanyValidationFailedException;
import se.comerit.resurs.exception.companyvalidation.CompanyValidationFailureReason;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CompanyValidationServiceTest testar att företagsvalideringen tolkar svaret från den externa källan rätt.
 *
 * Den täcker:
 * - validateCompanyExists: bolaget finns, bolaget saknas, källan svarar inte
 * - validateSignatory: behörig firmatecknare, okänt personnummer, firmateckning kräver två personer
 *
 * Servicen rör inte databasen, så testet kör utan Spring och utan Testcontainers.
 * Klienten ersätts av StubClient nedan.
 */
public class CompanyValidationServiceTest {

    private static final String ORG_NUMBER = "556000-1234";
    private static final String SIGNS_ALONE = "197503121234";
    private static final String SIGNS_JOINTLY = "196609307777";
    private static final String UNKNOWN_PERSONAL_NUMBER = "199001019999";

    private static final CompanyValidationApiDTO COMPANY = new CompanyValidationApiDTO(
            "Malmö Fastigheter AB",
            ORG_NUMBER,
            List.of(
                    new Signatory(SIGNS_ALONE, "Anders Karlsson", "Styrelseordförande", SigningRight.ALONE),
                    new Signatory(SIGNS_JOINTLY, "Johan Berg", "Styrelseledamot", SigningRight.JOINTLY)
            ),
            Instant.parse("2026-08-14T10:06:04Z"));

    @Test
    void validateCompanyExists_shouldReturnCompany_whenRegistryHasIt() {
        CompanyValidationService service = new CompanyValidationService(new StubClient(COMPANY));

        CompanyValidationApiDTO result = service.validateCompanyExists(ORG_NUMBER);

        assertEquals(ORG_NUMBER, result.orgNumber());
        assertEquals("Malmö Fastigheter AB", result.companyName());
    }

    @Test
    void validateCompanyExists_shouldThrowCompanyNotFound_whenRegistryHasNoCompany() {
        CompanyValidationService service = new CompanyValidationService(new StubClient(null));

        CompanyValidationFailedException thrown = assertThrows(
                CompanyValidationFailedException.class,
                () -> service.validateCompanyExists(ORG_NUMBER));

        assertEquals(CompanyValidationFailureReason.COMPANY_NOT_FOUND, thrown.reason());
    }

    @Test
    void validateCompanyExists_shouldPropagateException_whenRegistryIsUnavailable() {
        CompanyValidationService service = new CompanyValidationService(new UnavailableClient());

        assertThrows(CompanyRegistryUnavailableException.class,
                () -> service.validateCompanyExists(ORG_NUMBER));
    }

    @Test
    void validateSignatory_shouldReturnSignatory_whenPersonMaySignAlone() {
        CompanyValidationService service = new CompanyValidationService(new StubClient(COMPANY));

        Signatory result = service.validateSignatory(COMPANY, SIGNS_ALONE);

        assertEquals("Anders Karlsson", result.name());
        assertEquals(SigningRight.ALONE, result.signingRight());
    }

    @Test
    void validateSignatory_shouldThrowNotAuthorized_whenPersonIsNotASignatory() {
        CompanyValidationService service = new CompanyValidationService(new StubClient(COMPANY));

        CompanyValidationFailedException thrown = assertThrows(
                CompanyValidationFailedException.class,
                () -> service.validateSignatory(COMPANY, UNKNOWN_PERSONAL_NUMBER));

        assertEquals(CompanyValidationFailureReason.NOT_AUTHORIZED_SIGNATORY, thrown.reason());
    }

    @Test
    void validateSignatory_shouldThrowRequiresJointSignature_whenPersonMayNotSignAlone() {
        CompanyValidationService service = new CompanyValidationService(new StubClient(COMPANY));

        CompanyValidationFailedException thrown = assertThrows(
                CompanyValidationFailedException.class,
                () -> service.validateSignatory(COMPANY, SIGNS_JOINTLY));

        assertEquals(CompanyValidationFailureReason.REQUIRES_JOINT_SIGNATURE, thrown.reason());
    }

    /** Svarar med det bolag testet skickar in, eller tomt när bolaget är null. */
    private static class StubClient implements CompanyValidationClient {

        private final CompanyValidationApiDTO company;

        StubClient(CompanyValidationApiDTO company) {
            this.company = company;
        }

        @Override
        public Optional<CompanyValidationApiDTO> lookup(String orgNumber) {
            return Optional.ofNullable(company);
        }

        @Override
        public Optional<CompanyFinancialApiDTO> fetchLatestAnnualReport(String orgNumber) {
            return Optional.empty();
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
