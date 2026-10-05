package se.comerit.resurs.dto.backoffice;

import se.comerit.resurs.persistence.model.CreditApplication;

import java.math.BigDecimal;
import java.time.Instant;

public record HistoricalReviewInfo(
        long id,
        BigDecimal requestedAmount,
        String purpose,
        String decision,
        Instant createdAt,
        Instant updatedAt,
        String companyName,
        String orgNumber
) {
    public HistoricalReviewInfo(CreditApplication application) {
        this(application.getId(),
                application.getRequestedAmount(),
                application.getPurpose(),
                application.getDecision(),
                application.getCreatedAt(),
                application.getUpdatedAt(),
                application.getCompany().getCompany_name(),
                application.getCompany().getOrg_number());
    }
}
