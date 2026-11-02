package se.comerit.resurs;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;

public class CircuitBreakerResetListener implements TestExecutionListener {
    @Override
    public void beforeTestMethod(TestContext testContext) {
        testContext.getApplicationContext()
                .getBeanProvider(CircuitBreakerRegistry.class)
                .ifAvailable(registry -> registry.getAllCircuitBreakers().forEach(CircuitBreaker::reset));
    }
}

