package se.comerit.resurs;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;
import se.comerit.resurs.dto.ContactDetails;
import se.comerit.resurs.dto.CreditApplicationDTO;
import se.comerit.resurs.dto.application.ApplicationSubmission;
import se.comerit.resurs.dto.audit.ChainVerificationResponse;
import se.comerit.resurs.dto.auth.CaseWorkerLoginRequest;
import se.comerit.resurs.dto.auth.CaseWorkerLoginResponse;
import se.comerit.resurs.dto.auth.CompanyLoginRequest;
import se.comerit.resurs.dto.auth.CompanyLoginResponse;
import se.comerit.resurs.dto.backoffice.ApplicationCommentRequest;
import se.comerit.resurs.persistence.AuditEventRepository;
import se.comerit.resurs.persistence.model.AuditEvent;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuditSigningIntegrationTest {


    private RestTestClient restTestClient;

    @Autowired
    private JdbcTemplate jdbcTemplate;


    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        restTestClient = RestTestClient
                .bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    private static final Path SEED_SQL = Paths.get("").toAbsolutePath()
            .resolve("../../infra/seed.sql").normalize();

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:12")
            .withCopyFileToContainer(MountableFile.forHostPath(SEED_SQL), "/docker-entrypoint-initdb.d/seed.sql");

    private String persNR = "750312-1234";
    private String orgNR = "556000-1234";
    private String email = "karin@resurs.se";
    private String password = "password123";


    @Test
    void shouldVerifyAuditChainAfterMultipleEvents() {

        String token = csrfToken();
        String sessionToken = loginAsCompany(token,orgNR,persNR);



        // 1. Create a credit application through your existing API.
        ApplicationSubmission newApplication = createApplicationBody();

        CreditApplicationDTO savedApplication = restTestClient.post()
                .uri("/api/application/apply")
                .header("X-XSRF-TOKEN", token)
                .header(HttpHeaders.COOKIE, sessionToken + "; XSRF-TOKEN=" + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(createApplicationBody())
                .exchange()
                .returnResult(CreditApplicationDTO.class)
                .getResponseBody();




        // 2. Verify the initial audit chain.
        ChainVerificationResponse initialResponse =
                restTestClient.get()
                        .uri("/api/audit/{id}", savedApplication.id())
                        .header("X-XSRF-TOKEN", token)
                        .header(HttpHeaders.COOKIE, sessionToken + "; XSRF-TOKEN=" + token)
                        .exchange()
                        .expectStatus().isOk()
                        .expectBody(ChainVerificationResponse.class)
                        .returnResult()
                        .getResponseBody();

        assertThat(initialResponse).isNotNull();




        // 2. login as caseworker + Assert chain as valid

        logout(sessionToken,token);
        sessionToken = loginAsCaseWorker(token,email,password);




        ChainVerificationResponse validAtCreation =
                restTestClient.get()
                        .uri("/api/audit/{id}", savedApplication.id())
                        .header("X-XSRF-TOKEN", token)
                        .header(HttpHeaders.COOKIE, sessionToken + "; XSRF-TOKEN=" + token)
                        .exchange()
                        .expectStatus().isOk()
                        .expectBody(ChainVerificationResponse.class)
                        .returnResult()
                        .getResponseBody();

        assertEquals(0,validAtCreation.code());


        // 3. Perform additional actions that create audit entries.
        restTestClient
                .post()
                .uri("/api/backoffice/application/"+savedApplication.id()+"/comment")
                .header("X-XSRF-TOKEN", token)
                .header(HttpHeaders.COOKIE, sessionToken + "; XSRF-TOKEN=" + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ApplicationCommentRequest("Skicka kontoutdrag"))
                .exchange()
                .expectStatus().isOk();



        // 4. Verify the chain again.
        ChainVerificationResponse validAfterUpdate =
                restTestClient.get()
                        .uri("/api/audit/{id}", savedApplication.id())
                        .header("X-XSRF-TOKEN", token)
                        .header(HttpHeaders.COOKIE, sessionToken + "; XSRF-TOKEN=" + token)
                        .exchange()
                        .expectStatus().isOk()
                        .expectBody(ChainVerificationResponse.class)
                        .returnResult()
                        .getResponseBody();

        assertEquals(0,validAfterUpdate.code());
    }
    @Test
    void tamperedEventInChainFailsVerification() {

        String token = csrfToken();
        String sessionToken = loginAsCompany(token,orgNR,persNR);



        // 1. Create a credit application through your existing API.
        ApplicationSubmission newApplication = createApplicationBody();

        CreditApplicationDTO savedApplication = restTestClient.post()
                .uri("/api/application/apply")
                .header("X-XSRF-TOKEN", token)
                .header(HttpHeaders.COOKIE, sessionToken + "; XSRF-TOKEN=" + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(createApplicationBody())
                .exchange()
                .returnResult(CreditApplicationDTO.class)
                .getResponseBody();




        // 2. Verify the initial audit chain.
        ChainVerificationResponse initialResponse =
                restTestClient.get()
                        .uri("/api/audit/{id}", savedApplication.id())
                        .header("X-XSRF-TOKEN", token)
                        .header(HttpHeaders.COOKIE, sessionToken + "; XSRF-TOKEN=" + token)
                        .exchange()
                        .expectStatus().isOk()
                        .expectBody(ChainVerificationResponse.class)
                        .returnResult()
                        .getResponseBody();

        assertThat(initialResponse).isNotNull();




        // 2. login as caseworker + Assert chain as valid

        logout(sessionToken,token);
        sessionToken = loginAsCaseWorker(token,email,password);




        ChainVerificationResponse validAtCreation =
                restTestClient.get()
                        .uri("/api/audit/{id}", savedApplication.id())
                        .header("X-XSRF-TOKEN", token)
                        .header(HttpHeaders.COOKIE, sessionToken + "; XSRF-TOKEN=" + token)
                        .exchange()
                        .expectStatus().isOk()
                        .expectBody(ChainVerificationResponse.class)
                        .returnResult()
                        .getResponseBody();

        assertEquals(0,validAtCreation.code());


        // 3. Perform additional actions that create audit entries.
        restTestClient
                .post()
                .uri("/api/backoffice/application/"+savedApplication.id()+"/comment")
                .header("X-XSRF-TOKEN", token)
                .header(HttpHeaders.COOKIE, sessionToken + "; XSRF-TOKEN=" + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ApplicationCommentRequest("Skicka kontoutdrag"))
                .exchange()
                .expectStatus().isOk();



        // 4. Verify the chain again.
        ChainVerificationResponse validAfterUpdate =
                restTestClient.get()
                        .uri("/api/audit/{id}", savedApplication.id())
                        .header("X-XSRF-TOKEN", token)
                        .header(HttpHeaders.COOKIE, sessionToken + "; XSRF-TOKEN=" + token)
                        .exchange()
                        .expectStatus().isOk()
                        .expectBody(ChainVerificationResponse.class)
                        .returnResult()
                        .getResponseBody();

        assertEquals(0,validAfterUpdate.code());


        // 5. Manually tamper with the database.
        jdbcTemplate.update("""
    UPDATE audit_events
    SET data = '{"actorType":"COMPANY","purpose":"Tampered"}'::jsonb
    WHERE application_id = ?
      AND sequence_number = 1
    """, savedApplication.id());

// 6. Verify that tampering is detected.
        ChainVerificationResponse tamperedResponse =
                restTestClient.get()
                        .uri("/api/audit/{id}", savedApplication.id())
                        .header("X-XSRF-TOKEN", token)
                        .header(HttpHeaders.COOKIE, sessionToken + "; XSRF-TOKEN=" + token)
                        .exchange()
                        .expectStatus().isOk()
                        .expectBody(ChainVerificationResponse.class)
                        .returnResult()
                        .getResponseBody();

        assertThat(tamperedResponse).isNotNull();
        assertNotEquals(0, tamperedResponse.code());


    }

    private ApplicationSubmission createApplicationBody() {
        return new ApplicationSubmission(
                orgNR,
                BigDecimal.valueOf(250000),
                "Expansion",
                12,
                new ContactDetails("Test", "mail@live.se", "number")
        );
    }


    private void logout(String session, String xsrf){
        restTestClient.post()
                .uri("/api/auth/logout")
                .header(HttpHeaders.COOKIE, session + "; XSRF-TOKEN=" + xsrf)
                .header("X-XSRF-TOKEN", xsrf)
                .exchange()
                .expectStatus().isNoContent();
    }
    private String csrfToken() {
        List<String> cookies = restTestClient
                .post()
                .uri("/api/auth/logout")
                .exchange()
                .returnResult(Void.class)
                .getResponseHeaders()
                .get(HttpHeaders.SET_COOKIE);

        return cookies.stream()
                .map(value -> value.split(";", 2)[0])
                .filter(value -> value.startsWith("XSRF-TOKEN="))
                .findFirst()
                .orElseThrow()
                .substring("XSRF-TOKEN=".length());
    }

    private String loginAsCompany(String token, String orgNumber, String personalNumber) {
        return restTestClient
                .post()
                .uri("/api/auth/login/company")
                .header("X-XSRF-TOKEN", token)
                .header(HttpHeaders.COOKIE, "XSRF-TOKEN=" + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CompanyLoginRequest(orgNumber, personalNumber))
                .exchange()
                .expectStatus()
                .isOk()
                .returnResult(CompanyLoginResponse.class)
                .getResponseHeaders()
                .get(HttpHeaders.SET_COOKIE)
                .stream()
                .map(value -> value.split(";", 2)[0])
                .filter(value -> value.startsWith("JSESSIONID="))
                .findFirst()
                .orElseThrow();
    }

    private String loginAsCaseWorker(String token, String email, String password) {
        return restTestClient.post()
                .uri("/api/auth/login/caseworker")
                .header("X-XSRF-TOKEN", token)
                .header(HttpHeaders.COOKIE, "XSRF-TOKEN=" + token)
                .body(new CaseWorkerLoginRequest(email, password))
                .exchange()
                .returnResult(CaseWorkerLoginResponse.class)
                .getResponseHeaders()
                .get(HttpHeaders.SET_COOKIE)
                .stream()
                .map(value -> value.split(";", 2)[0])
                .filter(value -> value.startsWith("JSESSIONID="))
                .findFirst()
                .orElseThrow();
    }

}
