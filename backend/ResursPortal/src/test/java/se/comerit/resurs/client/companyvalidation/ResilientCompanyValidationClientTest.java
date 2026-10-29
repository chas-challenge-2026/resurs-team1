package se.comerit.resurs.client.companyvalidation;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.springboot.circuitbreaker.autoconfigure.CircuitBreakerAutoConfiguration;
import io.github.resilience4j.springboot.retry.autoconfigure.RetryAutoConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import se.comerit.resurs.exception.companyvalidation.CompanyRegistryUnavailableException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(
        classes = {ResilientCompanyValidationClient.class, MockCompanyValidationClient.class},
        properties = {
                "resilience4j.retry.instances.company-lookup.wait-duration=10ms",
                "resilience4j.retry.instances.annual-report.wait-duration=10ms"
        })
@ImportAutoConfiguration({AopAutoConfiguration.class, CircuitBreakerAutoConfiguration.class, RetryAutoConfiguration.class})
class ResilientCompanyValidationClientTest {

    private static final String AVAILABLE_ORG_NUMBER = "556000-1234";
    private static final String REGISTRY_DOWN_ORG_NUMBER = "556000-5555";
    private static final String UNKNOWN_ORG_NUMBER = "556000-0000";
    private static final int MINIMUM_CALLS_TO_OPEN = 5;

    @Autowired
    private CompanyValidationClient client;

    @MockitoSpyBean(name = "rawCompanyValidationClient")
    private CompanyValidationClient delegate;

    @BeforeEach
    void clearDelegateInvocations() {
        clearInvocations(delegate);
    }

    @Test
    void lookup_whenRegistryAvailable_returnsCompanyWithoutRetry() {
        assertThat(client.lookup(AVAILABLE_ORG_NUMBER)).isPresent();

        verify(delegate, times(1)).lookup(AVAILABLE_ORG_NUMBER);
    }

    @Test
    void lookup_whenRegistryDown_retriesThreeTimesThenThrows() {
        assertThatThrownBy(() -> client.lookup(REGISTRY_DOWN_ORG_NUMBER))
                .isInstanceOf(CompanyRegistryUnavailableException.class);

        verify(delegate, times(3)).lookup(REGISTRY_DOWN_ORG_NUMBER);
    }

    @Test
    void lookup_whenCompanyNotFound_doesNotRetry() {
        assertThat(client.lookup(UNKNOWN_ORG_NUMBER)).isEmpty();

        verify(delegate, times(1)).lookup(UNKNOWN_ORG_NUMBER);
    }

    @Test
    void lookup_whenCircuitOpen_failsFastWithoutCallingRegistry() {
        openLookupCircuit();

        assertThatThrownBy(() -> client.lookup(AVAILABLE_ORG_NUMBER))
                .isInstanceOf(CompanyRegistryUnavailableException.class)
                .hasCauseInstanceOf(CallNotPermittedException.class);

        verify(delegate, never()).lookup(anyString());
    }

    @Test
    void fetchLatestAnnualReport_whenLookupCircuitOpen_stillCallsRegistry() {
        openLookupCircuit();

        assertThat(client.fetchLatestAnnualReport(AVAILABLE_ORG_NUMBER)).isPresent();

        verify(delegate, times(1)).fetchLatestAnnualReport(AVAILABLE_ORG_NUMBER);
    }

    private void openLookupCircuit() {
        for (int i = 0; i < MINIMUM_CALLS_TO_OPEN; i++) {
            assertThatThrownBy(() -> client.lookup(REGISTRY_DOWN_ORG_NUMBER))
                    .isInstanceOf(CompanyRegistryUnavailableException.class);
        }
        clearInvocations(delegate);
    }
}
