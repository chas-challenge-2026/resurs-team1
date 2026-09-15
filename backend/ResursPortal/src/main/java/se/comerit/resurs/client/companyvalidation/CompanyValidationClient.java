package se.comerit.resurs.client.companyvalidation;

import se.comerit.resurs.dto.CompanyValidationApiDTO;

import java.util.Optional;

public interface CompanyValidationClient {
    Optional <CompanyValidationApiDTO> lookup(String orgNumber);
}
