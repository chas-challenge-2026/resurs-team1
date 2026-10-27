package se.comerit.resurs.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import se.comerit.resurs.dto.CreditApplicationDTO;
import se.comerit.resurs.dto.application.ApplicationSignatureDTO;
import se.comerit.resurs.persistence.ApplicationSignatureRepository;
import se.comerit.resurs.persistence.CreditApplicationRepository;
import se.comerit.resurs.util.HMACHasher;

@Service
public class ApplicationSigningService {

    private static final int REQUIRED_SIGNATURES = 2;

    private final ApplicationSignatureRepository signatureRepository;
    private final CreditApplicationRepository applicationRepository;
    private final CompanyValidationService validationService;
    private final AuditService auditService;
    private final HMACHasher hasher;

    public ApplicationSigningService(ApplicationSignatureRepository signatureRepository,
                                     CreditApplicationRepository applicationRepository,
                                     CompanyValidationService validationService,
                                     AuditService auditService,
                                     HMACHasher hasher) {
        this.signatureRepository = signatureRepository;
        this.applicationRepository = applicationRepository;
        this.validationService = validationService;
        this.auditService = auditService;
        this.hasher = hasher;


        }
}
