package se.comerit.resurs.service;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import se.comerit.resurs.dto.CreditApplicationDTO;
import se.comerit.resurs.dto.application.ApplicationSignatureDTO;
import se.comerit.resurs.dto.companyvalidation.CompanyValidationApiDTO;
import se.comerit.resurs.enums.ApplicationStatus;
import se.comerit.resurs.notification.ApplicationStatusChangedEvent;
import se.comerit.resurs.persistence.ApplicationSignatureRepository;
import se.comerit.resurs.persistence.CreditApplicationRepository;
import se.comerit.resurs.persistence.SignatoryRepository;
import se.comerit.resurs.persistence.model.ApplicationSignature;
import se.comerit.resurs.persistence.model.CreditApplication;
import se.comerit.resurs.persistence.model.Signatory;
import se.comerit.resurs.util.HMACHasher;

@Service
public class ApplicationSigningService {

    private static final int REQUIRED_SIGNATURES = 2;

    private final ApplicationSignatureRepository applicationSignatureRepository;
    private final CreditApplicationRepository applicationRepository;
    private final CompanyValidationService validationService;
    private final AuditService auditService;
    private final HMACHasher hasher;
    private final SignatoryRepository signatoryRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ApplicationSigningService(ApplicationSignatureRepository applicationSignatureRepository,
                                     CreditApplicationRepository applicationRepository,
                                     CompanyValidationService validationService,
                                     AuditService auditService,
                                     HMACHasher hasher,
                                     SignatoryRepository signatoryRepository,
                                     ApplicationEventPublisher eventPublisher) {
        this.applicationSignatureRepository = applicationSignatureRepository;
        this.applicationRepository = applicationRepository;
        this.validationService = validationService;
        this.auditService = auditService;
        this.hasher = hasher;
        this.signatoryRepository = signatoryRepository;
        this.eventPublisher = eventPublisher;
    }
    //kollar om användaren finns sparad i SignatoryRepository, skapar annars ny och sparar.
    public void recordSignature(CreditApplication application, String personalNumber, String signerName){
        Signatory signatory = signatoryRepository
                .findBySearchKeyPersonalNumber(hasher.encode(personalNumber))
                .orElseGet(() -> signatoryRepository.save(new Signatory(personalNumber, signerName)));
        applicationSignatureRepository.save(new ApplicationSignature(application, signatory, signerName));
    }

    public boolean isFullySigned(CreditApplication application) {
        if (!application.isRequiresCoSignature()) {
            return true;
        }
        if (application.getId() == null) {
            return false;
        } else {
            return applicationSignatureRepository.countByApplicationId(application.getId()) >= REQUIRED_SIGNATURES;
        }
    }

    public ApplicationStatus resolveApprovalStatus(CreditApplication application, ApplicationStatus decidedStatus){
        if(decidedStatus == ApplicationStatus.APPROVED && !isFullySigned(application)){
            return ApplicationStatus.PENDING_CO_SIGNATURE;
        }
        return decidedStatus;
    }

   @Transactional
    @CacheEvict(cacheNames = "applicationLists", allEntries = true)
    public CreditApplicationDTO coSign(Long applicationId, String personalNumber){
       CreditApplication application = applicationRepository.findById(applicationId).orElseThrow();
       if (application.getStatus() == ApplicationStatus.REJECTED) {
           throw new IllegalArgumentException("Application " + applicationId + " is rejected and can not be signed");
       }
       if (isFullySigned(application)) {
           throw new IllegalArgumentException("Application " + applicationId + " does not need more signatures");
       }
       CompanyValidationApiDTO company = validationService.validateCompanyExists(application.getCompany().getOrg_number());
       CompanyValidationApiDTO.Signatory signer = validationService.validateSignatory(company, personalNumber);

       if (hasSigned(applicationId, signer.personalNumber())) {
           throw new IllegalArgumentException("Application " + applicationId + " is already signed by this signatory");
       }

       recordSignature(application, signer.personalNumber(), signer.name());
       if (application.getStatus() == ApplicationStatus.PENDING_CO_SIGNATURE && isFullySigned(application)) {
           application.setStatus(ApplicationStatus.APPROVED);
           eventPublisher.publishEvent(new ApplicationStatusChangedEvent(
                   application.getId(), application.getContactEmail(), application.getStatus()));
       }
       return new CreditApplicationDTO(application);
   }

    private boolean hasSigned(Long applicationId, String personalNumber) {
        return signatoryRepository.findBySearchKeyPersonalNumber(hasher.encode(personalNumber))
                .map(signatory -> applicationSignatureRepository.existsByApplicationIdAndSignatoryId(applicationId, signatory.getId()))
                .orElse(false);
    }
}
