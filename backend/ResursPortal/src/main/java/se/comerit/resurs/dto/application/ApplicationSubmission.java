package se.comerit.resurs.dto.application;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import se.comerit.resurs.dto.ContactDetails;

import java.math.BigDecimal;

public record ApplicationSubmission(
        @NotBlank String orgNumber,
        @NotNull @Positive BigDecimal requestedAmount,
        @NotBlank String purpose,
        @NotNull @Positive Integer durationMonths,
        @NotNull @Valid ContactDetails contactDetails

) {
}
