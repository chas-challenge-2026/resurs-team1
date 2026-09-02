package se.comerit.resurs.dto.backoffice;

import se.comerit.resurs.persistence.model.CreditApplication;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record HistoricalReviewInfo(
        long id,
        BigDecimal requested_amount,
        String purpose,
        String decision,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String company_name,
        String org_number
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
