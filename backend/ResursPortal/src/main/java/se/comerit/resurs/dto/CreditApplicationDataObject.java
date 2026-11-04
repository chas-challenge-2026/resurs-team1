package se.comerit.resurs.dto;

import se.comerit.resurs.enums.ApplicationStatus;

import java.math.BigDecimal;
import java.time.Instant;

public interface CreditApplicationDataObject {
    Long id();
    BigDecimal requestedAmount();
    String purpose();
    ApplicationStatus status();
    String decision();
    String decisionReason();
    String scoringResult();
    Integer durationMonths();
    Instant createdAt();
    Instant updatedAt();
    String companyName();
    String orgNumber();
    String authorizedSignatory();
    ContactDetails contactDetails();
    String comment();

}
