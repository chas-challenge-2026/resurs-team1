package se.comerit.resurs.dto.application;

import se.comerit.resurs.dto.CreditApplicationDTO;
import se.comerit.resurs.enums.ApplicationStatus;
import se.comerit.resurs.persistence.model.CreditApplication;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ApplicationShortDTO (
        Long id,
        BigDecimal requestedAmount,
        String purpose,
        ApplicationStatus status,
        String decision,
        LocalDateTime createdAt
){
    public ApplicationShortDTO(CreditApplication app) {
        this(app.getId(), app.getRequestedAmount(), app.getPurpose(), app.getStatus(), app.getDecision(),app.getCreatedAt());
    }
    public ApplicationShortDTO(CreditApplicationDTO appDTO){
        this(appDTO.id(),appDTO.requestedAmount(),appDTO.purpose(),appDTO.status(),appDTO.decision(),appDTO.createdAt());
    }

}
