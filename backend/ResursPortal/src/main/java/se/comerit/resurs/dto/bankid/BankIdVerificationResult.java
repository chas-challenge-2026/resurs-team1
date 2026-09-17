package se.comerit.resurs.dto.bankid;

/**
 * BankIdVerificationResult -> resultatet av en lyckad BankID-verifiering
 *
 * Innehåller personnumret som verifierades samt namnet som BankID intygar hör till just det personnumret.
 *
 * Representerar endast ett lyckat resultat -> misslyckat kastas som BankIdVerificationFailedException
 *
 */

public record BankIdVerificationResult(String personalNumber, String name) {
}
