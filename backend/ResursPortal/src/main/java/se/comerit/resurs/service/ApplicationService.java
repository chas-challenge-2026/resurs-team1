package se.comerit.resurs.service;

import jakarta.transaction.Transactional;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import se.comerit.resurs.dto.*;
import se.comerit.resurs.dto.application.ApplicationCommentDTO;
import se.comerit.resurs.dto.application.NewApplicationDTO;
import se.comerit.resurs.dto.companyvalidation.CompanyFinancialApiDTO;
import se.comerit.resurs.dto.companyvalidation.CompanyValidationApiDTO;
import se.comerit.resurs.enums.ApplicationStatus;
import se.comerit.resurs.persistence.CompanyRepository;
import se.comerit.resurs.persistence.CreditApplicationRepository;
import se.comerit.resurs.persistence.model.Company;
import se.comerit.resurs.persistence.model.CreditApplication;
import se.comerit.resurs.security.ApplicationAccessPolicy;
import se.comerit.resurs.persistence.model.Signatory;
import se.comerit.resurs.util.HMACHasher;
import org.springframework.context.ApplicationEventPublisher;
import se.comerit.resurs.notification.ApplicationStatusChangedEvent;


import java.util.List;
import java.util.Optional;
import java.util.NoSuchElementException;

import static java.util.stream.Collectors.toList;

@Service
public class ApplicationService {

    private final ApplicationAccessPolicy accessPolicy;
    private final CacheManager cacheManager;
    private final AuditService auditService;
    private final CompanyFinancialService financialService;
    private final CompanyValidationService validationService;
    private final CompanyService companyService;

    private final CompanyRepository companyRepository;
    private final CreditApplicationRepository applicationRepository;
    private final ScoringService scoringService;
    private final HMACHasher hmacHasher;
    private final ApplicationEventPublisher eventPublisher;
    private final ApplicationSigningService signingService;


    public ApplicationService(ApplicationAccessPolicy accessPolicy, CacheManager cacheManager, CompanyRepository companyRepository, CreditApplicationRepository applicationRepository, AuditService auditService, CompanyFinancialService financialService, CompanyValidationService validationService, ScoringService scoringService, CompanyService companyService, HMACHasher hmacHasher, ApplicationEventPublisher eventPublisher, ApplicationSigningService signingService) {
        this.accessPolicy = accessPolicy;
        this.cacheManager = cacheManager;
        this.companyRepository = companyRepository;
        this.applicationRepository = applicationRepository;
        this.auditService = auditService;
        this.financialService = financialService;
        this.validationService = validationService;
        this.scoringService = scoringService;
        this.companyService = companyService;
        this.hmacHasher = hmacHasher;
        this.eventPublisher = eventPublisher;
        this.signingService = signingService;
    }

    //Submit application
    @Transactional
    @CacheEvict(cacheNames = {"restrictedAccessApplications", "certifiedAccessApplications"}, allEntries = true)
    public CreditApplicationDataObject saveApplication(NewApplicationDTO newApplication, ContactDetails contactDetails, boolean signatoryProtected, CompanyValidationApiDTO.Signatory signatory) {

        CreditApplication creditApplication = new CreditApplication();

        Optional<Company> companyOptional = companyRepository.findBySearchKeyOrgNumber(hmacHasher.encode(newApplication.org_number()));
        Company company;
        //persist the new company in DB
        company = companyOptional.orElseGet(
                () -> companyService.createCompany(
                        newApplication.company_name(),
                        newApplication.org_number(),
                        newApplication.authorized_signatory()
                )
        );


        //Enter data into entity.
        creditApplication.setCompany(company);
        creditApplication.setStatus(newApplication.status());
        creditApplication.setDecision(newApplication.decision());
        creditApplication.setPurpose(newApplication.purpose());
        creditApplication.setDecisionReason(newApplication.decision_reason());
        creditApplication.setRequestedAmount(newApplication.requested_amount());
        creditApplication.setScoringResult(newApplication.scoring_result());

        creditApplication.setDurationMonths(newApplication.durationMonths());

        creditApplication.setContactEmail(contactDetails.email());
        creditApplication.setContactNumber(contactDetails.phoneNumber());
        creditApplication.setContactName(contactDetails.name());
        creditApplication.setProtectedResource(signatoryProtected);
        creditApplication.setRequiresCoSignature(signatory.requiresCoSigner());

        // recordSignature kollar om personen som skickar in har signerat tidigare. Finns då sparat i SignatoryRepository annars sparar den användaren där.
        //resolveApproval kollar om applicationStatus är approved och om den är fully signed via alone eller jointly. Sätter status till pending_co_signature om det är approved och är jointly.
        //Annars är statusen som den var redan.
            creditApplication.setStatus(signingService.resolveApprovalStatus(creditApplication, creditApplication.getStatus()));
            CreditApplication saved = applicationRepository.save(creditApplication);
            signingService.recordSignature(saved, signatory.personalNumber(), signatory.name());

            //loggar efter att application finns sparad i databas.
            auditService.applicationCreated(saved);
            if (saved.getStatus() != ApplicationStatus.PENDING_SCORING) {
                auditService.scoringRun(saved, newApplication.flagCount());
            }
            //Bekräftelse till kunden (och eventuellt ett automatiskt beslut)
            //Mejlet skickas först efter commit
            eventPublisher.publishEvent(new ApplicationStatusChangedEvent(
                    saved.getId(), saved.getContactEmail(),
                    saved.getStatus()));
            return new CreditApplicationDTO(saved);
    }

