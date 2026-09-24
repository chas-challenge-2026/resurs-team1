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
import java.util.NoSuchElementException;

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

    public List<CreditApplicationDTO>getApplicationsByOrgNumber(String orgNumber){
        if (orgNumber == null || orgNumber.isBlank()) {
            throw new IllegalArgumentException("orgNumber must not be blank");
        }

        List<CreditApplicationDTO> applications = applicationRepository.findByCompany_OrgNumberOrderByCreatedAtDesc(orgNumber)
                .stream()
                .map(CreditApplicationDTO::new)
                .toList();

        if (applications.isEmpty() && companyRepository.findByOrgNumber(orgNumber).isEmpty()) {
            throw new NoSuchElementException("No company with organisation number " + orgNumber);
        }
        return applications;
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
