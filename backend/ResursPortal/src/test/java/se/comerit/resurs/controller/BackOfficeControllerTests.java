package se.comerit.resurs.controller;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;
import se.comerit.resurs.dto.ContactDetails;
import se.comerit.resurs.dto.CreditApplicationDTO;
import se.comerit.resurs.dto.application.ApplicationSubmission;
import se.comerit.resurs.dto.auth.CaseWorkerLoginRequest;
import se.comerit.resurs.dto.auth.CaseWorkerLoginResponse;
import se.comerit.resurs.dto.auth.CompanyLoginRequest;
import se.comerit.resurs.dto.auth.CompanyLoginResponse;
import se.comerit.resurs.dto.backoffice.BackOfficeListsDTO;
import se.comerit.resurs.service.BackofficeService;

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


}
