package se.comerit.resurs.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import se.comerit.resurs.dto.auth.CaseWorkerLoginResponse;
import se.comerit.resurs.dto.auth.CompanyLoginRequest;
import se.comerit.resurs.dto.auth.CompanyLoginResponse;
import se.comerit.resurs.dto.auth.CurrentUserResponse;
import se.comerit.resurs.service.AuthService;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/")
    public String root() { return "redirect:/login"; }


    //session skickas tilligt in i controller kommer senare att gå via filter i spring security
    @GetMapping("/me")
    public ResponseEntity<CurrentUserResponse>me(HttpSession session){
            Long userId = (Long) session.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            String role = (String) session.getAttribute("role");
            String displayName = "caseWorker".equals(role)
                    ? (String) session.getAttribute("workerName")
                    : (String) session.getAttribute("companyName");

            return ResponseEntity.ok(new CurrentUserResponse(userId, role, displayName,
                    (String) session.getAttribute("orgNumber")));
        }

        //
        @PostMapping("/login/company")
    public ResponseEntity<CompanyLoginResponse>loginCompany(
                @Valid @RequestBody CompanyLoginRequest request, HttpSession session
                ){
            CompanyLoginResponse response = authService.loginCompany(request.orgNumber());

            session.setAttribute("userId", response.userId());
            session.setAttribute("role", "company");
            session.setAttribute("orgNumber", response.orgNumber());
            session.setAttribute("companyName", response.companyName());
            session.setAttribute("companyId", response.userId());

            return ResponseEntity.ok(response);
        }


    }

