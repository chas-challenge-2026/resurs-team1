package se.comerit.resurs.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.ResourceAccessException;
import se.comerit.resurs.client.spar.SparClient;
import se.comerit.resurs.dto.spar.SparPersonDTO;
import se.comerit.resurs.exception.spar.SparUnavailableException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SparServiceTest {

    private static final String PERSONAL_NUMBER = "750312-1234";

    @Mock
    private SparClient sparClient;

    @InjectMocks
    private SparService sparService;

    @Test
    void hasProtectedIdentity_whenPersonIsProtected_returnsTrue() {
        when(sparClient.lookup(PERSONAL_NUMBER)).thenReturn(Optional.of(new SparPersonDTO(PERSONAL_NUMBER, true)));

        assertThat(sparService.hasProtectedIdentity(PERSONAL_NUMBER)).isTrue();
    }

    @Test
    void hasProtectedIdentity_whenPersonIsNotProtected_returnsFalse() {
        when(sparClient.lookup(PERSONAL_NUMBER)).thenReturn(Optional.of(new SparPersonDTO(PERSONAL_NUMBER, false)));

        assertThat(sparService.hasProtectedIdentity(PERSONAL_NUMBER)).isFalse();
    }

    @Test
    void hasProtectedIdentity_whenPersonNotFound_returnsTrue() {
        when(sparClient.lookup(PERSONAL_NUMBER)).thenReturn(Optional.empty());

        assertThat(sparService.hasProtectedIdentity(PERSONAL_NUMBER)).isTrue();
    }

    @Test
    void hasProtectedIdentity_whenSparUnavailable_throws() {
        when(sparClient.lookup(PERSONAL_NUMBER))
                .thenThrow(new SparUnavailableException(new ResourceAccessException("timeout")));

        assertThatThrownBy(() -> sparService.hasProtectedIdentity(PERSONAL_NUMBER))
                .isInstanceOf(SparUnavailableException.class);
    }
}
