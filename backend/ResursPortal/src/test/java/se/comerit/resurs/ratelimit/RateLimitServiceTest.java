package se.comerit.resurs.ratelimit;

import io.github.bucket4j.ConsumptionProbe;
import org.junit.jupiter.api.Test;
import se.comerit.resurs.config.RateLimitProperties;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tester för RateLimitService.
 * <p>
 * Ingen Spring och ingen databas. Påfyllningen är satt till 10 minuter, så ingen hink hinner fyllas på
 * under ett test.
 */
class RateLimitServiceTest {

    private RateLimitService serviceWith(int capacity) {
        RateLimitProperties properties = new RateLimitProperties(true, capacity, capacity, Duration.ofMinutes(10));
        return new RateLimitService(properties, new BucketFactory(properties));
    }

    //Kontrollerar att inställningarna används: capacity anrop går igenom, sedan nekas nästa
    @Test
    void shouldAllowUpToCapacityAndThenBlock() {
        RateLimitService service = serviceWith(3);

        assertThat(service.consume("user").isConsumed()).isTrue();
        assertThat(service.consume("user").isConsumed()).isTrue();
        assertThat(service.consume("user").isConsumed()).isTrue();
        assertThat(service.consume("user").isConsumed()).isFalse();
    }

    //Kontrollerar att varje nyckel har sin egen hink, så att en användare inte tar slut på någon annans anrop
    @Test
    void shouldKeepSeparateBucketsPerKey() {
        RateLimitService service = serviceWith(1);

        assertThat(service.consume("a").isConsumed()).isTrue();
        assertThat(service.consume("a").isConsumed()).isFalse();

        assertThat(service.consume("b").isConsumed()).isTrue();
    }

    //Kontrollerar att svaret säger hur många anrop som är kvar, och hur länge man ska vänta när hinken är tom
    @Test
    void shouldReportRemainingTokensAndWaitTime() {
        RateLimitService service = serviceWith(2);

        ConsumptionProbe first = service.consume("user");
        service.consume("user");
        ConsumptionProbe blocked = service.consume("user");

        assertThat(first.getRemainingTokens()).isEqualTo(1);
        assertThat(blocked.isConsumed()).isFalse();
        assertThat(blocked.getNanosToWaitForRefill()).isPositive();
    }
}