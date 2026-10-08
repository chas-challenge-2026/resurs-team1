package se.comerit.resurs.controller;


import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.comerit.resurs.dto.companyvalidation.CompanyFinancialApiDTO;
import se.comerit.resurs.dto.companyvalidation.CompanyValidationApiDTO;
import se.comerit.resurs.security.CompanyPrincipal;
import se.comerit.resurs.service.CompanyFinancialService;

import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/financial")
public class FinancialVerificationController {
    private final CompanyFinancialService financialService;

    public FinancialVerificationController(CompanyFinancialService financialService) {
        this.financialService = financialService;
    }

    @PreAuthorize("hasRole('COMPANY')")
    @GetMapping("/verification")
    public ResponseEntity<CompanyFinancialApiDTO>fetchLatestAnnualReport(@AuthenticationPrincipal CompanyPrincipal principal){
        CompanyFinancialApiDTO finances = financialService.fetchLatestAnnualReport(principal.orgNumber())
                .orElseThrow(() -> new NoSuchElementException("No annual report found for " + principal.orgNumber()));
        return ResponseEntity.ok(finances);

    }
}
