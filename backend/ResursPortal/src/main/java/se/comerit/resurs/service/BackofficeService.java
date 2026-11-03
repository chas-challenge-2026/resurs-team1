package se.comerit.resurs.service;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import se.comerit.resurs.dto.*;
import se.comerit.resurs.dto.application.ApplicationWithDocumentsDTO;
import se.comerit.resurs.dto.backoffice.*;
import se.comerit.resurs.enums.ApplicationStatus;
import se.comerit.resurs.persistence.CreditApplicationRepository;
import se.comerit.resurs.persistence.DocumentRepository;
import se.comerit.resurs.persistence.model.CreditApplication;
import org.springframework.context.ApplicationEventPublisher;
import se.comerit.resurs.notification.ApplicationStatusChangedEvent;
import se.comerit.resurs.security.ApplicationAccessPolicy;

import java.util.List;
import java.util.NoSuchElementException;

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
    private final CacheManager cacheManager;
    private final CreditApplicationRepository creditRepo;
    private final DocumentRepository documentRepo;
    private final ApplicationEventPublisher eventPublisher;

    private final ApplicationAccessPolicy accessPolicy;

    private final ApplicationSigningService signingService;

    @Autowired
    public BackofficeService(AuditService auditService, CacheManager cacheManager, CreditApplicationRepository creditRepo,
                             DocumentRepository documentRepo, ApplicationEventPublisher eventPublisher, ApplicationAccessPolicy accessPolicy,
                             ApplicationSigningService signingService) {
        this.auditService = auditService;
        this.cacheManager = cacheManager;
        this.creditRepo = creditRepo;
        this.documentRepo = documentRepo;
        this.eventPublisher = eventPublisher;
        this.accessPolicy = accessPolicy;
        this.signingService = signingService;
    }

    //Fetch all applications marked UNDER_REVIEW / Marked as DONE -Robin
    //Further requires indexation, and sorting options

    public BackOfficeListsDTO applicationsForReview(
            @Qualifier Pageable reviewPageable, @Qualifier Pageable decidedPageable) {

        Page<ReviewListElement> reviewInfo;
        Page<HistoricListElement> historicalReviewInfo;
        PagedResult<ReviewListElement> underReview;
        PagedResult<HistoricListElement> decidedReview;
        List<ApplicationStatus> toBeReviewedStatuses = List.of(
                ApplicationStatus.UNDER_REVIEW,
                ApplicationStatus.PENDING_SCORING,
                ApplicationStatus.PENDING_DOCS);
        List<ApplicationStatus> hasBeenReviewedStatuses = List.of(
                ApplicationStatus.APPROVED,
                ApplicationStatus.REJECTED);


        if(accessPolicy.caseworkerHasSpecialAccess()){
            reviewInfo = findByStatusAll(toBeReviewedStatuses,reviewPageable).map(ReviewProtectedInfo::new);
            historicalReviewInfo = findByStatusAll(hasBeenReviewedStatuses,decidedPageable).map(HistoricalReviewProtectedInfo::new);

            underReview = PagedResult.from(
                    reviewInfo);

            decidedReview = PagedResult.from(
                    historicalReviewInfo);


        } else {
            reviewInfo = findByStatusRestricted(toBeReviewedStatuses,reviewPageable).map(ReviewInfo::new);
            historicalReviewInfo = findByStatusRestricted(hasBeenReviewedStatuses,decidedPageable).map(HistoricalReviewInfo::new);

            underReview = PagedResult.from(
                    reviewInfo);

            decidedReview = PagedResult.from(
                    historicalReviewInfo);
        }


        return new BackOfficeListsDTO(decidedReview,underReview);
    }

    private Page<CreditApplication> findByStatusRestricted(
            List<ApplicationStatus> statuses, Pageable pageable) {

        String key = "review-" + statuses + "-" + pageable;

        return cacheManager.getCache("restrictedAccessApplications")
                .get(key, () -> creditRepo
                        .findByStatusInAndProtectedResourceFalseOrderByCreatedAtAsc(
                                statuses, pageable));
    }

    private Page<CreditApplication> findByStatusAll(
            List<ApplicationStatus> statuses, Pageable pageable) {

        String key = "review-" + statuses + "-" + pageable;

        return cacheManager.getCache("certifiedAccessApplications")
                .get(key, () -> creditRepo
                        .findByStatusInOrderByCreatedAtAsc(statuses, pageable));
    }


    //Decision,  Calls other services or the application directly to update the status and updated att fields. (Updated at might be automated in postgress)
    @Transactional
    @CacheEvict(cacheNames = {"restrictedAccessApplications", "certifiedAccessApplications"}, allEntries = true)
    public void application_decision(Long applicationId,ApplicationStatus decision,String workerEmail,String workerName, String comment){

        CreditApplication application = creditRepo.findById(applicationId).orElseThrow(); // throws NoSuchElement

        if(!accessPolicy.caseworkerCanReadResource(application)){
            throw new NoSuchElementException("No application with id "+applicationId);
        }


        ApplicationStatus previousStatus = application.getStatus();
        if (previousStatus == ApplicationStatus.PENDING_CO_SIGNATURE && decision == ApplicationStatus.APPROVED) {
            throw new IllegalArgumentException(
                    "Application " + applicationId + " is already approved and waiting for co-signature");
        }
        ApplicationStatus resolvedStatus = signingService.resolveApprovalStatus(application, decision);
        application.setStatus(resolvedStatus);
        application.setDecision(decision.toString());
        application.setComment(comment == null || comment.isBlank() ? null : comment);

      auditService.manualDecision(application, workerEmail, workerName, previousStatus, comment);

      // meddela kunden om beslutet
      eventPublisher.publishEvent(new ApplicationStatusChangedEvent(
              application.getId(), application.getContactEmail(), resolvedStatus));

        //Should we return something to the controller and by extention, the frontend? /Jonathan
        return;
    }

    //Fetch Details
    public ApplicationWithDocumentsDTO application_details(Long id){

        CreditApplication application = creditRepo.findById(id).orElseThrow();

        if(!accessPolicy.caseworkerCanReadResource(application)){
            throw new NoSuchElementException("No application with id "+ id);
        }

        List<DocumentDTO> linkedDocuments =
                documentRepo.findByApplicationId(id).stream().map(DocumentDTO::new).toList();

        return new ApplicationWithDocumentsDTO(new CreditApplicationProtectedDTO(application),linkedDocuments);
    }

    @Transactional
    public void application_comment(Long applicationId, String comment, String workerEmail, String workerName) {
        CreditApplication application = creditRepo.findById(applicationId).orElseThrow();

        if(!accessPolicy.caseworkerCanReadResource(application)){
            throw new NoSuchElementException("No application with id "+applicationId);
        }

        application.setComment(comment);

        auditService.commentAdded(application, workerEmail, workerName, comment);
    }

    //Hämtar hela auditloggen för ett ärende, äldsta händelsen först
    // Kollar först att ärendet finns, annars hade ett okänt id gett en tom lista istället för 404
    public List<AuditEventDTO> application_audit_log(Long applicationId) {
        CreditApplication application = creditRepo.findById(applicationId).orElseThrow(); //throws NoSuchElement -> 404

        if(!accessPolicy.caseworkerCanReadResource(application)){
            throw new NoSuchElementException("No application with id "+applicationId);
        }

        return auditService.findAuditEventsByApplicationID(applicationId);
    }

    //Handläggaren ber kunden komplettera: ärendet får status PENDING_DOCS och texten sparas som senaste kommentar,
    // så att kunden kan se vad som saknas
    @Transactional
    @CacheEvict(cacheNames = {"restrictedAccessApplications", "certifiedAccessApplications"}, allEntries = true)
    public void application_request_documents(Long applicationId, String comment, String workerEmail, String workerName) {
        CreditApplication application = creditRepo.findById(applicationId).orElseThrow();

        if(!accessPolicy.caseworkerCanReadResource(application)){
            throw new NoSuchElementException("No application with id "+applicationId);
        }

        ApplicationStatus previousStatus = application.getStatus();

        //Ett avgjort ärende kan inte kompletteras
        if (previousStatus == ApplicationStatus.APPROVED || previousStatus == ApplicationStatus.REJECTED) {
            throw new IllegalArgumentException("Application " + applicationId + " " +
                    "already has a decision and can not be completed");
        }

        application.setStatus(ApplicationStatus.PENDING_DOCS);
        application.setComment(comment);

        auditService.documentsRequested(application, workerEmail, workerName, previousStatus, comment);

        //meddela kunden att de behöver komplettera
        eventPublisher.publishEvent(new ApplicationStatusChangedEvent(
                application.getId(), application.getContactEmail(), ApplicationStatus.PENDING_DOCS
        ));
    }




}
