package se.comerit.resurs.dto.companyvalidation;

//DTO meant to mock the structure of company validation from external API
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import se.comerit.resurs.enums.SigningRight;

import java.time.Instant;
import java.util.List;

public record CompanyValidationApiDTO(
        @NotBlank String companyName,
        @NotBlank String orgNumber,
        @NotNull List<@Valid Signatory> signatories,
        Instant updatedAt
) {

    public record Signatory(
            @NotBlank String personalNumber,
            @NotBlank String name,
            String position,
            @NotNull SigningRight signingRight
    ){}
}