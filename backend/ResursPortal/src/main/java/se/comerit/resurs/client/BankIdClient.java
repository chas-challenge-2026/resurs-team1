package se.comerit.resurs.client;

import se.comerit.resurs.dto.bankid.BankIdVerificationResult;

import java.util.Optional;

/**
 * Gränssnittet mot BankID
 *
 * Ansvarar för: kontraktet för att verifiera en persons identitet via personnummer, oavsett om det är
 * mocken eller en skarp integration i framtiden som ska svara.
 *
 * Inte ansvarig för: hur verifieringen faktiskt går till -> det görs utav implementationen
 * (MockBankIdClient / skarp klient senare)
 */
public interface BankIdClient {
    Optional<BankIdVerificationResult> verify(String personalNumber);
}
