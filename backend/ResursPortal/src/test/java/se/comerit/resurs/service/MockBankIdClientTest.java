package se.comerit.resurs.service;

import org.junit.jupiter.api.Test;
import se.comerit.resurs.client.MockBankIdClient;
import se.comerit.resurs.dto.bankid.BankIdVerificationResult;

import static org.junit.jupiter.api.Assertions.*;

public class MockBankIdClientTest {
    private final MockBankIdClient mockBankIdClient = new MockBankIdClient();

    @Test
    void verify_shouldReturnApproved_whenIdentityIsFirstApprovedNumber() {
        BankIdVerificationResult result = mockBankIdClient.verify("196701011234");

        assertTrue(result.approved());
        assertEquals("Anders Karlsson", result.name());
    }

    @Test
    void verify_shouldReturnApproved_whenIndentityIsSecondApprovedNumber() {
        BankIdVerificationResult result = mockBankIdClient.verify("196902024321");

        assertTrue(result.approved());
        assertEquals("Maria Svensson", result.name());
    }

    @Test
    void verify_shouldReturnNotApproved_whenIdentityIsUnknown() {
        BankIdVerificationResult result = mockBankIdClient.verify("197101011234");

        assertFalse(result.approved());
        assertNull(result.name());
    }

    @Test
    void verify_shouldReturnNotApproved_whenIdentityIsEmpty() {
        BankIdVerificationResult result = mockBankIdClient.verify("");
        assertFalse(result.approved());
    }
}
