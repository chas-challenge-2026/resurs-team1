package se.comerit.resurs.dto;

import io.swagger.v3.oas.models.info.Contact;
import se.comerit.resurs.enums.ApplicationStatus;
import se.comerit.resurs.persistence.model.CreditApplication;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreditApplicationDTO(
        Long id,
        BigDecimal requestedAmount,
        String purpose,
        ApplicationStatus status,
        String decision,
        String decisionReason,
        String scoringResult,
        Integer durationMonths,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String companyName,
        String orgNumber,
        String authorizedSignatory,
        ContactDetails contactDetails

) {
    public CreditApplicationDTO(CreditApplication app) {
        this(app.getId(), app.getRequestedAmount(), app.getPurpose(), app.getStatus(), app.getDecision(), app.getDecisionReason(), app.getScoringResult(),app.getDurationMonths() , app.getCreatedAt(), app.getUpdatedAt(), app.getCompany().getCompany_name(), app.getCompany().getOrg_number(), app.getCompany().getAuthorized_signatory(),new ContactDetails(app.getContactName(), app.getContactEmail(), app.getContactNumber()));
    }
}