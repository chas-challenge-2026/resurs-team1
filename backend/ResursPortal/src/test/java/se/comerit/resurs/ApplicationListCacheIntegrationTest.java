package se.comerit.resurs;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;
import se.comerit.resurs.dto.ContactDetails;
import se.comerit.resurs.dto.CreditApplicationDTO;
import se.comerit.resurs.dto.application.NewApplicationDTO;
import se.comerit.resurs.dto.backoffice.BackOfficeListsDTO;
import se.comerit.resurs.enums.ApplicationStatus;
import se.comerit.resurs.persistence.AuditEventRepository;
import se.comerit.resurs.persistence.CompanyRepository;
import se.comerit.resurs.persistence.CreditApplicationRepository;
import se.comerit.resurs.persistence.DocumentRepository;
import se.comerit.resurs.service.ApplicationService;
import se.comerit.resurs.service.BackofficeService;
import se.comerit.resurs.service.DocumentService;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

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
    private CreditApplicationRepository applicationRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @BeforeEach
    void setUp() {
        auditEventRepository.deleteAll();
        documentRepository.deleteAll();
        applicationRepository.deleteAll();
        companyRepository.deleteAll();
        applicationLists().clear();
    }

    @Test
    void readApplicationsByCompanyDesc_calledTwice_returnsCachedList() {
        Long companyId = companyIdOf(save(ORG_NUMBER, ApplicationStatus.UNDER_REVIEW));

        List<CreditApplicationDTO> first = applicationService.readApplicationsByCompanyDesc(companyId, FIRST_PAGE);
        List<CreditApplicationDTO> second = applicationService.readApplicationsByCompanyDesc(companyId, FIRST_PAGE);

        assertThat(second).isSameAs(first);
    }

    @Test
    void readApplicationsByCompanyDesc_differentPages_returnDifferentApplications() {
        CreditApplicationDTO older = save(ORG_NUMBER, ApplicationStatus.UNDER_REVIEW);
        CreditApplicationDTO newer = save(ORG_NUMBER, ApplicationStatus.UNDER_REVIEW);
        Long companyId = companyIdOf(newer);

        List<CreditApplicationDTO> firstPage = applicationService.readApplicationsByCompanyDesc(companyId, PageRequest.of(0, 1));
        List<CreditApplicationDTO> secondPage = applicationService.readApplicationsByCompanyDesc(companyId, PageRequest.of(1, 1));

        assertThat(firstPage).extracting(CreditApplicationDTO::id).containsExactly(newer.id());
        assertThat(secondPage).extracting(CreditApplicationDTO::id).containsExactly(older.id());
    }

    @Test
    void readApplicationsByCompanyDesc_differentCompanies_returnOwnApplications() {
        CreditApplicationDTO own = save(ORG_NUMBER, ApplicationStatus.UNDER_REVIEW);
        CreditApplicationDTO other = save(OTHER_ORG_NUMBER, ApplicationStatus.UNDER_REVIEW);

        List<CreditApplicationDTO> ownList = applicationService.readApplicationsByCompanyDesc(companyIdOf(own), FIRST_PAGE);
        List<CreditApplicationDTO> otherList = applicationService.readApplicationsByCompanyDesc(companyIdOf(other), FIRST_PAGE);

        assertThat(ownList).extracting(CreditApplicationDTO::id).containsExactly(own.id());
        assertThat(otherList).extracting(CreditApplicationDTO::id).containsExactly(other.id());
    }

    @Test
    void getApplicationsByOrgNumber_calledTwice_returnsCachedList() {
        save(ORG_NUMBER, ApplicationStatus.UNDER_REVIEW);

        List<CreditApplicationDTO> first = applicationService.getApplicationsByOrgNumber(ORG_NUMBER, FIRST_PAGE);
        List<CreditApplicationDTO> second = applicationService.getApplicationsByOrgNumber(ORG_NUMBER, FIRST_PAGE);

        assertThat(second).isSameAs(first);
    }

    @Test
    void applicationsForReview_calledTwice_returnsCachedLists() {
        save(ORG_NUMBER, ApplicationStatus.UNDER_REVIEW);

        BackOfficeListsDTO first = backofficeService.applicationsForReview(FIRST_PAGE, FIRST_PAGE);
        BackOfficeListsDTO second = backofficeService.applicationsForReview(FIRST_PAGE, FIRST_PAGE);

        assertThat(second).isSameAs(first);
    }

    @Test
    void saveApplication_evictsCachedLists_soNewApplicationIsListed() {
        CreditApplicationDTO existing = save(ORG_NUMBER, ApplicationStatus.UNDER_REVIEW);
        Long companyId = companyIdOf(existing);
        applicationService.readApplicationsByCompanyDesc(companyId, FIRST_PAGE);

        CreditApplicationDTO added = save(ORG_NUMBER, ApplicationStatus.UNDER_REVIEW);

        assertThat(applicationService.readApplicationsByCompanyDesc(companyId, FIRST_PAGE))
                .extracting(CreditApplicationDTO::id)
                .containsExactlyInAnyOrder(existing.id(), added.id());
    }

    @Test
    void rescoreApplication_evictsCachedLists() {
        CreditApplicationDTO application = save(RESCORABLE_ORG_NUMBER, ApplicationStatus.UNDER_REVIEW);
        Long companyId = companyIdOf(application);
        applicationService.readApplicationsByCompanyDesc(companyId, FIRST_PAGE);

        applicationService.rescoreApplication(application.id());

        assertThat(cachedCompanyList(companyId)).isNull();
    }

    @Test
    void applicationDecision_evictsCachedLists_soNewStatusIsListed() {
        CreditApplicationDTO application = save(ORG_NUMBER, ApplicationStatus.UNDER_REVIEW);
        Long companyId = companyIdOf(application);
        applicationService.readApplicationsByCompanyDesc(companyId, FIRST_PAGE);

        backofficeService.application_decision(application.id(), ApplicationStatus.APPROVED, WORKER_EMAIL, WORKER_NAME, null);

        assertThat(applicationService.readApplicationsByCompanyDesc(companyId, FIRST_PAGE))
                .extracting(CreditApplicationDTO::status)
                .containsExactly(ApplicationStatus.APPROVED);
    }

    @Test
    void applicationComment_evictsCachedLists_soNewCommentIsListed() {
        CreditApplicationDTO application = save(ORG_NUMBER, ApplicationStatus.UNDER_REVIEW);
        Long companyId = companyIdOf(application);
        applicationService.readApplicationsByCompanyDesc(companyId, FIRST_PAGE);

        backofficeService.application_comment(application.id(), "Ny kommentar", WORKER_EMAIL, WORKER_NAME);

        assertThat(applicationService.readApplicationsByCompanyDesc(companyId, FIRST_PAGE))
                .extracting(CreditApplicationDTO::comment)
                .containsExactly("Ny kommentar");
    }

    @Test
    void applicationRequestDocuments_evictsCachedLists_soPendingDocsIsListed() {
        CreditApplicationDTO application = save(ORG_NUMBER, ApplicationStatus.UNDER_REVIEW);
        Long companyId = companyIdOf(application);
        applicationService.readApplicationsByCompanyDesc(companyId, FIRST_PAGE);

        backofficeService.application_request_documents(application.id(), "Skicka balansräkning", WORKER_EMAIL, WORKER_NAME);

        assertThat(applicationService.readApplicationsByCompanyDesc(companyId, FIRST_PAGE))
                .extracting(CreditApplicationDTO::status)
                .containsExactly(ApplicationStatus.PENDING_DOCS);
    }

    @Test
    void uploadDocument_evictsCachedLists_soUnderReviewIsListed() {
        CreditApplicationDTO application = save(ORG_NUMBER, ApplicationStatus.PENDING_DOCS);
        Long companyId = companyIdOf(application);
        applicationService.readApplicationsByCompanyDesc(companyId, FIRST_PAGE);
        MockMultipartFile file = new MockMultipartFile("file", "balansrakning.pdf",
                "application/pdf", "pdf-content".getBytes());

        documentService.uploadDocument(application.id(), "balansrakning", file);

        assertThat(applicationService.readApplicationsByCompanyDesc(companyId, FIRST_PAGE))
                .extracting(CreditApplicationDTO::status)
                .containsExactly(ApplicationStatus.UNDER_REVIEW);
    }

    @Test
    void applicationDecision_evictsBackofficeLists() {
        CreditApplicationDTO application = save(ORG_NUMBER, ApplicationStatus.UNDER_REVIEW);
        BackOfficeListsDTO before = backofficeService.applicationsForReview(FIRST_PAGE, FIRST_PAGE);

        backofficeService.application_decision(application.id(), ApplicationStatus.REJECTED, WORKER_EMAIL, WORKER_NAME, null);

        assertThat(backofficeService.applicationsForReview(FIRST_PAGE, FIRST_PAGE)).isNotSameAs(before);
    }

    private CreditApplicationDTO save(String orgNumber, ApplicationStatus status) {
        return applicationService.saveApplication(newApplication(orgNumber, status), CONTACTS, false);
    }

    private Long companyIdOf(CreditApplicationDTO application) {
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
