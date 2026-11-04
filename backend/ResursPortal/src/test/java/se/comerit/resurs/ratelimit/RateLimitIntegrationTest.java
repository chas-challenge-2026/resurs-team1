package se.comerit.resurs.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;
import se.comerit.resurs.dto.auth.CaseWorkerLoginRequest;
import se.comerit.resurs.dto.auth.CaseWorkerLoginResponse;
import se.comerit.resurs.dto.auth.CompanyLoginRequest;
import se.comerit.resurs.dto.auth.CompanyLoginResponse;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tester för rate limiting genom hela kedjan (filter, nyckel och räknare) mot en riktig server.
 *
 * Gränsen sänks till 5 anrop så att testerna blir korta. Varje test använder en egen IP-adress
 * (skickas i X-Forwarded-For), annars delar alla tester på samma räknare och påverkar varandra.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "rate-limit.capacity=5",
                "rate-limit.refill-tokens=5",
                "rate-limit.refill-period=10m",
                "server.forward-headers-strategy=native"
        })
@Testcontainers
class RateLimitIntegrationTest {

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

    //En användare som inte är inloggad får 5 anrop (här felaktiga inloggningar, 401), sedan 429 med Retry-After
    @Test
    void unauthenticatedRequests_areBlockedWith429AfterTheLimit() {
        String ip = "203.0.113.10";
        String token = csrfToken(ip);

        for (int i = 0; i < 5; i++) {
            assertThat(failedLoginStatus(ip, token)).isEqualTo(401);
        }

        restTestClient
                .post()
                .uri("/api/auth/login/caseworker")
                .header("X-Forwarded-For", ip)
                .header("X-XSRF-TOKEN", token)
                .cookie("XSRF-TOKEN", token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CaseWorkerLoginRequest("karin@resurs.se", "wrong-password"))
                .exchange()
                .expectStatus().isEqualTo(429)
                .expectHeader().exists(HttpHeaders.RETRY_AFTER)
                .expectBody(String.class)
                .value(body -> assertThat(body).contains("TOO_MANY_REQUESTS"));
    }

    //Olika IP-adresser har olika räknare: en som tagit slut påverkar inte en annan
    @Test
    void differentIpAddresses_haveSeparateLimits() {
        String tokenA = csrfToken("203.0.113.20");
        for (int i = 0; i < 6; i++) {
            failedLoginStatus("203.0.113.20", tokenA);
        }

        String tokenB = csrfToken("203.0.113.21");
        assertThat(failedLoginStatus("203.0.113.21", tokenB)).isEqualTo(401);
    }

    //En inloggad användare räknas per användare: handläggaren stoppas efter 5 anrop,
    //medan ett företag fortfarande kommer igenom
    @Test
    void authenticatedUsers_areLimitedPerUser() {
        String caseWorkerSession = loginAsCaseWorker("203.0.113.30");
        String companySession = loginAsCompany("203.0.113.31");

        for (int i = 0; i < 5; i++) {
            restTestClient.get().uri("/api/auth/me")
                    .header("X-Forwarded-For", "203.0.113.30")
                    .cookie("JSESSIONID", caseWorkerSession)
                    .exchange()
                    .expectStatus().isOk();
        }

        restTestClient.get().uri("/api/auth/me")
                .header("X-Forwarded-For", "203.0.113.30")
                .cookie("JSESSIONID", caseWorkerSession)
                .exchange()
                .expectStatus().isEqualTo(429);

        restTestClient.get().uri("/api/auth/me")
                .header("X-Forwarded-For", "203.0.113.31")
                .cookie("JSESSIONID", companySession)
                .exchange()
                .expectStatus().isOk();
    }

    //Anrop utanför /api räknas inte (t.ex. swagger och statiska filer)
    @Test
    void requestsOutsideApi_areNotCounted() {
        for (int i = 0; i < 10; i++) {
            restTestClient.get().uri("/v3/api-docs")
                    .header("X-Forwarded-For", "203.0.113.40")
                    .exchange()
                    .expectStatus().isOk();
        }
    }

    //Ett felaktigt inloggningsförsök. Inloggningen kräver ingen inloggning, så den räknas på IP-adressen.
    private int failedLoginStatus(String ip, String token) {
        return restTestClient
                .post()
                .uri("/api/auth/login/caseworker")
                .header("X-Forwarded-For", ip)
                .header("X-XSRF-TOKEN", token)
                .cookie("XSRF-TOKEN", token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CaseWorkerLoginRequest("karin@resurs.se", "wrong-password"))
                .exchange()
                .returnResult(Void.class)
                .getStatus()
                .value();
    }

    //Hämtar en CSRF-token. Utloggningen hanteras av säkerhetskedjan och räknas inte av rate limiting.
    private String csrfToken(String ip) {
        List<String> cookies = restTestClient
                .post()
                .uri("/api/auth/logout")
                .header("X-Forwarded-For", ip)
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

    private String loginAsCaseWorker(String ip) {
        String token = csrfToken(ip);
        return restTestClient
                .post()
                .uri("/api/auth/login/caseworker")
                .header("X-Forwarded-For", ip)
                .header("X-XSRF-TOKEN", token)
                .cookie("XSRF-TOKEN", token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CaseWorkerLoginRequest("karin@resurs.se", "password123"))
                .exchange()
                .expectStatus().isOk()
                .returnResult(CaseWorkerLoginResponse.class)
                .getResponseHeaders()
                .get(HttpHeaders.SET_COOKIE)
                .stream()
                .map(value -> value.split(";", 2)[0])
                .filter(value -> value.startsWith("JSESSIONID="))
                .map(value -> value.substring("JSESSIONID=".length()))
                .findFirst()
                .orElseThrow();
    }

    private String loginAsCompany(String ip) {
        String token = csrfToken(ip);
        return restTestClient
                .post()
                .uri("/api/auth/login/company")
                .header("X-Forwarded-For", ip)
                .header("X-XSRF-TOKEN", token)
                .cookie("XSRF-TOKEN", token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CompanyLoginRequest("556000-1234", "750312-1234"))
                .exchange()
                .expectStatus().isOk()
                .returnResult(CompanyLoginResponse.class)
                .getResponseHeaders()
                .get(HttpHeaders.SET_COOKIE)
                .stream()
                .map(value -> value.split(";", 2)[0])
                .filter(value -> value.startsWith("JSESSIONID="))
                .map(value -> value.substring("JSESSIONID=".length()))
                .findFirst()
                .orElseThrow();
    }
}