package se.comerit.resurs.service;

import org.springframework.stereotype.Service;
import se.comerit.resurs.client.BankIdClient;
import se.comerit.resurs.dto.bankid.BankIdVerificationResult;
import se.comerit.resurs.exception.bankid.BankIdVerificationFailedException;

/**
 * BankIdService -> det som AuthService pratar med för BankID-verifieringen
 *
 * Ansvarar för: vidarebefordra anrop till BankIdClient och kastar BankIdVerificationFailedException om personen inte
 * kunde verifieras.
 *
 * Inte ansvarig för: om mocken eller en skarp klient svarar, -> den känner bara till BankIdClient-gränssnittet.
 *
 */
@Service
public class BankIdService {
    private final BankIdClient bankIdClient;

    public BankIdService(BankIdClient bankIdClient) {
        this.bankIdClient = bankIdClient;
    }

    public BankIdVerificationResult verify(String personalNumber) {
        return bankIdClient.verify(personalNumber)
                .orElseThrow(() -> new BankIdVerificationFailedException(personalNumber));
    }
}
