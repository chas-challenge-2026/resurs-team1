package se.comerit.resurs.exception.bankid;

/**
 * BankIdClientException -> tekniskt fel i själva BankID-anropet
 *
 * Denna exception kastas när klienten (mock eller skarp integration) inte kan utföra sin uppgift,
 * t.ex nätverksfel mot en skarp integration senare.
 *
 * Inte samma sak som BankIdVerificationFailedException, som betyder att anropet lyckas men personen inte godkändes.
 *
 */

public class BankIdClientException extends RuntimeException {
    public BankIdClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
