package se.comerit.resurs.dto.application;

import se.comerit.resurs.enums.ApplicationStatus;
import se.comerit.resurs.persistence.model.CreditApplication;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record NewApplicationDTO(
        BigDecimal requested_amount,
        String purpose,
        ApplicationStatus status,
        String decision,
        String decision_reason,
        String scoring_result,
        String company_name,
        String org_number,
        String authorized_signatory,
        int flagCount

) {
}