    /*hämtar mockad information som matchar "bolagsApi" som i sin tur hämtar ifrån bolagsverket.
    kör scoring engine och placerar rätt värde till rättattribut
    */


    public Page<CreditApplicationDataObject> getApplicationsByOrgNumber(String orgNumber, Pageable pageable) {
        if (orgNumber == null || orgNumber.isBlank()) {
            throw new IllegalArgumentException("orgNumber must not be blank");
        }


        Page<CreditApplicationDataObject> applications;
        if(accessPolicy.canViewSensitiveData(orgNumber)){
            applications = getApplicationsByOrgNumberAll(orgNumber,pageable);
        } else {
            applications = getApplicationsByOrgNumberRestricted(orgNumber, pageable);
        }


        if (applications.isEmpty() && companyRepository.findBySearchKeyOrgNumber(
                hmacHasher.encode(orgNumber)).isEmpty()) {
            throw new NoSuchElementException("No company with organisation number " + orgNumber);
        }
        return applications;
    }

    private Page<CreditApplicationDataObject> getApplicationsByOrgNumberAll(
            String orgNumber, Pageable pageable) {

        String key = "org-" + orgNumber + "-" + pageable;

        return cacheManager.getCache("certifiedAccessApplications")
                .get(key, () -> {
                    byte[] hash = hmacHasher.encode(orgNumber);

                    return applicationRepository
                            .findByCompany_SearchKeyOrgNumberOrderByCreatedAtDesc(hash, pageable)
                            .map(CreditApplicationProtectedDTO::new);
                });
    }

    private Page<CreditApplicationDataObject> getApplicationsByOrgNumberRestricted(
            String orgNumber, Pageable pageable) {

        String key = "org-" + orgNumber + "-" + pageable;

        return cacheManager.getCache("restrictedAccessApplications")
                .get(key, () -> {
                    byte[] hash = hmacHasher.encode(orgNumber);

                    return applicationRepository
                            .findByCompany_SearchKeyOrgNumberAndProtectedResourceFalseOrderByCreatedAtDesc(
                                    hash, pageable)
                            .map(CreditApplicationDTO::new);
                });
    }

    // Kör om scoring på ett ärende som redan finns och uppdaterar det
    @Transactional
    @CacheEvict(cacheNames = {"restrictedAccessApplications", "certifiedAccessApplications"}, allEntries = true)
    public CreditApplicationDTO rescoreApplication(Long applicationId) {
        CreditApplication application = applicationRepository.findById(applicationId).orElseThrow();

        if(!accessPolicy.caseworkerCanReadResource(application)){
            throw new NoSuchElementException("No application with id " + applicationId);
        }


        // Om en handläggare redan fattat ett beslut så får vi inte skriva över det
        // Tittar på auditloggen och inte på status, eftersom scoring också sätter APPROVED/REJECTED själv och
        // de automatiska besluten vill vi att man ska kunna köra om
        if (auditService.hasManualDecision(applicationId)) {
            throw new IllegalArgumentException((
                    "Application " + applicationId + " has a manual decision and can not be re-scored"));
        }
        //Väntar ärendet på dokument från kunden ska vi inte köra om scoring, annars flyttas statusen bort från
        // PENDING_DOCS och kunden slutar se vad som saknas
        if (application.getStatus() == ApplicationStatus.PENDING_DOCS) {
            throw new IllegalArgumentException(
                    "Application " + applicationId + " is waiting for documents and can not be re-scored");
        }

        Company company = application.getCompany();

        // Hämtar ny årsredovisning, är registret nere blir det 503 och ärendet rullas tillbaka
        CompanyFinancialApiDTO financials = financialService
                .fetchLatestAnnualReport(company.getOrg_number())
                .orElseThrow();

        // Bransch ska hämtas från validation service
        NewApplicationDTO rescored = scoringService.scoreFromFinancialObject(
                financials, application.getRequestedAmount(), "bransch",
                company.getOrg_number(), company.getCompany_name(),
                company.getAuthorized_signatory(),
                application.getPurpose(), application.getDurationMonths());

        //Kom ihåg statusen före omkörningen
        ApplicationStatus previousStatus = application.getStatus();
        // Skriver de nya resultatet på det befintliga ärendet, alltså skapas inget nytt
        // Ingen save() behövs då ärendet redan hanteras av JPA inuti @Transactional
        application.setStatus(signingService.resolveApprovalStatus(application, rescored.status()));
        application.setDecision(rescored.decision());
        application.setDecisionReason(rescored.decision_reason());
        application.setScoringResult(rescored.scoring_result());

        auditService.scoringRun(application, rescored.flagCount());

        if (application.getStatus() != previousStatus) {
            eventPublisher.publishEvent(new ApplicationStatusChangedEvent(
                    application.getId(), application.getContactEmail(),
                    application.getStatus()));
        }

        return new CreditApplicationDTO(application);
    }

