package se.comerit.resurs;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;
import se.comerit.resurs.dto.ContactDetails;
import se.comerit.resurs.dto.CreditApplicationDTO;
import se.comerit.resurs.dto.CreditApplicationDataObject;
import se.comerit.resurs.dto.application.NewApplicationDTO;
import se.comerit.resurs.dto.backoffice.BackOfficeListsDTO;
import se.comerit.resurs.dto.companyvalidation.CompanyValidationApiDTO;
import se.comerit.resurs.enums.ApplicationStatus;
import se.comerit.resurs.enums.SigningRight;
import se.comerit.resurs.persistence.ApplicationSignatureRepository;
import se.comerit.resurs.persistence.AuditEventRepository;
import se.comerit.resurs.persistence.CompanyRepository;
import se.comerit.resurs.persistence.CreditApplicationRepository;
import se.comerit.resurs.persistence.DocumentRepository;
import se.comerit.resurs.service.ApplicationService;
import se.comerit.resurs.service.BackofficeService;
import se.comerit.resurs.service.DocumentService;
import se.comerit.resurs.util.HMACHasher;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@Testcontainers
class ApplicationListCacheIntegrationTest {

    private static final Path SEED_SQL = Paths.get("").toAbsolutePath()
            .resolve("../../infra/seed.sql")
            .normalize();

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withCopyFileToContainer(MountableFile.forHostPath(SEED_SQL), "/docker-entrypoint-initdb.d/seed.sql");

    private static final String APPLICATION_LISTS = "applicationLists";
    private static final String ORG_NUMBER = "556677-8899";
    private static final String OTHER_ORG_NUMBER = "111111-2222";
    private static final String RESCORABLE_ORG_NUMBER = "556000-7777";
    private static final String WORKER_EMAIL = "karin@resurs.se";
    private static final String WORKER_NAME = "Karin";
    private static final ContactDetails CONTACTS = new ContactDetails("Chunky", "Chunk@Jungle.DKI", "000999999");
    private static final CompanyValidationApiDTO.Signatory SIGNATORY =
            new CompanyValidationApiDTO.Signatory("198001011234", "Chunky", "VD", SigningRight.ALONE);
    private static final Pageable FIRST_PAGE = PageRequest.of(0, 10);

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private BackofficeService backofficeService;

    @Autowired
    private DocumentService documentService;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private ApplicationSignatureRepository signatureRepository;

    @Autowired
    @MockitoSpyBean
    private CreditApplicationRepository applicationRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private HMACHasher hmacHasher;

    @BeforeEach
    void setUp() {
        auditEventRepository.deleteAll();
        documentRepository.deleteAll();
        signatureRepository.deleteAll();
        applicationRepository.deleteAll();
        companyRepository.deleteAll();
        applicationLists().clear();
    }

    @Test
    @WithMockUser(authorities = {"ROLE_CASEWORKER"})
    void readApplicationsByCompanyDesc_calledTwice_fetchesApplicationsOnlyOnce() {
        Long companyId = companyIdOf(save(ORG_NUMBER, ApplicationStatus.UNDER_REVIEW));

        Page<CreditApplicationDataObject> first =
                applicationService.readApplicationsByCompanyDesc(companyId, FIRST_PAGE);

        Page<CreditApplicationDataObject> second =
                applicationService.readApplicationsByCompanyDesc(companyId, FIRST_PAGE);

        assertThat(first.getContent()).hasSize(1);
        assertThat(second.getContent()).hasSize(1);

        verify(applicationRepository, times(1))
                .findByCompanyIdAndProtectedResourceFalseOrderByCreatedAtDesc(
                        companyId, FIRST_PAGE);
    }

    @Test
    void readApplicationsByCompanyDesc_differentPages_returnDifferentApplications() {
        CreditApplicationDataObject older = save(ORG_NUMBER, ApplicationStatus.UNDER_REVIEW);
        CreditApplicationDataObject newer = save(ORG_NUMBER, ApplicationStatus.UNDER_REVIEW);
        Long companyId = companyIdOf(newer);

        Page<CreditApplicationDataObject> firstPage = applicationService.readApplicationsByCompanyDesc(companyId, PageRequest.of(0, 1));
        Page<CreditApplicationDataObject> secondPage = applicationService.readApplicationsByCompanyDesc(companyId, PageRequest.of(1, 1));

        assertThat(firstPage).extracting(CreditApplicationDataObject::id).containsExactly(newer.id());
        assertThat(secondPage).extracting(CreditApplicationDataObject::id).containsExactly(older.id());
    }

