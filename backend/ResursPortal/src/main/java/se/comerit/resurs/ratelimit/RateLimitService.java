package se.comerit.resurs.ratelimit;


import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import org.springframework.stereotype.Service;
import se.comerit.resurs.config.RateLimitProperties;

import java.time.Duration;

/**
 * RateLimitService -> håller reda på en hink per nyckel och tar ett anrop ur rätt hink
 *
 * Ansvarar för: att ge varje nyckel (användare eller IP-adress) sin egen hink, och att glömma bort hinkar
 * som ingen använt på ett tag så att minnet inte växer för alltid.
 *
 * Inte ansvarig för: hur hinken ser ut (BucketFactorys jobb) eller att svara med 429 (RateLimitFilters jobb).
 *
 * OBS: hinkarna ligger i applikationens minne. Redis (issue #284) byter ut lagringen här.
 */

@Service
public class RateLimitService {

    private final Cache<String, Bucket> buckets;
    private final BucketFactory bucketFactory;

    public RateLimitService(RateLimitProperties properties, BucketFactory bucketFactory) {
        this.bucketFactory = bucketFactory;

        //Tiden det tar att fylla en tom hink helt. Har en hink legat oanvänd så länge är den identisk med en ny,
        // så den kan glömmas bort utan att någon märker det.
        Duration fullRefillTime = properties.refillPeriod()
                .multipliedBy(properties.capacity())
                .dividedBy(properties.refillTokens());

        this.buckets = Caffeine.newBuilder()
                .expireAfterAccess(fullRefillTime)
                .build();
    }

    //Tar ett anrop ur nyckelns hink. Svaret säger om det gick, hur många som är kvar och hur länge man måste vänta
    public ConsumptionProbe consume(String key) {
        Bucket bucket = buckets.get(key, k -> bucketFactory.create());
        return bucket.tryConsumeAndReturnRemaining(1);
    }
}
