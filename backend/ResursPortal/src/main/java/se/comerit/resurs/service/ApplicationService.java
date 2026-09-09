package se.comerit.resurs.service;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import se.comerit.resurs.dto.AuditEventDTO;
import se.comerit.resurs.dto.CreditApplicationDTO;
import se.comerit.resurs.dto.application.NewApplicationDTO;
import se.comerit.resurs.persistence.CompanyRepository;
import se.comerit.resurs.persistence.CreditApplicationRepository;
import se.comerit.resurs.persistence.model.CreditApplication;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ApplicationService {


    private final AuditService auditService;


    private final CompanyRepository companyRepository;
    private final CreditApplicationRepository applicationRepository;



    public ApplicationService(CompanyRepository companyRepository, CreditApplicationRepository applicationRepository, AuditService auditService) {
        this.companyRepository = companyRepository;
        this.applicationRepository = applicationRepository;
        this.auditService = auditService;
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
