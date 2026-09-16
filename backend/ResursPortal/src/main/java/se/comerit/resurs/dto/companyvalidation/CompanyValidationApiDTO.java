package se.comerit.resurs.dto.companyvalidation;

//DTO meant to mock the structure of company validation from external API
import se.comerit.resurs.enums.SigningRight;

import java.time.Instant;
import java.util.List;

public record CompanyValidationApiDTO(
        String orgNumber,
        List<Signatory> signatories,
        Instant updatedAt
) {

    public record Signatory(
            String personalNumber,
            String name,
            String position,
            SigningRight signingRight
    ){}
}