    @Test
    void readApplicationsByCompanyDesc_differentCompanies_returnOwnApplications() {
        CreditApplicationDataObject own = save(ORG_NUMBER, ApplicationStatus.UNDER_REVIEW);
        CreditApplicationDataObject other = save(OTHER_ORG_NUMBER, ApplicationStatus.UNDER_REVIEW);

        Page<CreditApplicationDataObject> ownList = applicationService.readApplicationsByCompanyDesc(companyIdOf(own), FIRST_PAGE);
        Page<CreditApplicationDataObject> otherList = applicationService.readApplicationsByCompanyDesc(companyIdOf(other), FIRST_PAGE);

        assertThat(ownList).extracting(CreditApplicationDataObject::id).containsExactly(own.id());
        assertThat(otherList).extracting(CreditApplicationDataObject::id).containsExactly(other.id());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_CASEWORKER"})
    void getApplicationsByOrgNumber_calledTwice_fetchesApplicationsOnlyOnce() {
        save(ORG_NUMBER, ApplicationStatus.UNDER_REVIEW);

        Page<CreditApplicationDataObject> first =
                applicationService.getApplicationsByOrgNumber(ORG_NUMBER, FIRST_PAGE);

        Page<CreditApplicationDataObject> second =
                applicationService.getApplicationsByOrgNumber(ORG_NUMBER, FIRST_PAGE);

        assertThat(first.getContent()).hasSize(1);
        assertThat(second.getContent()).hasSize(1);

        verify(applicationRepository, times(1))
                .findByCompany_SearchKeyOrgNumberAndProtectedResourceFalseOrderByCreatedAtDesc(
                        hmacHasher.encode(ORG_NUMBER), FIRST_PAGE);
    }

    @Test
    void applicationsForReview_calledTwice_fetchesApplicationsOnlyOnce() {
        save(ORG_NUMBER, ApplicationStatus.UNDER_REVIEW);

        BackOfficeListsDTO first =
                backofficeService.applicationsForReview(FIRST_PAGE, FIRST_PAGE);

        BackOfficeListsDTO second =
                backofficeService.applicationsForReview(FIRST_PAGE, FIRST_PAGE);

        assertThat(first).isNotNull();
        assertThat(second).isNotNull();

        verify(applicationRepository, times(1))
                .findByStatusInAndProtectedResourceFalseOrderByCreatedAtAsc(
                        List.of(
                                ApplicationStatus.UNDER_REVIEW,
                                ApplicationStatus.PENDING_SCORING,
                                ApplicationStatus.PENDING_DOCS
                        ),
                        FIRST_PAGE
                );

        verify(applicationRepository, times(1))
                .findByStatusInAndProtectedResourceFalseOrderByCreatedAtAsc(
                        List.of(
                                ApplicationStatus.APPROVED,
                                ApplicationStatus.REJECTED
                        ),
                        FIRST_PAGE
                );

    }

    @Test
    void saveApplication_evictsCachedLists_soNewApplicationIsListed() {
        CreditApplicationDataObject existing = save(ORG_NUMBER, ApplicationStatus.UNDER_REVIEW);
        Long companyId = companyIdOf(existing);
        applicationService.readApplicationsByCompanyDesc(companyId, FIRST_PAGE);

        CreditApplicationDataObject added = save(ORG_NUMBER, ApplicationStatus.UNDER_REVIEW);

        assertThat(applicationService.readApplicationsByCompanyDesc(companyId, FIRST_PAGE))
                .extracting(CreditApplicationDataObject::id)
                .containsExactlyInAnyOrder(existing.id(), added.id());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_CASEWORKER"})
    void rescoreApplication_evictsCachedLists() {
        CreditApplicationDataObject application = save(RESCORABLE_ORG_NUMBER, ApplicationStatus.UNDER_REVIEW);
        Long companyId = companyIdOf(application);
        applicationService.readApplicationsByCompanyDesc(companyId, FIRST_PAGE);

        applicationService.rescoreApplication(application.id());

        assertThat(cachedCompanyList(companyId)).isNull();
    }

    @Test
    @WithMockUser(authorities = {"ROLE_CASEWORKER"})
    void applicationDecision_evictsCachedLists_soNewStatusIsListed() {
        CreditApplicationDataObject application = save(ORG_NUMBER, ApplicationStatus.UNDER_REVIEW);
        Long companyId = companyIdOf(application);
        applicationService.readApplicationsByCompanyDesc(companyId, FIRST_PAGE);

        backofficeService.application_decision(application.id(), ApplicationStatus.APPROVED, WORKER_EMAIL, WORKER_NAME, null);

        assertThat(applicationService.readApplicationsByCompanyDesc(companyId, FIRST_PAGE))
                .extracting(CreditApplicationDataObject::status)
                .containsExactly(ApplicationStatus.APPROVED);
    }


    @Test
    @WithMockUser(authorities = {"ROLE_CASEWORKER"})
    void applicationRequestDocuments_evictsCachedLists_soPendingDocsIsListed() {
        CreditApplicationDataObject application = save(ORG_NUMBER, ApplicationStatus.UNDER_REVIEW);
        Long companyId = companyIdOf(application);
        applicationService.readApplicationsByCompanyDesc(companyId, FIRST_PAGE);

        backofficeService.application_request_documents(application.id(), "Skicka balansräkning", WORKER_EMAIL, WORKER_NAME);

        assertThat(applicationService.readApplicationsByCompanyDesc(companyId, FIRST_PAGE))
                .extracting(CreditApplicationDataObject::status)
                .containsExactly(ApplicationStatus.PENDING_DOCS);
    }

    @Test
    void uploadDocument_evictsCachedLists_soUnderReviewIsListed() {
        CreditApplicationDataObject application = save(ORG_NUMBER, ApplicationStatus.PENDING_DOCS);
        Long companyId = companyIdOf(application);
        applicationService.readApplicationsByCompanyDesc(companyId, FIRST_PAGE);
        MockMultipartFile file = new MockMultipartFile("file", "balansrakning.pdf",
                "application/pdf", "pdf-content".getBytes());

        documentService.uploadDocument(application.id(), "balansrakning", file);

        assertThat(applicationService.readApplicationsByCompanyDesc(companyId, FIRST_PAGE))
                .extracting(CreditApplicationDataObject::status)
                .containsExactly(ApplicationStatus.PENDING_DOCS);
    }

    @Test
    @WithMockUser(authorities = {"ROLE_CASEWORKER"})
    void applicationDecision_evictsBackofficeLists() {
        CreditApplicationDataObject application = save(ORG_NUMBER, ApplicationStatus.UNDER_REVIEW);
        BackOfficeListsDTO before = backofficeService.applicationsForReview(FIRST_PAGE, FIRST_PAGE);

        backofficeService.application_decision(application.id(), ApplicationStatus.REJECTED, WORKER_EMAIL, WORKER_NAME, null);

        assertThat(backofficeService.applicationsForReview(FIRST_PAGE, FIRST_PAGE)).isNotSameAs(before);
    }

    private CreditApplicationDataObject save(String orgNumber, ApplicationStatus status) {
        return applicationService.saveApplication(newApplication(orgNumber, status), CONTACTS, false, SIGNATORY);
    }

    private Long companyIdOf(CreditApplicationDataObject application) {
        return applicationRepository.findById(application.id()).orElseThrow().getCompany().getId();
    }

    private Cache applicationLists() {
        return cacheManager.getCache(APPLICATION_LISTS);
    }

    private Cache.ValueWrapper cachedCompanyList(Long companyId) {
        return applicationLists().get("company-" + companyId + "-" + FIRST_PAGE);
    }

    private NewApplicationDTO newApplication(String orgNumber, ApplicationStatus status) {
        return new NewApplicationDTO(
                new BigDecimal("250000"),
                "Expansion",
                12,
                status,
                "APPROVED",
                "Test decision reason",
                "Test scoring result",
                "Test Company",
                orgNumber,
                "Test Person",
                0
        );
    }
}
