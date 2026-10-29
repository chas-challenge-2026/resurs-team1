package se.comerit.resurs.client.spar;

import org.junit.jupiter.api.Test;
import se.comerit.resurs.dto.spar.SparPersonDTO;
import se.comerit.resurs.exception.spar.SparUnavailableException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MockSparClientTest {

    private final MockSparClient client = new MockSparClient();

    @Test
    void lookup_whenProtectedPerson_returnsProtectedIdentity() {
        Optional<SparPersonDTO> result = client.lookup("710214-7777");

        assertThat(result).isPresent();
        assertThat(result.get().protectedIdentity()).isTrue();
    }

    @Test
    void lookup_whenRegularPerson_returnsUnprotectedIdentity() {
        Optional<SparPersonDTO> result = client.lookup("750312-1234");

        assertThat(result).isPresent();
        assertThat(result.get().protectedIdentity()).isFalse();
    }

    @Test
    void lookup_whenUnknownPerson_returnsEmpty() {
        assertThat(client.lookup("000000-0000")).isEmpty();
    }

    @Test
    void lookup_whenSparUnavailable_throws() {
        assertThatThrownBy(() -> client.lookup(MockSparData.UNAVAILABLE_PERSONAL_NUMBER))
                .isInstanceOf(SparUnavailableException.class);
    }
}
