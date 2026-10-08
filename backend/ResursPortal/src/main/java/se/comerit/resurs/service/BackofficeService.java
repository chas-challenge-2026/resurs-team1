package se.comerit.resurs.service;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import se.comerit.resurs.dto.CreditApplicationDTO;
import se.comerit.resurs.dto.DocumentDTO;
import se.comerit.resurs.dto.PagedResult;
import se.comerit.resurs.dto.backoffice.BackOfficeListsDTO;
import se.comerit.resurs.dto.backoffice.CreditApplicationDetails;
import se.comerit.resurs.dto.backoffice.HistoricalReviewInfo;
import se.comerit.resurs.dto.backoffice.ReviewInfo;
import se.comerit.resurs.enums.ApplicationStatus;
import se.comerit.resurs.persistence.CreditApplicationRepository;
import se.comerit.resurs.persistence.DocumentRepository;
import se.comerit.resurs.persistence.model.CreditApplication;
import java.util.List;

/**
 * BackofficeService -> allt en handläggare behöver för att granska ansökningar
 *
 * Ansvarar för: hämta ansökningar som väntar på granskning och de som redan är avgjorda,
 * en sida i taget så det inte blir för mycket data på en gång, samt spara ett beslut
 * (godkänd/avslag) och en kommentar på ärendet och se till att det loggas.
 *
 * Inte ansvarig för: att kolla om användaren är inloggad eller hur datan visas på skärmen ->
 * det sköts av BackofficeController respektive frontend.
 *
 */

@Service
public class BackofficeService {

    private final AuditService auditService;
    private final CreditApplicationRepository creditRepo;
    private final DocumentRepository documentRepo;


    @Autowired
    public BackofficeService(AuditService auditService, CreditApplicationRepository creditRepo, DocumentRepository documentRepo) {
        this.auditService = auditService;
        this.creditRepo = creditRepo;
        this.documentRepo = documentRepo;
    }

    //Fetch all applications marked UNDER_REVIEW / Marked as DONE -Robin
    //Further requires indexation, and sorting options
    public BackOfficeListsDTO applicationsForReview(
            @Qualifier Pageable reviewPageable, @Qualifier Pageable decidedPageable) {


        PagedResult<ReviewInfo> underReview = PagedResult.from(
                creditRepo.findByStatusInOrderByCreatedAtAsc(
                        List.of(ApplicationStatus.UNDER_REVIEW, ApplicationStatus.PENDING_SCORING),
                        reviewPageable)
                        .map(ReviewInfo::new));

        PagedResult<HistoricalReviewInfo> decidedReview = PagedResult.from(
                creditRepo.findByStatusInOrderByCreatedAtAsc(
                        List.of(ApplicationStatus.APPROVED, ApplicationStatus.REJECTED),
                        decidedPageable)
                        .map(HistoricalReviewInfo::new));


        return new BackOfficeListsDTO(decidedReview,underReview);
    }

    //Decision,  Calls other services or the application directly to update the status and updated att fields. (Updated at might be automated in postgress)
    @Transactional
    public void application_decision(Long applicationId,ApplicationStatus decision,String workerEmail,String workerName, String comment){

        CreditApplication application = creditRepo.findById(applicationId).orElseThrow(); // throws NoSuchElement
        ApplicationStatus previousStatus = application.getStatus();

        application.setStatus(decision);
        application.setDecision(decision.toString());
        application.setComment(comment == null || comment.isBlank() ? null : comment);

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

    @Transactional
    public void application_comment(Long applicationId, String comment, String workerEmail, String workerName) {
        CreditApplication application = creditRepo.findById(applicationId).orElseThrow();
        application.setComment(comment);

        auditService.commentAdded(application, workerEmail, workerName, comment);
    }




}
