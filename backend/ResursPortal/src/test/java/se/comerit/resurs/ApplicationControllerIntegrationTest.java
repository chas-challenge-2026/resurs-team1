
package se.comerit.resurs;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;
import se.comerit.resurs.dto.ContactDetails;
import se.comerit.resurs.dto.CreditApplicationDTO;
import se.comerit.resurs.dto.application.ApplicationSubmission;
import se.comerit.resurs.dto.auth.CompanyLoginRequest;
import se.comerit.resurs.dto.auth.CompanyLoginResponse;
import se.comerit.resurs.enums.ApplicationStatus;
import se.comerit.resurs.enums.AuditAction;
import se.comerit.resurs.persistence.AuditEventRepository;
import se.comerit.resurs.persistence.model.AuditEvent;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ApplicationControllerIntegrationTest {

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

    @LocalServerPort
    private int port;

    private RestTestClient restTestClient;

    @Autowired
    private AuditEventRepository auditEventRepository;

    // 556000-4444 finns i registret men årsredovisningen är "nere" i mocken
    private static final String ANNUAL_REPORT_UNAVAILABLE_ORG_NUMBER = "556000-4444";
    private static final String SIGNATORY_PERSONAL_NUMBER = "750312-1234";

    @BeforeEach
    void setUp() {
        restTestClient = RestTestClient
                .bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    // ============================================================
    // POST /application/apply
    // ============================================================



    @Test
    void submitApplication_withCorrectSession_returns201() {

        String token = csrfToken();

        var loginResponse = restTestClient
                .post()
                .uri("/api/auth/login/company")
                .header("X-XSRF-TOKEN", token)
                .header(HttpHeaders.COOKIE, "XSRF-TOKEN=" + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CompanyLoginRequest(
                        "556000-1234",
                        "750312-1234"
                ))
                .exchange()
                .expectStatus()
                .isOk()
                .returnResult(CompanyLoginResponse.class);

        String sessionCookie = loginResponse
                .getResponseHeaders()
                .get(HttpHeaders.SET_COOKIE)
                        .stream()
                                .map(value -> value.split(";", 2)[0])
                                .filter(value -> value.startsWith("JSESSIONID="))
                                .findFirst()
                                .orElseThrow();


        restTestClient
                .post()
                .uri("/api/application/apply")
                .header("X-XSRF-TOKEN", token)
                .header(HttpHeaders.COOKIE, sessionCookie + "; XSRF-TOKEN=" + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ApplicationSubmission(
                        "556000-1234",
                        BigDecimal.valueOf(250000),
                        "Expansion",
                        12,
                        new ContactDetails("Test", "mail@live.se", "number")
                ))
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody(CreditApplicationDTO.class)
                .value(application -> {
                    assertThat(application).isNotNull();
                    assertThat(application.orgNumber()).isEqualTo("556000-1234");
                    assertThat(application.requestedAmount())
                            .isEqualByComparingTo(BigDecimal.valueOf(250000));
                    assertThat(application.purpose()).isEqualTo("Expansion");
                    assertThat(application.durationMonths()).isEqualTo(12);
                    assertThat(application.contactDetails().name()).isEqualTo("Test");
                    assertThat(application.contactDetails().email()).isEqualTo("mail@live.se");
                    assertThat(application.contactDetails().phoneNumber()).isEqualTo("number");
                });
    }

    // Registret svarar på bolagsuppslaget men inte på årsredovisningen -> ansökan sparas utan scoring
    @Test
    void submitApplication_whenAnnualReportUnavailable_returns201WithPendingScoring() {
        String token = csrfToken();
        String sessionCookie = loginAsCompany(token, ANNUAL_REPORT_UNAVAILABLE_ORG_NUMBER, SIGNATORY_PERSONAL_NUMBER);

        restTestClient
                .post()
                .uri("/api/application/apply")
                .header("X-XSRF-TOKEN", token)
                .header(HttpHeaders.COOKIE, sessionCookie + "; XSRF-TOKEN=" + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(submissionFor(ANNUAL_REPORT_UNAVAILABLE_ORG_NUMBER))
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody(CreditApplicationDTO.class)
                .value(application -> {
                    assertThat(application.status()).isEqualTo(ApplicationStatus.PENDING_SCORING);
                    assertThat(application.decision()).isNull();
                    assertThat(application.decisionReason()).isNull();
                    assertThat(application.scoringResult()).isNull();
                    assertThat(application.companyName()).isEqualTo("Karlssons Bygg AB");
                    assertThat(application.authorizedSignatory()).isEqualTo("Anders Karlsson");
                });
    }

    // Ingen scoring har körts, så auditloggen ska bara innehålla att ansökan skapades
    @Test
    void submitApplication_whenAnnualReportUnavailable_logsNoScoringRun() {
        String token = csrfToken();
        String sessionCookie = loginAsCompany(token, ANNUAL_REPORT_UNAVAILABLE_ORG_NUMBER, SIGNATORY_PERSONAL_NUMBER);

        CreditApplicationDTO application = restTestClient
                .post()
                .uri("/api/application/apply")
                .header("X-XSRF-TOKEN", token)
                .header(HttpHeaders.COOKIE, sessionCookie + "; XSRF-TOKEN=" + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(submissionFor(ANNUAL_REPORT_UNAVAILABLE_ORG_NUMBER))
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody(CreditApplicationDTO.class)
                .returnResult()
                .getResponseBody();

        assertThat(application).isNotNull();
        assertThat(auditEventRepository.findByApplicationIdOrderBySequenceNumberAsc(application.id()))
                .extracting(AuditEvent::getAction)
                .containsExactly(AuditAction.APPLICATION_CREATED);
    }



    @Test
    void submitApplication_withoutSession_returns401() {

        String token = csrfToken();

        restTestClient
                .post()
                .uri("/api/application/apply")
                .header("X-XSRF-TOKEN", token)
                .header(HttpHeaders.COOKIE, "XSRF-TOKEN=" + token)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(
                        "orgNumber=556677-8899" +
                                "&companyName=Test+Company" +
                                "&authorizedSignatory=Test+Person" +
                                "&egetKapital=500000" +
                                "&totaltKapital=1000000" +
                                "&omsattningstillgangar=400000" +
                                "&kortfristigaSkulder=200000" +
                                "&totalaSkulder=500000" +
                                "&rorelseresultat=100000" +
                                "&nettoomsattning=2000000" +
                                "&requestedAmount=250000" +
                                "&purpose=Expansion" +
                                "&operativtKassaflode=150000" +
                                "&investeringsKassaflode=-50000" +
                                "&ranteKostnader=10000" +
                                "&bransch=IT"
                )
                .exchange()
                .expectStatus()
                .isUnauthorized();
    }

    // ============================================================
    // GET /application/{id}
    // ============================================================

    @Test
    void getApplication_withoutSession_returns401() {

        restTestClient
                .get()
                .uri("/api/application/1")
                .exchange()
                .expectStatus()
                .isUnauthorized();
    }

    // ============================================================
    // GET /application
    // ============================================================

    @Test
    void getApplications_withoutSession_returns401() {

        restTestClient
                .get()
                .uri("/api/application")
                .exchange()
                .expectStatus()
                .isUnauthorized();
    }

    // ============================================================
    // GET /application/dashboard
    // ============================================================

    @Test
    void dashboard_withoutSession_returns401() {

        restTestClient
                .get()
                .uri("/api/application/dashboard")
                .exchange()
                .expectStatus()
                .isUnauthorized();
    }

    // Loggar in som företag och returnerar JSESSIONID-cookien
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

    private ApplicationSubmission submissionFor(String orgNumber) {
        return new ApplicationSubmission(
                orgNumber,
                BigDecimal.valueOf(250000),
                "Expansion",
                12,
                new ContactDetails("Test", "mail@live.se", "number")
        );
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
}
