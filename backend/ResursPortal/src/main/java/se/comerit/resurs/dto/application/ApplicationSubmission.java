package se.comerit.resurs.dto.application;

import se.comerit.resurs.dto.ContactDetails;

import java.math.BigDecimal;

public record ApplicationSubmission(
        String orgNumber,
        BigDecimal requestedAmount,
        String purpose,
        Integer durationMonths,
        ContactDetails contactDetails

) {
}
