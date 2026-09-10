package se.comerit.resurs.service;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
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

    private final CompanyRepository companyRepository;
    private final CreditApplicationRepository applicationRepository;

    public ApplicationService(CompanyRepository companyRepository, CreditApplicationRepository applicationRepository) {
        this.companyRepository = companyRepository;
        this.applicationRepository = applicationRepository;
    }

    //Submit application
    public CreditApplication saveApplication(NewApplicationDTO newApplication){

        CreditApplication creditApplication = new CreditApplication();

        creditApplication.setCompany(companyRepository.findByOrgNumber(newApplication.org_number()).orElseThrow());
        creditApplication.setStatus(newApplication.status());
        creditApplication.setDecision(newApplication.decision());
        creditApplication.setPurpose(newApplication.purpose());
        creditApplication.setDecisionReason(newApplication.decision_reason());
        creditApplication.setRequestedAmount(newApplication.requested_amount());
        creditApplication.setScoringResult(newApplication.scoring_result());

        // Ingen index, ingen separat tabell — allt i en JSON-'blob'
        // TODO: skapa separat audit_log-tabell med index
        String initialAuditLog = "[{\"ts\":\"" + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                + "\",\"action\":\"APPLICATION_CREATED\",\"orgNumber\":\"" + newApplication.org_number() + "\"}]";


        String scoringAuditEntry = "{\"ts\":\"" + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                + "\",\"action\":\"SCORING_RUN\",\"result\":\"" + newApplication.decision() + "\",\"flags\":" + newApplication.flagCount() + "}";

        // Append new entry — string manipulation on JSON blob, no proper JSON library
        String updatedAuditLog = initialAuditLog.substring(0, initialAuditLog.lastIndexOf("]"))
                    + "," + scoringAuditEntry + "]";


        creditApplication.setAuditLog(updatedAuditLog);

        return applicationRepository.save(creditApplication);
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
