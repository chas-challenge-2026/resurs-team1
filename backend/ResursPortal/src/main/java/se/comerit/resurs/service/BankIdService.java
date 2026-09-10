package se.comerit.resurs.service;

import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class BankIdService implements ValidationInterface {
    private static final Set<String> APPROVED_ORG_NUMBERS = Set.of("556000-1234", "556000-5678");

    @Override
    public boolean isApproved(String orgNumber) {
        return APPROVED_ORG_NUMBERS.contains(orgNumber);
    }
}
