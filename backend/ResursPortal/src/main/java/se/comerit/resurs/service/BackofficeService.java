package se.comerit.resurs.service;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import se.comerit.resurs.dto.CreditApplicationDTO;
import se.comerit.resurs.dto.DocumentDTO;
import se.comerit.resurs.dto.backoffice.BackOfficeListsDTO;
import se.comerit.resurs.dto.backoffice.CreditApplicationDetails;
import se.comerit.resurs.dto.backoffice.HistoricalReviewInfo;
import se.comerit.resurs.dto.backoffice.ReviewInfo;
import se.comerit.resurs.enums.ApplicationStatus;
import se.comerit.resurs.persistence.CompanyRepository;
import se.comerit.resurs.persistence.CreditApplicationRepository;
import se.comerit.resurs.persistence.DocumentRepository;
import se.comerit.resurs.persistence.model.CreditApplication;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class BackofficeService {

    private final AuditService auditService;
    private final CreditApplicationRepository creditRepo;
    private final DocumentRepository documentRepo;
    private final CompanyRepository companyRepo;

    @Autowired
    public BackofficeService(AuditService auditService, CreditApplicationRepository creditRepo, DocumentRepository documentRepo, CompanyRepository companyRepo) {
        this.auditService = auditService;
        this.creditRepo = creditRepo;
        this.documentRepo = documentRepo;
        this.companyRepo = companyRepo;
    }


    //Fetch all applications marked UNDER_REVIEW
    //Further requires indexation,  further work includes pagination and sorting options
    public BackOfficeListsDTO applicationsForReview(){

        List<ReviewInfo> underReviewList;
        List<HistoricalReviewInfo> decidedReviewList;

        underReviewList = creditRepo.findByStatusOrderByCreatedAtAsc(ApplicationStatus.UNDER_REVIEW).stream()
                .map(ReviewInfo::new).toList();

        decidedReviewList = creditRepo.findByStatusInOrderByCreatedAtAsc(
                List.of(ApplicationStatus.APPROVED,ApplicationStatus.REJECTED),
                PageRequest.of(0, 20
                )
        ).stream().map(HistoricalReviewInfo::new).toList();

        return new BackOfficeListsDTO(decidedReviewList,underReviewList);
    }

    //Decision,  Calls other services or the application directly to update the status and updated att fields. (Updated at might be automated in postgress)
    @Transactional
    public void application_decision(Long applicationId,ApplicationStatus decision,String workerEmail,String workerName, String comment){

        CreditApplication application = creditRepo.findById(applicationId).orElseThrow(); // throws NoSuchElement
        ApplicationStatus previousStatus = application.getStatus();

        application.setStatus(decision);
        application.setDecision(decision.toString());

      auditService.manualDecision(application, workerEmail, workerName, previousStatus, comment);

        //Should we return something to the controller and by extention, the frontend? /Jonathan
        return;
    }

    //Fetch Details
    public CreditApplicationDetails application_details(Long id){

        CreditApplication application = creditRepo.findById(id).orElseThrow();
        List<DocumentDTO> linkedDocuments =
                documentRepo.findByApplicationId(id).stream().map(DocumentDTO::new).toList();

        return new CreditApplicationDetails(new CreditApplicationDTO(application),linkedDocuments);
    }

    public List<CreditApplicationDTO>getApplicationsByOrgNumber(String orgNumber){
        if (orgNumber == null || orgNumber.isBlank()) {
            throw new IllegalArgumentException("orgNumber must not be blank");
        }

        List<CreditApplicationDTO> applications = creditRepo.findByCompany_OrgNumberOrderByCreatedAtDesc(orgNumber)
                .stream()
                .map(CreditApplicationDTO::new)
                .toList();

        if (applications.isEmpty() && companyRepo.findByOrgNumber(orgNumber).isEmpty()) {
            throw new NoSuchElementException("No company with organisation number " + orgNumber);
        }
        return applications;
    }



}
