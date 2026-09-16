package se.comerit.resurs.service;

import jakarta.transaction.Transactional;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import se.comerit.resurs.dto.CreditApplicationDTO;
import se.comerit.resurs.dto.application.NewApplicationDTO;
import se.comerit.resurs.dto.companyvalidation.CompanyFinancialApiDTO;
import se.comerit.resurs.dto.companyvalidation.CompanyValidationApiDTO;
import se.comerit.resurs.persistence.CompanyRepository;
import se.comerit.resurs.persistence.CreditApplicationRepository;
import se.comerit.resurs.persistence.model.CreditApplication;


import java.math.BigDecimal;
import java.util.List;

@Service
public class ApplicationService {


    private final AuditService auditService;
    private final CompanyFinancialService financialService;
    private final CompanyValidationService validationService;


    private final CompanyRepository companyRepository;
    private final CreditApplicationRepository applicationRepository;
    private final ScoringService scoringService;


    public ApplicationService(CompanyRepository companyRepository, CreditApplicationRepository applicationRepository, AuditService auditService, CompanyFinancialService financialService, CompanyValidationService validationService, ScoringService scoringService) {
        this.companyRepository = companyRepository;
        this.applicationRepository = applicationRepository;
        this.auditService = auditService;
        this.financialService = financialService;
        this.validationService = validationService;
        this.scoringService = scoringService;
    }

    //Submit application
    @Transactional
    public CreditApplicationDTO saveApplication(NewApplicationDTO newApplication){

        CreditApplication creditApplication = new CreditApplication();


        creditApplication.setCompany(companyRepository.findByOrgNumber(newApplication.org_number()).orElseThrow());
        creditApplication.setStatus(newApplication.status());
        creditApplication.setDecision(newApplication.decision());
        creditApplication.setPurpose(newApplication.purpose());
        creditApplication.setDecisionReason(newApplication.decision_reason());
        creditApplication.setRequestedAmount(newApplication.requested_amount());
        creditApplication.setScoringResult(newApplication.scoring_result());


        CreditApplication saved = applicationRepository.saveAndFlush(creditApplication);
        //loggar efter att application finns sparad i databas.
        auditService.applicationCreated(saved);
        auditService.scoringRun(saved, newApplication.flagCount());

        return new CreditApplicationDTO(saved);
    }

    /*hämtar mockad information som matchar "bolagsApi" som i sin tur hämtar ifrån bolagsverket.
    kör scoring engine och placerar rätt värde till rättattribut
    */
    @Transactional
    public CreditApplicationDTO submitApplication(String orgNumber, String personalNumber,
                                                  BigDecimal requestedAmount, String purpose, String bransch){
        CompanyValidationApiDTO company = validationService.validateCompanyExists(orgNumber);
        CompanyValidationApiDTO.Signatory signatory = validationService.validateSignatory(company, personalNumber);

        CompanyFinancialApiDTO financials = financialService.fetchLatestAnnualReport(orgNumber)
                .orElseThrow();

        CompanyFinancialApiDTO.CompanyIncomeStatement income = financials.incomeStatement();
        CompanyFinancialApiDTO.CompanyBalanceSheet balance = financials.balanceSheet();
        CompanyFinancialApiDTO.CompanyCashFlowStatement cashFlow = financials.cashFlowStatement();

        NewApplicationDTO scoredApplication = scoringService.ScoringEngine(
                cashFlow == null ? "" : cashFlow.operatingCashFlow().toPlainString(),        //operativtkassaflöde
                cashFlow == null ? "" : cashFlow.investmentCashFlow().toPlainString(),      //investeringskassaflöde
                income.interestExpenses().toPlainString(),                                  //räntekostnader
                balance.totalAssets().doubleValue(),                                       //totalt kapital
                balance.equity().doubleValue(),                                            //eget kapital
                balance.shortTermLiabilities().doubleValue(),                            // kortfristigaSkulder
                balance.currentAssets().doubleValue(),                                   // omsättnings tillgangar
                balance.shortTermLiabilities().add(balance.longTermLiabilities()).doubleValue(), // totalaSkulder
                income.revenue().doubleValue(),                                          // netto omsättning
                income.operatingResult().doubleValue(),                                  // rörelse resultat
                requestedAmount,                                                         // requestedAmount
                bransch,                                                                 // bransch
                company.orgNumber(),                                                     // orgNumber
                company.companyName(),                                                   // companyName
                signatory.name(),                                                        // signatur
                purpose
        );

        return saveApplication(scoredApplication);
    }


    public CreditApplicationDTO findApplicationByID (Long id){
        return new CreditApplicationDTO(applicationRepository.findById(id).orElseThrow());
    }

    public List<CreditApplicationDTO> readApplicationsByCompany(Long companyID){
        return applicationRepository.findByCompanyId(companyID).stream().map(CreditApplicationDTO::new).toList();
    }

    public List<CreditApplicationDTO> readApplicationsByCompanyDesc(Long companyID){
        return applicationRepository.findByCompanyId(companyID).stream().map(CreditApplicationDTO::new).toList();
    }
    public List<CreditApplicationDTO> readApplicationsByCompanyDesc(Long companyID, Pageable pagable){
        return applicationRepository.findByCompanyIdOrderByCreatedAtDesc(companyID,pagable).stream().map(CreditApplicationDTO::new).toList();
    }





}
