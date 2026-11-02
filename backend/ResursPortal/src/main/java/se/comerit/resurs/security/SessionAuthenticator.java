package se.comerit.resurs.security;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class SessionAuthenticator {

    //Core authorities; det grundläggande som skiljer anställd mot kund.
    public static final String COMPANY = "ROLE_COMPANY";
    public static final String CASE_WORKER = "ROLE_CASE_WORKER";
//repo för en session
    private final SecurityContextRepository contextRepository =
            new HttpSessionSecurityContextRepository();

    public void authenticate(Object principal, Set<String> authorities,
                             HttpServletRequest request, HttpServletResponse response){
// skapar platshållare för data används som token
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                principal, null, authorities.stream().map(SimpleGrantedAuthority::new).toList());
//securitycontext hantera datan med authentication, securityucontextholder (threadlocal) hanterar att sessionen pekar på rätt thread om många använder samma metod samtidigt
        // eftersom att alla delar samma värde när någon ska kolla din token måste man hantera threadsen så man vet vilken thread det gäller.
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        contextRepository.saveContext(context, request, response);
    }
}
