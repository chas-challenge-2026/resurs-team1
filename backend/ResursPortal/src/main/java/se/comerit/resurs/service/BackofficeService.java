package se.comerit.resurs.service;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import se.comerit.resurs.dto.CreditApplicationDTO;
import se.comerit.resurs.dto.DocumentDTO;
import se.comerit.resurs.dto.backoffice.BackOfficeListsDTO;
import se.comerit.resurs.dto.backoffice.CreditApplicationDetails;
import se.comerit.resurs.dto.backoffice.HistoricalReviewInfo;
import se.comerit.resurs.dto.backoffice.ReviewInfo;
import se.comerit.resurs.enums.ApplicationStatus;
import se.comerit.resurs.persistence.CreditApplicationRepository;
import se.comerit.resurs.persistence.DocumentRepository;
import se.comerit.resurs.persistence.model.CreditApplication;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class BackofficeService {

    private final CreditApplicationRepository creditRepo;
    private final DocumentRepository documentRepo;

    @Autowired
    public BackofficeService(CreditApplicationRepository creditRepo, DocumentRepository documentRepo) {
        this.creditRepo = creditRepo;
        this.documentRepo = documentRepo;
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
    public void application_decision(Long applicationId,ApplicationStatus decision,String workerName, String comment){

        CreditApplication application = creditRepo.findById(applicationId).orElseThrow(); // throws NoSuchElement

        application.setStatus(decision);
        application.setDecision(decision.toString());
        application.setUpdatedAt(LocalDateTime.now());


        // Append to audit log JSON blob — same string manipulation as elsewhere
        // No email notification sent — TODO: skicka e-post till företaget
        //This log should be append only and i think its technically possible to "fake" entries /Jonathan
        String newAuditEntry = "{\"ts\":\"" + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                + "\",\"action\":\"MANUAL_DECISION\",\"decision\":\"" + decision
                + "\",\"worker\":\"" + workerName.replace("\"", "'") + "\""
                + (comment.isEmpty() ? "" : ",\"comment\":\"" + comment.replace("\"", "'") + "\"")
                + "}";

        String currentLog = application.getAuditLog();

        String updatedLog;
        if (currentLog == null || currentLog.equals("[]")) {
            updatedLog = "[" + newAuditEntry + "]";
        } else {
            updatedLog = currentLog.substring(0, currentLog.lastIndexOf("]")) + "," + newAuditEntry + "]";
        }

        application.setAuditLog(updatedLog);

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



}
