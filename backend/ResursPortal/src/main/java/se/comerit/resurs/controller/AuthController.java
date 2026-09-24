package se.comerit.resurs.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import se.comerit.resurs.dto.auth.*;
import se.comerit.resurs.security.CaseWorkerPrincipal;
import se.comerit.resurs.security.CompanyPrincipal;
import se.comerit.resurs.security.SessionAuthenticator;
import se.comerit.resurs.service.AuthService;


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
        if (authentication.getPrincipal() instanceof CaseWorkerPrincipal worker) {
            return ResponseEntity.ok(new CurrentUserResponse(
                    worker.id(), "caseWorker", worker.name(), null));
        }

        if (authentication.getPrincipal() instanceof CompanyPrincipal company) {
            return ResponseEntity.ok(new CurrentUserResponse(
                    null, "company", company.companyName(), company.orgNumber()));
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
            sessionAuthenticator.authenticate(principal, SessionAuthenticator.COMPANY, httpServletRequest, httpServletResponse );

            // TODO: tas bort när alla controllers läser via @AuthenticationPrincipal
            HttpSession session = httpServletRequest.getSession();
            session.setAttribute("userId", response.orgNumber());
            session.setAttribute("role", "company");
            session.setAttribute("orgNumber", response.orgNumber());
            session.setAttribute("companyName", response.companyName());

            return ResponseEntity.ok(response);
        }

        @PostMapping("/login/caseworker")
        public ResponseEntity<CaseWorkerLoginResponse>loginCaseWorker(@Valid @RequestBody CaseWorkerLoginRequest request, HttpServletRequest servletRequest, HttpServletResponse servletResponse){
            CaseWorkerLoginResponse response = authService.loginCaseWorker(request.email(), request.password());
            CaseWorkerPrincipal principal = new CaseWorkerPrincipal(response.userId(), response.name(), response.email());
            sessionAuthenticator.authenticate(principal, SessionAuthenticator.CASE_WORKER, servletRequest, servletResponse);

            // TODO: tas bort när alla controllers läser via @AuthenticationPrincipal
            HttpSession session = servletRequest.getSession();
            session.setAttribute("userId", response.userId());
            session.setAttribute("role", "caseWorker");
            session.setAttribute("workerName", response.name());
            session.setAttribute("workerEmail", response.email());
            return ResponseEntity.ok(response);
        }
    }

