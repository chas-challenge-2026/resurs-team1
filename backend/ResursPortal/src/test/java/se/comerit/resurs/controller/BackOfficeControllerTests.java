package se.comerit.resurs.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;
import se.comerit.resurs.dto.auth.CaseWorkerLoginRequest;
import se.comerit.resurs.dto.auth.CaseWorkerLoginResponse;
import se.comerit.resurs.dto.auth.CompanyLoginRequest;
import se.comerit.resurs.dto.auth.CompanyLoginResponse;
import se.comerit.resurs.service.BackofficeService;
import se.comerit.resurs.dto.backoffice.ApplicationCommentRequest;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class BackOfficeControllerTests {


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

    @MockitoSpyBean
    private BackofficeService service;

    @BeforeEach
    void setUp() {
        restTestClient = RestTestClient
                .bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    @Value("${spring.data.web.pageable.max-page-size}")
    private int maxPageSize;

    // Säkerställer att en väldigt stor begärd sidstorlek (99999) håller sig till det satta MAX_PAGE_SIZE
    @Test
    void applicationForReview_shouldCapPageSizeAtMax() {

        String token = csrfToken();

        var loginResponse = restTestClient
                .post()
                .uri("/api/auth/login/caseworker")
                .header("X-XSRF-TOKEN", token)
                .cookie("XSRF-TOKEN", token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CaseWorkerLoginRequest(
                        "karin@resurs.se",
                        "password123"
                ))
                .exchange()
                .expectStatus()
                .isOk()
                .returnResult(CaseWorkerLoginResponse.class);

        String sessionId = loginResponse
                .getResponseHeaders()
                .get(HttpHeaders.SET_COOKIE)
                .stream()
                .map(value -> value.split(";", 2)[0])
                .filter(value -> value.startsWith("JSESSIONID="))
                .map(value -> value.substring("JSESSIONID=".length()))
                .findFirst()
                .orElseThrow();



        restTestClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/backoffice")
                        .queryParam("review_page", 0)
                        .queryParam("review_size", 100)
                        .queryParam("decided_page", 0)
                        .queryParam("decided_size", 500)
                        .build())
                .cookie("JSESSIONID", sessionId)
                .cookie("XSRF-TOKEN", token)
                .exchange()
                .expectStatus().isOk();

        verify(service).applicationsForReview(
                argThat(pageable -> pageable.getPageSize() == maxPageSize),
                argThat(pageable -> pageable.getPageSize() == maxPageSize)
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

    // Loggar in som handläggare (Karin från seed.sql) och returnerar sessions-id:t.
    private String loginAsCaseWorker(String token) {
        return restTestClient
                .post()
                .uri("/api/auth/login/caseworker")
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

    // Loggar in som företag (556000-1234, firmatecknare från mockdatan) och returnerar sessions-id:t.
    private String loginAsCompany(String token) {
        return restTestClient
                .post()
                .uri("/api/auth/login/company")
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

    // ============================================================
    // GET /api/backoffice/application/{id}/audit
    // ============================================================

    //En handläggare ska kunna hämta auditloggen: 200 (ärende 1 från seed.sql har inga händelser än, så listan är tom)
    @Test
    void auditLog_asCaseWorker_returnsOk() {
        String token = csrfToken();
        String sessionId = loginAsCaseWorker(token);

        restTestClient
                .get()
                .uri("/api/backoffice/application/1/audit")
                .cookie("JSESSIONID", sessionId)
                .exchange()
                .expectStatus().isOk();
    }

    //Ett företag ska inte få hämta auditloggen (403)
    @Test
    void auditLog_asCompany_returnsForbidden() {
        String token = csrfToken();
        String sessionId = loginAsCompany(token);

        restTestClient
                .get()
                .uri("/api/backoffice/application/1/audit")
                .cookie("JSESSIONID", sessionId)
                .exchange()
                .expectStatus().isForbidden();
    }

    //Utan inloggning ska man få 401
    @Test
    void auditLog_withoutSession_returnsUnauthorized() {
        restTestClient
                .get()
                .uri("/api/backoffice/application/1/audit")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    //Ett ärende som INTE finns ska ge 404
    @Test
    void auditLog_unknownApplication_returnsNotFound() {
        String token = csrfToken();
        String sessionId = loginAsCaseWorker(token);

        restTestClient
                .get()
                .uri("/api/backoffice/application/999999/audit")
                .cookie("JSESSIONID", sessionId)
                .exchange()
                .expectStatus().isNotFound();
    }

    // ============================================================
    // POST /api/backoffice/application/{id}/requested-documents
    // ============================================================

    // Ett företag får inte begära komplettering, bara handläggare (403)
    @Test
    void requestDocuments_asCompany_returnsForbidden() {
        String token = csrfToken();
        String sessionId = loginAsCompany(token);

        restTestClient
                .post()
                .uri("/api/backoffice/application/1/request-documents")
                .header("X-XSRF-TOKEN", token)
                .cookie("XSRF-TOKEN", token)
                .cookie("JSESSIONID", sessionId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ApplicationCommentRequest("Skicka kontoutdrag"))
                .exchange()
                .expectStatus().isForbidden();
    }

    // Utan inloggning ska man få 401
    @Test
    void requestDocuments_withoutSession_returnsUnauthorized() {
        String token = csrfToken();

        restTestClient
                .post()
                .uri("/api/backoffice/application/1/request-documents")
                .header("X-XSRF-TOKEN", token)
                .cookie("XSRF-TOKEN", token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ApplicationCommentRequest("Skicka kontoutdrag"))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    // Ett ärende som inte finns ska ge 404
    @Test
    void requestDocuments_unknownApplication_returnsNotFound() {
        String token = csrfToken();
        String sessionId = loginAsCaseWorker(token);

        restTestClient
                .post()
                .uri("/api/backoffice/application/999999/request-documents")
                .header("X-XSRF-TOKEN", token)
                .cookie("XSRF-TOKEN", token)
                .cookie("JSESSIONID", sessionId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ApplicationCommentRequest("Skicka kontoutdrag"))
                .exchange()
                .expectStatus().isNotFound();
    }

    // En tom kommentar ska ge 400 (valideringen stoppar den innan något ändras)
    @Test
    void requestDocuments_blankComment_returnsBadRequest() {
        String token = csrfToken();
        String sessionId = loginAsCaseWorker(token);

        restTestClient
                .post()
                .uri("/api/backoffice/application/1/request-documents")
                .header("X-XSRF-TOKEN", token)
                .cookie("XSRF-TOKEN", token)
                .cookie("JSESSIONID", sessionId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ApplicationCommentRequest("   "))
                .exchange()
                .expectStatus().isBadRequest();
    }


}
