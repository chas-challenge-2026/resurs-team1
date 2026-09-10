package se.comerit.resurs;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;
import se.comerit.resurs.dto.CreditApplicationDTO;
import se.comerit.resurs.dto.application.NewApplicationDTO;
import se.comerit.resurs.enums.ApplicationStatus;
import se.comerit.resurs.persistence.AuditEventRepository;
import se.comerit.resurs.persistence.CompanyRepository;
import se.comerit.resurs.persistence.CreditApplicationRepository;
import se.comerit.resurs.persistence.model.Company;
import se.comerit.resurs.persistence.model.CreditApplication;
import se.comerit.resurs.service.ApplicationService;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Testcontainers
class ApplicationServiceTests {

    private static final Path SEED_SQL = Paths.get("").toAbsolutePath()
            .resolve("../../infra/seed.sql")
            .normalize();

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("resurs_test")
                    .withUsername("test")
                    .withPassword("test")
                    .withCopyFileToContainer(
                            MountableFile.forHostPath(SEED_SQL),
                            "/docker-entrypoint-initdb.d/seed.sql"
                    );

    @Autowired
    AuditEventRepository auditEventRepository;

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private CreditApplicationRepository applicationRepository;

    private Company testCompany;

    @BeforeEach
    void setUp() {
        auditEventRepository.deleteAll();
        applicationRepository.deleteAll();
        companyRepository.deleteAll();

        testCompany = new Company();

        testCompany.setOrg_number("556677-8899");

        testCompany = companyRepository.save(testCompany);
    }

    // ============================================================
    // saveApplication()
    // ============================================================

    @Test
    void saveApplication_shouldPersistApplication() {

        CreditApplicationDTO saved = applicationService.saveApplication(createApplicationDTO());


        assertThat(saved).isNotNull();
        assertThat(saved.id()).isNotNull();


        assertThat(saved.org_number())
                .isEqualTo(testCompany.getOrg_number());

        assertThat(saved.requested_amount())
                .isEqualByComparingTo(new BigDecimal("250000"));

        assertThat(saved.purpose())
                .isEqualTo("Expansion");

        assertThat(saved.status())
                .isEqualTo(ApplicationStatus.PENDING_DOCS);
    }

    @Test
    void saveApplication_shouldCopyApplicationValues() {

        NewApplicationDTO dto = createApplicationDTO();

        CreditApplicationDTO saved = applicationService.saveApplication(dto);

        assertThat(saved.purpose())
                .isEqualTo(dto.purpose());

        assertThat(saved.requested_amount())
                .isEqualByComparingTo(dto.requested_amount());

        assertThat(saved.status())
                .isEqualTo(dto.status());

        assertThat(saved.decision())
                .isEqualTo(dto.decision());

        assertThat(saved.decision_reason())
                .isEqualTo(dto.decision_reason());

        assertThat(saved.scoring_result())
                .isEqualTo(dto.scoring_result());
    }

    /*
    @Test
    void saveApplication_shouldCreateAuditLog() {

        NewApplicationDTO dto = createApplicationDTO();

        CreditApplicationDTO saved = applicationService.saveApplication(dto);

        String auditLog = saved.authorized_signatory();

        assertThat(auditLog)
                .isNotBlank();

        assertThat(auditLog)
                .contains("\"action\":\"APPLICATION_CREATED\"");

        assertThat(auditLog)
                .contains("\"action\":\"SCORING_RUN\"");

        assertThat(auditLog)
                .contains("\"orgNumber\":\"556677-8899\"");

        assertThat(auditLog)
                .contains("\"result\":\"" + dto.decision() + "\"");

        assertThat(auditLog)
                .contains("\"flags\":" + dto.flagCount());
    }


    @Test
    void saveApplication_shouldPersistAuditLogToDatabase() {
//byt detta test med
        NewApplicationDTO dto = createApplicationDTO();

        CreditApplicationDTO saved = applicationService.saveApplication(dto);

        CreditApplication fromDatabase =
                applicationRepository.findById(saved.id()).orElseThrow();

        assertThat(fromDatabase.getPurpose())
                .contains("\"action\":\"APPLICATION_CREATED\"");

        assertThat(fromDatabase.getScoringResult())
                .contains("\"action\":\"SCORING_RUN\"");
    }
*/
    @Test
    void saveApplication_shouldAssociateApplicationWithCorrectCompany() {
//gör om med
        Company anotherCompany = new Company();

        anotherCompany.setOrg_number("111111-2222");

        anotherCompany = companyRepository.save(anotherCompany);

        NewApplicationDTO dto = newApplicationDTO(
                "111111-2222",
                "Other company"
        );

        CreditApplicationDTO saved =
                applicationService.saveApplication(dto);

        assertThat(saved.org_number())
                .isEqualTo("111111-2222");
    }

