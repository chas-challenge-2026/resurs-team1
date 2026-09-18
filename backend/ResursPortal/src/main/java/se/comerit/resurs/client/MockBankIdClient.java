package se.comerit.resurs.client;

import org.springframework.stereotype.Component;
import se.comerit.resurs.dto.bankid.BankIdVerificationResult;
import java.util.Map;
import java.util.Optional;

/**
 * MockBankIdClient -> mock-implementation av BankIdClient
 *
 * Ansvarar för: låtsas vara BankID under v2. Den slår upp ett personnummer mot en hårdkodad lista med godkända
 * identiteter och returnerar personens namn om det finns.
 *
 * Inte ansvarig för: att avgöra vad ett lyckat/misslyckat resultat betyder för inloggningen ->
 * det gör BankIdService/AuthService, som kan bytas ut mot en skarp implementation senare utan att AuthService ska
 * behöva ändras.
 *
 */
@Component
public class MockBankIdClient implements BankIdClient {
    private static final Map<String, String> APPROVED_IDENTITIES = Map.of(
            "196701011234", "Anders Karlsson",
            "196902024321", "Maria Svensson"
    );

    @Override
    public Optional<BankIdVerificationResult> verify(String personalNumber) {
        String name = APPROVED_IDENTITIES.get(personalNumber);
        return Optional.ofNullable(name)
                .map(n -> new BankIdVerificationResult(personalNumber, n));
    }

}
