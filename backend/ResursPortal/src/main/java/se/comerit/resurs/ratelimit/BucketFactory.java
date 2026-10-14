package se.comerit.resurs.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Component;
import se.comerit.resurs.config.RateLimitProperties;

/**
 * BucketFactory -> bygger en ny hink (Bucket) med de gränser som står i application.properties
 *
 * Ansvarar för: hur en hink ser ut (hur många anrop den rymmer och hur snabbt den fylls på).
 *
 * Inte ansvarig för: att komma ihåg vilken hink som hör till vilken användare (RateLimitService).
 */
@Component
public class BucketFactory {

    private final RateLimitProperties properties;
    public BucketFactory(RateLimitProperties properties) {
        this.properties = properties;
    }

    public Bucket create() {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(properties.capacity())
                        .refillGreedy(properties.refillTokens(), properties.refillPeriod())
                        .build())
                .build();
    }
}
