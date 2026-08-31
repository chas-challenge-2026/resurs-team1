package se.comerit.resurs.dto;

import se.comerit.resurs.enums.ApplicationStatus;
import se.comerit.resurs.persistence.model.CreditApplication;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreditApplicationDetails(
        Long id,
        BigDecimal requested_amount,
        String purpose,
        ApplicationStatus status,
        String decision,
        String decision_reason,
        String scoring_result,
        String audit_log,
        LocalDateTime created_at,
        LocalDateTime updated_at,
        String company_name,
        String org_number,
        String authorized_signatory

) {
    public CreditApplicationDetails(CreditApplication app) {
        this(app.getId(), app.getRequested_amount(), app.getPurpose(), app.getStatus(), app.getDecision(), app.getDecision_reason(), app.getScoring_result(), app.getAudit_log(), app.getCreated_at(), app.getUpdated_at(), app.getCompany().getCompany_name(), app.getCompany().getOrg_number(), app.getCompany().getAuthorized_signatory());
    }
}