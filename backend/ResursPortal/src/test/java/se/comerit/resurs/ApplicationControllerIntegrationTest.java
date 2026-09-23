
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

import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;

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

        var loginResponse = restTestClient
                .post()
                .uri("/api/auth/login/company")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CompanyLoginRequest(
                        "556000-1234",
                        "197503121234"
                ))
                .exchange()
                .expectStatus()
                .isOk()
                .returnResult(CompanyLoginResponse.class);

        String sessionCookie = loginResponse
                .getResponseHeaders()
                .getFirst(HttpHeaders.SET_COOKIE);

        restTestClient
                .post()
                .uri("/api/application/apply")
                .header(HttpHeaders.COOKIE, sessionCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ApplicationSubmission(
                        "556000-1234",
                        BigDecimal.valueOf(250000),
                        "Expansion",
                        12,
                        new ContactDetails("Test", "mail", "number")
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
                    assertThat(application.contactDetails().email()).isEqualTo("mail");
                    assertThat(application.contactDetails().phoneNumber()).isEqualTo("number");
                });
    }



    @Test
    void submitApplication_withoutSession_returns401() {

        restTestClient
                .post()
                .uri("/api/application/apply")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ApplicationSubmission(
                        "556677-8899",
                                BigDecimal.valueOf(250000),
                                "Expansion",
                        12,
                        new ContactDetails("Test","mail","number")
                                )
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
}

