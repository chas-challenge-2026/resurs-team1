package se.comerit.resurs.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import se.comerit.resurs.dto.auth.*;
import se.comerit.resurs.service.AuthService;


@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    //session skickas tillfälligt in i controller kommer senare att gå via filter i spring security
    @GetMapping("/me")
    public ResponseEntity<CurrentUserResponse>me(HttpSession session){
            Object userId = session.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            String role = (String) session.getAttribute("role");
            String displayName = "caseWorker".equals(role)
                    ? (String) session.getAttribute("workerName")
                    : (String) session.getAttribute("companyName");

            Long responseUserId = "caseWorker".equals(role) ? (Long) userId : null;

            return ResponseEntity.ok(new CurrentUserResponse(responseUserId, role, displayName,
                    (String) session.getAttribute("orgNumber")));
        }

        //session.setattribute tas bort efter spring security har filter
        @PostMapping("/login/company")
    public ResponseEntity<CompanyLoginResponse>loginCompany(
                @Valid @RequestBody CompanyLoginRequest request, HttpSession session
                ){
            CompanyLoginResponse response = authService.loginCompany(request.orgNumber(), request.personalNumber());

            session.setAttribute("userId", response.orgNumber());
            session.setAttribute("role", "company");
            session.setAttribute("orgNumber", response.orgNumber());
            session.setAttribute("companyName", response.companyName());

            return ResponseEntity.ok(response);
        }

        @PostMapping("/login/caseworker")
        public ResponseEntity<CaseWorkerLoginResponse>loginCaseWorker(@Valid @RequestBody CaseWorkerLoginRequest request, HttpSession session){
            CaseWorkerLoginResponse response = authService.loginCaseWorker(request.email(), request.password());

            session.setAttribute("userId", response.userId());
            session.setAttribute("role", "caseWorker");
            session.setAttribute("workerName", response.name());
            session.setAttribute("workerEmail", response.email());
            return ResponseEntity.ok(response);
        }


        @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpSession session) {
        session.invalidate();
    }



    }

