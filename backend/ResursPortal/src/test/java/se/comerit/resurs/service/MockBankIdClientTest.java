package se.comerit.resurs.service;

import org.junit.jupiter.api.Test;
import se.comerit.resurs.client.MockBankIdClient;
import se.comerit.resurs.dto.bankid.BankIdVerificationResult;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class MockBankIdClientTest {
    private final MockBankIdClient mockBankIdClient = new MockBankIdClient();

    @Test
    void verify_shouldReturnApproved_whenIdentityIsFirstApprovedNumber() {
        Optional<BankIdVerificationResult> result = mockBankIdClient.verify("197503121234");

        assertTrue(result.isPresent());
        assertEquals("Anders Karlsson", result.get().name());
    }

    @Test
    void verify_shouldReturnApproved_whenIndentityIsSecondApprovedNumber() {
        Optional<BankIdVerificationResult> result = mockBankIdClient.verify("198206245678");

        assertTrue(result.isPresent());
        assertEquals("Maria Svensson", result.get().name());
    }

    @Test
    void verify_shouldReturnNotApproved_whenIdentityIsUnknown() {
        Optional<BankIdVerificationResult> result = mockBankIdClient.verify("197101011234");

        assertFalse(result.isPresent());
    }

    @Test
    void verify_shouldReturnNotApproved_whenIdentityIsEmpty() {
        Optional<BankIdVerificationResult> result = mockBankIdClient.verify("");
        assertFalse(result.isPresent());
    }
}
