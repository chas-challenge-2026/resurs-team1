package se.comerit.resurs.exception.bankid;


/**
 * BankIdVerificationFailedException -> personen kunde inte verifieras av BankID
 *
 * Denna exception kastas av BankIdService när ett personnummer inte finns bland godkända identiteter.
 *
 */
public class BankIdVerificationFailedException extends RuntimeException {
    public BankIdVerificationFailedException(String personalNumber) {
        super("BankID could not verify identity with this personal number: " + personalNumber);
    }
}
