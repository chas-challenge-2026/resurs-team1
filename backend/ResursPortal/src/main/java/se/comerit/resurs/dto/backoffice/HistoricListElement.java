package se.comerit.resurs.dto.backoffice;

import java.math.BigDecimal;
import java.time.Instant;

public interface HistoricListElement {
    Long id();
    BigDecimal requestedAmount();

    Instant createdAt();
    Instant updatedAt();

    String companyName();
    String purpose();
    String decision();

}
