package se.comerit.resurs.dto.backoffice;

import se.comerit.resurs.enums.ApplicationStatus;

import java.math.BigDecimal;
import java.time.Instant;

public interface ReviewListElement {
    ApplicationStatus status();

    Long id();
    BigDecimal requestedAmount();
    Instant createdAt();
    String companyName();
    String purpose();
}
