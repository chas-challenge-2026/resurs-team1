package se.comerit.resurs.dto;

import se.comerit.resurs.enums.ApplicationStatus;
import se.comerit.resurs.persistence.model.CreditApplication;

import java.math.BigDecimal;
import java.time.Instant;

public record CreditApplicationDTO(
        Long id,
        BigDecimal requestedAmount,
        String purpose,
        ApplicationStatus status,
        String decision,
        String decisionReason,
        String scoringResult,
        Integer durationMonths,
        Instant createdAt,
        Instant updatedAt,
        String companyName,
        String orgNumber,
        String authorizedSignatory,
        ContactDetails contactDetails,
        String comment

) {
    public CreditApplicationDTO(CreditApplication app) {
        this(app.getId(), app.getRequestedAmount(), app.getPurpose(), app.getStatus(), app.getDecision(), app.getDecisionReason(), app.getScoringResult(),app.getDurationMonths() , app.getCreatedAt(), app.getUpdatedAt(), app.getCompany().getCompany_name(), app.getCompany().getOrg_number(), app.getCompany().getAuthorized_signatory(),new ContactDetails(app.getContactName(), app.getContactEmail(), app.getContactNumber()), app.getComment());
    }
}