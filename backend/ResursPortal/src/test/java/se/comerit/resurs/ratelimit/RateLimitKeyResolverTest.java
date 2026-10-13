package se.comerit.resurs.ratelimit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import se.comerit.resurs.security.CaseWorkerPrincipal;
import se.comerit.resurs.security.CompanyPrincipal;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tester för RateLimitKeyResolver.
 *
 * Ingen Spring och ingen databas. Testerna sätter den inloggade användaren direkt i SecurityContextHolder
 * och rensar den efteråt, så att de inte påverkar varandra.
 */
class RateLimitKeyResolverTest {

    private final RateLimitKeyResolver resolver = new RateLimitKeyResolver();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void loginAs(Authentication authentication) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }

    private MockHttpServletRequest requestFrom(String ip) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(ip);
        return request;
    }

    //En inloggad handläggare får en nyckel baserad på sitt id
    @Test
    void shouldUseIdForLoggedInCaseWorker() {
        CaseWorkerPrincipal worker = new CaseWorkerPrincipal(5L, "Karin", "karin@resurs.se");
        loginAs(new UsernamePasswordAuthenticationToken(worker, null,
                List.of(new SimpleGrantedAuthority("ROLE_CASE_WORKER"))));

        assertThat(resolver.resolve(requestFrom("203.0.113.5"))).isEqualTo("caseworker:5");
    }

    //Ett inloggat företag får en nyckel baserad på orgnummer och person
    @Test
    void shouldUseOrgNumberAndPersonForLoggedInCompany() {
        CompanyPrincipal company = new CompanyPrincipal("556000-1234", "Fasen Elteknik AB", "750312-1234");
        loginAs(new UsernamePasswordAuthenticationToken(company, null,
                List.of(new SimpleGrantedAuthority("ROLE_COMPANY"))));

        assertThat(resolver.resolve(requestFrom("203.0.113.5"))).isEqualTo("company:556000-1234:750312-1234");
    }

    //Den som inte är inloggad räknas på IP-adress
    @Test
    void shouldUseIpAddressWhenNobodyIsLoggedIn() {
        assertThat(resolver.resolve(requestFrom("203.0.113.5"))).isEqualTo("ip:203.0.113.5");
    }

    //Spring Security ger en "anonym" användare på öppna sidor. Den räknas också på IP-adress
    @Test
    void shouldUseIpAddressForAnonymousUser() {
        loginAs(new AnonymousAuthenticationToken("key", "anonymousUser",
                List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

        assertThat(resolver.resolve(requestFrom("203.0.113.5"))).isEqualTo("ip:203.0.113.5");
    }

    //En annan sorts inloggad användare får en nyckel baserad på sitt namn
    @Test
    void shouldFallBackToNameForOtherKindsOfUsers() {
        loginAs(new UsernamePasswordAuthenticationToken("someone", null,
                List.of(new SimpleGrantedAuthority("ROLE_OTHER"))));

        assertThat(resolver.resolve(requestFrom("203.0.113.5"))).isEqualTo("user:someone");
    }
}