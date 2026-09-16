package se.comerit.resurs.client.companyvalidation;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import se.comerit.resurs.dto.companyvalidation.CompanyValidationApiDTO;
import se.comerit.resurs.dto.companyvalidation.CompanyValidationApiDTO.Signatory;
import se.comerit.resurs.enums.SigningRight;
import se.comerit.resurs.exception.companyvalidation.CompanyRegistryUnavailableException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
@Component
@ConditionalOnProperty(prefix = "company-validation", name = "client", havingValue = "mock")
class MockCompanyValidationClient implements CompanyValidationClient {

    private static final String UNAVAILABLE_ORG_NUMBER = "556000-5555";
    private static final Instant UPDATED_AT = Instant.parse("2026-08-14T10:06:04Z");

    private static final Map<String, CompanyValidationApiDTO> REGISTRY = Map.of(
            "556000-1234", new CompanyValidationApiDTO("556000-1234", List.of(
                    new Signatory("197503121234", "Anders Karlsson", "Styrelseordförande", SigningRight.ALONE)
            ), UPDATED_AT),

            "556000-5678", new CompanyValidationApiDTO("556000-5678", List.of(
                    new Signatory("198206245678", "Maria Svensson", "Styrelseordförande", SigningRight.ALONE),
                    new Signatory("197503121234", "Anders Karlsson", "Styrelseledamot", SigningRight.ALONE)
            ), UPDATED_AT),

            "556000-7777", new CompanyValidationApiDTO("556000-7777", List.of(
                    new Signatory("196609307777", "Johan Berg", "Styrelseledamot", SigningRight.JOINTLY),
                    new Signatory("197102147777", "Eva Berg", "Styrelseledamot", SigningRight.JOINTLY)
            ), UPDATED_AT),

            "556000-9999", new CompanyValidationApiDTO("556000-9999", List.of(
                    new Signatory("196811059999", "Erik Lindqvist", "Styrelseordförande", SigningRight.ALONE)
            ), UPDATED_AT)
    );
    @Override
    public Optional<CompanyValidationApiDTO> lookup(String orgNumber) {
        if (UNAVAILABLE_ORG_NUMBER.equals(orgNumber)) {
            throw new CompanyRegistryUnavailableException("company-registry", orgNumber, null);
        }
        return Optional.ofNullable(REGISTRY.get(orgNumber));
    }
}
