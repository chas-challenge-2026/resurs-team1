package se.comerit.resurs.client.spar;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import se.comerit.resurs.dto.spar.SparPersonDTO;
import se.comerit.resurs.exception.spar.SparUnavailableException;

import java.util.Optional;

import static se.comerit.resurs.client.spar.MockSparData.REGISTRY;
import static se.comerit.resurs.client.spar.MockSparData.UNAVAILABLE_PERSONAL_NUMBER;

@Component
@ConditionalOnProperty(prefix = "spar", name = "client", havingValue = "mock")
class MockSparClient implements SparClient {

    @Override
    public Optional<SparPersonDTO> lookup(String personalNumber) {
        if (UNAVAILABLE_PERSONAL_NUMBER.equals(personalNumber)) {
            throw new SparUnavailableException(new ResourceAccessException("Simulated SPAR timeout"));
        }
        return Optional.ofNullable(REGISTRY.get(personalNumber));
    }
}