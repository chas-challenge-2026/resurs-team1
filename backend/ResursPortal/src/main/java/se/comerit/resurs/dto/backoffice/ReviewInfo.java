package se.comerit.resurs.dto.backoffice;

import se.comerit.resurs.persistence.model.CreditApplication;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReviewInfo(
        long id,
        BigDecimal requested_amount,
        String purpose,
        LocalDateTime createdAt,
        String scoring_result,
        String decision_reason,
        String company_name,
        String org_number
) {
    public ReviewInfo(CreditApplication application) {
        this(application.getId(),
                application.getRequested_amount(),
                application.getPurpose(),
                application.getCreatedAt(),
                application.getScoring_result(),
                application.getDecision_reason(),
                application.getCompany().getCompany_name(),
                application.getCompany().getOrg_number());
    }
}
