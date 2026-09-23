package se.comerit.resurs.dto.application;

import java.math.BigDecimal;

public record ApplicationSubmission(
        String orgNumber,
        BigDecimal requestedAmount,

        String contactName,
        String contactPhone,
        String contactEmail,

        String purpose,
        Integer durationMonths

) {
}