    public CreditApplicationDataObject findApplicationByID (Long id){
        CreditApplication app = applicationRepository.findById(id).orElseThrow();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if(app.isProtectedResource()){
            if(!accessPolicy.canViewSensitiveData(app.getCompany().getOrg_number())){
                throw new NoSuchElementException("No application with id " + id );
            }
            return new CreditApplicationProtectedDTO(app);
        }
        return new CreditApplicationDTO(app);
    }

    //Currently tests only
    public List<CreditApplicationDataObject> readApplicationsByCompany(Long companyID){
        return applicationRepository.findByCompanyId(companyID)
                .stream()
                .filter(application ->
                        accessPolicy.canViewSensitiveData(application.getCompany().getOrg_number())
                                || !application.isProtectedResource())
                .map(application -> application.isProtectedResource()
                        ? new CreditApplicationProtectedDTO(application)
                        : new CreditApplicationDTO(application))
                .map(dto->(CreditApplicationDataObject) dto)
                .toList();
    }

    //currently tests only
    public List<CreditApplicationDataObject> readApplicationsByCompanyDesc(Long companyID){

        return applicationRepository.findByCompanyIdOrderByCreatedAtDesc(companyID)
                .stream()
                .filter(application ->
                        accessPolicy.canViewSensitiveData(application.getCompany().getOrg_number())
                                || !application.isProtectedResource())
                .map(application -> application.isProtectedResource()
                        ? new CreditApplicationProtectedDTO(application)
                        : new CreditApplicationDTO(application))
                .map(dto->(CreditApplicationDataObject) dto)
                .toList();

    }
    public Page<CreditApplicationDataObject> readApplicationsByCompanyDesc(Long companyID, Pageable pageable) {

        Page<CreditApplication> fetchedApps;
        if(accessPolicy.canViewSensitiveData(companyRepository.findById(companyID).orElseThrow().getOrg_number())){
            fetchedApps = applicationsByCompanyAll( companyID, pageable);
        } else {
            fetchedApps = applicationsByCompanyRestricted( companyID, pageable);
        }

        return fetchedApps.map(application -> application.isProtectedResource()
                ? new CreditApplicationProtectedDTO(application)
                : new CreditApplicationDTO(application));
    }

    private Page<CreditApplication> applicationsByCompanyAll(
            Long companyID, Pageable pageable) {

        return cacheManager.getCache("certifiedAccessApplications")
                .get("Company-" + companyID + "-" + pageable,
                        () -> applicationRepository
                                .findByCompanyIdOrderByCreatedAtDesc(companyID, pageable));
    }

    private Page<CreditApplication> applicationsByCompanyRestricted(
            Long companyID, Pageable pageable) {

        return cacheManager.getCache("restrictedAccessApplications")
                .get("Company-" + companyID + "-" + pageable,
                        () -> applicationRepository
                                .findByCompanyIdAndProtectedResourceFalseOrderByCreatedAtDesc(
                                        companyID, pageable));
    }

    public List<ApplicationCommentDTO> findComments(Long applicationId) {

        CreditApplication app = applicationRepository.findById(applicationId).orElseThrow();

        boolean canView = accessPolicy.canViewSensitiveData(app.getCompany().getOrg_number());

        if(app.isProtectedResource() && !canView){
            throw new NoSuchElementException("No application for that ID");
        }
        return auditService.findComments(applicationId);
    }





}
