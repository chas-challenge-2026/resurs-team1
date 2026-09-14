package se.comerit.resurs.dto.status;

import se.comerit.resurs.enums.ApplicationStatus;
import se.comerit.resurs.persistence.model.CreditApplication;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ApplicationStatusDetails(
        Long id,
        BigDecimal requestedAmount,
        String purpose,
        ApplicationStatus status,
        String decision,
        String decisionReason,
        String scoringResult,
        String auditLog,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String companyName,
        String orgNumber

) {
    public ApplicationStatusDetails(CreditApplication app) {
        this(app.getId(), app.getRequestedAmount(), app.getPurpose(), app.getStatus(), app.getDecision(), app.getDecisionReason(), app.getScoringResult(), app.getAuditLog(), app.getCreatedAt(), app.getUpdatedAt(), app.getCompany().getCompany_name(), app.getCompany().getOrg_number());
    }
}