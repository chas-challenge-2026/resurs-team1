package se.comerit.resurs.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.*;

import se.comerit.resurs.dto.auth.*;
import se.comerit.resurs.security.CaseWorkerPrincipal;
import se.comerit.resurs.security.CompanyPrincipal;
import se.comerit.resurs.security.SessionAuthenticator;
import se.comerit.resurs.service.AuthService;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;



@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final SessionAuthenticator sessionAuthenticator;
    private final HttpServletRequest httpServletRequest;


    public AuthController(AuthService authService, SessionAuthenticator sessionAuthenticator, HttpServletRequest httpServletRequest) {
        this.authService = authService;
        this.sessionAuthenticator = sessionAuthenticator;
        this.httpServletRequest = httpServletRequest;

    }

    @GetMapping("/me")
    public ResponseEntity<CurrentUserResponse>me(Authentication authentication){

        //separate roles from the authorities
        Set<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring(5))
                .collect(Collectors.toSet());


        if (authentication.getPrincipal() instanceof CaseWorkerPrincipal worker) {
            return ResponseEntity.ok(new CurrentUserResponse(
                    worker.id(), roles, worker.name(), null));
        }

        if (authentication.getPrincipal() instanceof CompanyPrincipal company) {
            return ResponseEntity.ok(new CurrentUserResponse(
                    null, roles, company.companyName(), company.orgNumber()));
        }

        throw new IllegalStateException(
                "Unknown principal type: " + authentication.getPrincipal().getClass());
    }


        //session.setattribute tas bort efter spring security har filter
        @PostMapping("/login/company")
    public ResponseEntity<CompanyLoginResponse>loginCompany(
                @Valid @RequestBody CompanyLoginRequest request, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse
                ){
            CompanyLoginResponse response = authService.loginCompany(request.orgNumber(), request.personalNumber());
            CompanyPrincipal principal = new CompanyPrincipal(response.orgNumber(), response.companyName(), request.personalNumber());
            sessionAuthenticator.authenticate(principal, Set.of(SessionAuthenticator.COMPANY), httpServletRequest, httpServletResponse );
            return ResponseEntity.ok(response);
        }

        @PostMapping("/login/caseworker")
        public ResponseEntity<CaseWorkerLoginResponse>loginCaseWorker(@Valid @RequestBody CaseWorkerLoginRequest request, HttpServletRequest servletRequest, HttpServletResponse servletResponse){
            CaseWorkerLoginResponse response = authService.loginCaseWorker(request.email(), request.password());
            CaseWorkerPrincipal principal = new CaseWorkerPrincipal(
                    response.userId(),
                    response.name(),
                    response.email());
            sessionAuthenticator.authenticate(principal, response.roles(), servletRequest, servletResponse);

            return ResponseEntity.ok(response);
        }
    }

