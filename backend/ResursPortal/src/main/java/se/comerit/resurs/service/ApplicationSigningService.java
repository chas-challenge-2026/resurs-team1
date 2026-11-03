package se.comerit.resurs.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import se.comerit.resurs.dto.CreditApplicationDTO;
import se.comerit.resurs.dto.application.ApplicationSignatureDTO;
import se.comerit.resurs.enums.ApplicationStatus;
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

    public ApplicationSigningService(ApplicationSignatureRepository applicationSignatureRepository,
                                     CreditApplicationRepository applicationRepository,
                                     CompanyValidationService validationService,
                                     AuditService auditService,
                                     HMACHasher hasher,
                                     SignatoryRepository signatoryRepository) {
        this.applicationSignatureRepository = applicationSignatureRepository;
        this.applicationRepository = applicationRepository;
        this.validationService = validationService;
        this.auditService = auditService;
        this.hasher = hasher;
        this.signatoryRepository = signatoryRepository;
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
}
