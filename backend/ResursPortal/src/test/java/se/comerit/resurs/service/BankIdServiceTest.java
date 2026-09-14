package se.comerit.resurs.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BankIdServiceTest {
    private final BankIdService bankIdService = new BankIdService();

    @Test
    void isApproved_shouldReturnTrue_whenOrgNumberIsFirstApprovedNumber() {
        boolean result = bankIdService.isApproved("556000-1234");
        assertTrue(result);
    }
    @Test
    void isApproved_shouldReturnTrue_whenOrgNumberIsSecondApprovedNumber() {
        boolean result = bankIdService.isApproved("556000-5678");

        assertTrue(result);
    }

    @Test
    void isApproved_shouldReturnFalse_whenOrgNumberIsNotApproved() {
        boolean result = bankIdService.isApproved("556000-9999");

        assertFalse(result);
    }

    @Test
    void isApproved_shouldReturnFalse_whenOrgNumberIsEmpty() {
        boolean result = bankIdService.isApproved("");

        assertFalse(result);
    }
}
