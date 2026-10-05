package se.comerit.resurs.dto.backoffice;

import se.comerit.resurs.persistence.model.CreditApplication;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * ReviewInfo -> en rad i listan över ansökningar som väntar på granskning
 *
 * Innehåller det handläggaren behöver se i översikten, innan hen klickar in på en ansökan
 * för att se allt.
 *
 * Representerar bara en förenklad version av en ansökan -> all detaljerad info hämtas
 * separat via application_details().
 *
 */

public record ReviewInfo(
        long id,
        BigDecimal requestedAmount,
        String purpose,
        Instant createdAt,
        String scoringResult,
        String decisionReason,
        String companyName,
        String orgNumber
) {
    public ReviewInfo(CreditApplication application) {
        this(application.getId(),
                application.getRequestedAmount(),
                application.getPurpose(),
                application.getCreatedAt(),
                application.getScoringResult(),
                application.getDecisionReason(),
                application.getCompany().getCompany_name(),
                application.getCompany().getOrg_number());
    }
}
