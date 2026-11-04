package se.comerit.resurs.ratelimit;

import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import se.comerit.resurs.config.RateLimitProperties;
import se.comerit.resurs.exception.ApiError;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Instant;

/**
 * RateLimitFilter -> stoppar anrop när en användare har gjort för många
 *
 * Körs före varje anrop till /api/**. Frågar RateLimitKeyResolver vem anropet kommer fråm, frågar RateLimitService om
 * anropet får gå igenom, och svarar annars direkt med 429 Too Many Requests och en Retry-After-header.
 * Controllern kör aldrig när anropet nekas.
 *
 * Ansvarar för: att koppla ihop användaren med räknaren och skriva 429-svaret.
 *
 * Inte ansvarig för: att veta vem användaren är (RateLimitKeyResolvers jobb) eller att räkna anropen (RateLimitService jobb)
 *
 * OBS: filtret är en vanlig @Component, så Spring lägger in det som servlet-filter för alla anrop. Det körs efter
 * Spring Security, så det ser vem som är inloggad. Anrop som säkerheten redan avvisat (t.ex 401 utan inloggning)
 * når inte hit och räknas inte. 429-svaret skrivs för hand eftersom GlobalExceptionHandler inte når fel i ett filter.
 *
 */

@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private static final long NANOS_PER_SECOND = 1_000_000_000L;
    private final RateLimitService rateLimitService;
    private final RateLimitKeyResolver keyResolver;
    private final RateLimitProperties properties;
    private final ObjectMapper objectMapper;

    public RateLimitFilter(RateLimitService rateLimitService, RateLimitKeyResolver keyResolver,
                           RateLimitProperties properties, ObjectMapper objectMapper) {
        this.rateLimitService = rateLimitService;
        this.keyResolver = keyResolver;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    //Filtret hoppar över när rate limiting är avstängd, och allt utom API-anrop (så att sidor, bilder och swagger inte räknas)
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !properties.enabled() || !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        ConsumptionProbe probe = rateLimitService.consume(keyResolver.resolve(request));

        if (probe.isConsumed()) {
            response.setHeader("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));
            filterChain.doFilter(request, response);
            return;
        }
        //Avrundat uppåt minst 1 sekund
        long retryAfterSeconds = Math.max(1,
                (probe.getNanosToWaitForRefill() + NANOS_PER_SECOND - 1) / NANOS_PER_SECOND);

        ApiError body = new ApiError(
                Instant.now(),
                "TOO_MANY_REQUESTS",
                "Too many requests. Please try again in " + retryAfterSeconds + " seconds.",
                null
        );

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));
        response.setHeader("X-RateLimit-Remaining", "0");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
