package se.comerit.resurs.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import java.time.Duration;

/**
 * RateLimitProperties -> inställningarna för rate limiting, läses från "rate-limit.*" i application properties
 *
 *  Ansvarar för: hur många anrop en användare får göra (capacity) och hur snabbt de fylls på igen
 *  (refillTokens per refillPeriod), samt en av/på knapp (enabled)
 *
 *  Inte ansvarig för: att faktiskt räkna anrop eller stoppa dem -> det gör RateLimitService och RateLimitFilter
 *
 *  Exempel: capacity=60, refillTokens=60, refillPeriod=1m betyder högst 60 anrop på en gång,
 *  och sedan i snitt 60 per minut (1 per sekund)
 */

@ConfigurationProperties(prefix = "rate-limit")
public record RateLimitProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("60") int capacity,
        @DefaultValue("60") int refillTokens,
        @DefaultValue("1m") Duration refillPeriod
) {
}
