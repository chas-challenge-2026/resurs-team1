package se.comerit.resurs.config;


import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.cache.autoconfigure.CacheProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.transaction.TransactionAwareCacheManagerProxy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@EnableCaching
@EnableConfigurationProperties(CacheProperties.class)
public class CacheConfig {


    @Bean
    public CacheManager cacheManager(CacheProperties properties) {
        CaffeineCacheManager caffeine = new CaffeineCacheManager();

        caffeine.setCacheSpecification(properties.getCaffeine().getSpec());
        caffeine.setCacheNames(properties.getCacheNames());

        caffeine.registerCustomCache("restrictedAccessApplications",
                Caffeine.newBuilder()
                        .maximumSize(1000)
                        .expireAfterWrite(Duration.ofMinutes(5))
                        .build());

        caffeine.registerCustomCache("certifiedAccessApplications",
                Caffeine.newBuilder()
                        .maximumSize(1000)
                        .expireAfterWrite(Duration.ofMinutes(5))
                        .build());

        return new TransactionAwareCacheManagerProxy(caffeine);
    }

}
