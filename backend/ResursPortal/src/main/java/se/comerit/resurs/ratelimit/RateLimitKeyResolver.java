package se.comerit.resurs.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import se.comerit.resurs.security.CaseWorkerPrincipal;
import se.comerit.resurs.security.CompanyPrincipal;

/**
 * RateLimitKeyResolver -> bestämmer vilken "hink" ett anrop ska räknas mot
 *
 * Ansvarar för: att ge varje användare sin egen nyckel ->
 * Inloggad handläggare får "caseworker:id",
 * inloggat företag får "company:orgnummer:personnummer",
 * den som inte är inloggad räknas på IP-adress "ip:adress"
 *
 * Inte ansvarig för att räkna anropen det gör RateLimitService eller att stoppa dem, det är RateLimitFilters uppgift
 *
 * OBS: nyckeln innehåller ett personnummer för företagsanvändare, den ligger bara i minnet nu,
 * men behövs bytas till en nyckel utan personnummer innan hinkarna flyttas till Redis (Issue #284)
 *
 */
@Component
public class RateLimitKeyResolver {
    public String resolve(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        boolean loggedIn = authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);

        if (loggedIn) {
            return switch (authentication.getPrincipal()) {
                case CaseWorkerPrincipal worker -> "caseworker:" + worker.id();
                //TODO: CompanyPrincipal is subject to change to using HmacHash,
                // this is a temporary solution while we wait for the merge from the correct branch
                case CompanyPrincipal company -> "company:" + company.orgNumber() + ":" + company.personalNumber();
                default -> "user:" + authentication.getName();
            };
        }
        //Bakom en proxy behövs server.forward-headers-strategy=native, annars ser alla besökare ut att komma från
        // proxyns adress.
        return "ip:" + request.getRemoteAddr();
    }
}
