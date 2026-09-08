package se.comerit.resurs;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import se.comerit.resurs.dto.auth.CaseWorkerLoginResponse;
import se.comerit.resurs.dto.auth.CompanyLoginResponse;
import se.comerit.resurs.exception.auth.LoginFailedException;
import se.comerit.resurs.exception.auth.LoginFailureReason;
import se.comerit.resurs.persistence.CaseWorkerRepository;
import se.comerit.resurs.persistence.CompanyRepository;
import se.comerit.resurs.persistence.CreditApplicationRepository;
import se.comerit.resurs.persistence.DocumentRepository;
import se.comerit.resurs.persistence.model.CaseWorker;
import se.comerit.resurs.persistence.model.Company;
import se.comerit.resurs.service.AuthService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;


import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.HexFormat;


@Testcontainers
@ActiveProfiles("test")
@SpringBootTest

public class AuthIntegrationTests {

    private static final String WORKER_NAME = "Test Arbetare";
    private static final String EMAIL = "Testarn@resurs.se";
    //icke hashat lösenordet
    private static final String PASSWORD = "password123";
    //samma lösen fast hashat
    private static final String PASSWORD_MD5 = "482c811da5d5b4bc6d497ffa98491e38";

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
                    "/docker-entrypoint-initdb.d/seed.sql");



    @Autowired
    private AuthService authService;
    @Autowired
    private CompanyRepository companyRepo;
    @Autowired
    private CaseWorkerRepository caseWorkerRepo;
    @Autowired
    private CreditApplicationRepository creditRepo;

    @Autowired
    private DocumentRepository documentRepo;


    @BeforeEach
    void cleanDatabase() {
        documentRepo.deleteAll();
        creditRepo.deleteAll();
        companyRepo.deleteAll();
        caseWorkerRepo.deleteAll();
    }

    private Company createCompany(){
        Company company = new Company();
        company.setCompany_name("TestBolaget");
        company.setOrg_number("556000-1234");
         return companyRepo.save(company);

    }

    private CaseWorker createWorker(){
        CaseWorker worker = new CaseWorker();
        worker.setName(WORKER_NAME);
        worker.setEmail(EMAIL);
        worker.setPasswordHash(PASSWORD_MD5);
        return caseWorkerRepo.save(worker);

    }

    @Test
    void companyLogin_shouldReturnCompanyData(){
         Company company = createCompany();
         CompanyLoginResponse response = authService.loginCompany("556000-1234");


        assertThat(response.userId()).isEqualTo(company.getId());
        assertThat(response.role()).isEqualTo("company");
        assertThat(response.orgNumber()).isEqualTo(company.getOrg_number());
        assertThat(response.companyName()).isEqualTo(company.getCompany_name());
    }

    @Test
    void loginCompany_shouldRejectOrgNumberNotApprovedByBankId() {
        createCompany();

        LoginFailedException thrown = assertThrows(
                LoginFailedException.class,
                () -> authService.loginCompany("556000-9999"));

        assertThat(thrown.reason()).isEqualTo(LoginFailureReason.BANKID_REJECTED);
    }
    @Test
    void loginCompany_shouldFailWhenApprovedCompanyIsMissingInDatabase() {
        // 556000-5678 är BankID godkänt men skapas aldrig här
        LoginFailedException thrown = assertThrows(
                LoginFailedException.class,
                () -> authService.loginCompany("556000-5678"));

        assertThat(thrown.reason()).isEqualTo(LoginFailureReason.COMPANY_NOT_FOUND);
    }

    @Test
    void loginCaseWorker_shouldReturnWorkerData() {
        CaseWorker worker =  createWorker();

        CaseWorkerLoginResponse response =
                authService.loginCaseWorker(EMAIL, PASSWORD);

        assertThat(response.userId()).isEqualTo(worker.getId());
        assertThat(response.role()).isEqualTo("caseWorker");
        assertThat(response.name()).isEqualTo(worker.getName());
        assertThat(response.email()).isEqualTo(worker.getEmail());

    }

    @Test
    void loginCaseWorker_shouldRejectWrongPassword() {
        createWorker();

        LoginFailedException thrown = assertThrows(
                LoginFailedException.class,
                () -> authService.loginCaseWorker("Testarn@resurs.se", "fel"));

        assertThat(thrown.reason()).isEqualTo(LoginFailureReason.BAD_CREDENTIALS);
    }

    @Test
    void passwordConstant_shouldBeMd5OfKnownPassword() throws Exception {
        byte[] digest = MessageDigest.getInstance("MD5")
                .digest(PASSWORD.getBytes(StandardCharsets.UTF_8));

        assertThat(HexFormat.of().formatHex(digest)).isEqualTo(PASSWORD_MD5);
    }
}