    @Test
    void saveApplication_shouldThrowWhenCompanyDoesNotExist() {
//kolla över
        NewApplicationDTO dto = newApplicationDTO(
                "999999-9999",
                "Nonexistent Company"
        );

        assertThatThrownBy(() ->
                applicationService.saveApplication(dto)
        ).isInstanceOf(Exception.class);

        assertThat(applicationRepository.count())
                .isZero();
    }

    // ============================================================
    // findApplicationByID()
    // ============================================================

    @Test
    void findApplicationByID_shouldReturnApplication() {
//kolla över
        CreditApplicationDTO saved =
                applicationService.saveApplication(createApplicationDTO());

        CreditApplicationDTO result =
                applicationService.findApplicationByID(saved.id());

        assertThat(result).isNotNull();
        assertThat(result.id())
                .isEqualTo(saved.id());

        assertThat(result.purpose())
                .isEqualTo("Expansion");

        assertThat(result.requested_amount())
                .isEqualByComparingTo(new BigDecimal("250000"));
    }

    @Test
    void findApplicationByID_shouldThrowWhenApplicationDoesNotExist() {
//kolla över
        assertThatThrownBy(() ->
                applicationService.findApplicationByID(999999L)
        ).isInstanceOf(Exception.class);
    }

    // ============================================================
    // readApplicationsByCompany()
    // ============================================================

    @Test
    void readApplicationsByCompany_shouldReturnOnlyCompanyApplications() {
//kolla över
        Company anotherCompany = new Company();

        anotherCompany.setOrg_number("111111-2222");

        anotherCompany = companyRepository.save(anotherCompany);

        applicationService.saveApplication(
                newApplicationDTO(
                        "556677-8899",
                        "Test Company"
                )
        );

        applicationService.saveApplication(
                newApplicationDTO(
                        "556677-8899",
                        "Test Company"
                )
        );

        applicationService.saveApplication(
                newApplicationDTO(
                        "111111-2222",
                        "Other Company"
                )
        );

        List<CreditApplicationDTO> applications =
                applicationService.readApplicationsByCompany(
                        testCompany.getId()
                );

        assertThat(applications)
                .hasSize(2);
    }

    @Test
    void readApplicationsByCompany_shouldReturnEmptyListWhenCompanyHasNoApplications() {
//kolla över
        List<CreditApplicationDTO> applications =
                applicationService.readApplicationsByCompany(
                        testCompany.getId()
                );

        assertThat(applications)
                .isEmpty();
    }

    // ============================================================
    // readApplicationsByCompanyDesc()
    // ============================================================

    @Test
    void readApplicationsByCompanyDesc_shouldReturnCompanyApplications() {
//kolla över
        applicationService.saveApplication(
                createApplicationDTO()
        );

        applicationService.saveApplication(
                createApplicationDTO()
        );

        List<CreditApplicationDTO> applications =
                applicationService.readApplicationsByCompanyDesc(
                        testCompany.getId()
                );

        assertThat(applications)
                .hasSize(2);
    }

    // ============================================================
    // readApplicationsByCompanyDesc(companyID, Pageable)
    // ============================================================

    @Test
    void readApplicationsByCompanyDesc_shouldRespectPageSize() {
//kolla över
        applicationService.saveApplication(createApplicationDTO());
        applicationService.saveApplication(createApplicationDTO());
        applicationService.saveApplication(createApplicationDTO());

        Pageable pageable = PageRequest.of(0, 2);

        List<CreditApplicationDTO> applications =
                applicationService.readApplicationsByCompanyDesc(
                        testCompany.getId(),
                        pageable
                );

        assertThat(applications)
                .hasSize(2);
    }

    @Test
    void readApplicationsByCompanyDesc_shouldReturnNewestFirst() {
//kolla över
        CreditApplicationDTO first =
                applicationService.saveApplication(createApplicationDTO());

        CreditApplicationDTO second =
                applicationService.saveApplication(createApplicationDTO());

        List<CreditApplicationDTO> applications =
                applicationService.readApplicationsByCompanyDesc(
                        testCompany.getId(),
                        PageRequest.of(0, 10)
                );

        assertThat(applications)
                .hasSize(2);

        assertThat(applications.get(0).id())
                .isEqualTo(second.id());

        assertThat(applications.get(1).id())
                .isEqualTo(first.id());
    }

    // ============================================================
    // Test data
    // ============================================================

    private NewApplicationDTO createApplicationDTO() {
        return newApplicationDTO(
                "556677-8899",
                "Test Company"
        );
    }

    private NewApplicationDTO newApplicationDTO(
            String orgNumber,
            String companyName
    ) {
        return new NewApplicationDTO(
                new BigDecimal("250000"),     // requested_amount
                "Expansion",                  // purpose
                ApplicationStatus.PENDING_DOCS, // status
                "APPROVED",                   // decision
                "Test decision reason",       // decision_reason
                "Test scoring result",       // scoring_result
                companyName,                  // company_name
                orgNumber,                    // org_number
                "Test Person",                // authorized_signatory
                3                            // flagCount
        );
    }
}

