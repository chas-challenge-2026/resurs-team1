package se.comerit.resurs.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;
import se.comerit.resurs.dto.auth.CaseWorkerLoginRequest;
import se.comerit.resurs.dto.auth.CompanyLoginRequest;
import se.comerit.resurs.dto.companyvalidation.CompanyFinancialApiDTO;
import se.comerit.resurs.exception.ApiError;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class FinancialVerificationControllerIntegrationTest {

    private static final Path SEED_SQL = Paths.get("").toAbsolutePath()
            .resolve("../../infra/seed.sql").normalize();

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withCopyFileToContainer(MountableFile.forHostPath(SEED_SQL), "/docker-entrypoint-initdb.d/seed.sql");

    private static final String ORG_NUMBER = "556000-1234";
    private static final String SIGNATORY_PERSONAL_NUMBER = "750312-1234";
    private static final String WORKER_EMAIL = "karin@resurs.se";
    private static final String WORKER_PASSWORD = "password123";
    private static final String ANNUAL_REPORT_URL = "/api/financial/verification";

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void fetchLatestAnnualReport_shouldReturnReport_whenLoggedInAsCompany() {
        String session = sessionCookie(restTemplate.exchange(
                "/api/auth/login/company", HttpMethod.POST,
                new HttpEntity<>(new CompanyLoginRequest(ORG_NUMBER, SIGNATORY_PERSONAL_NUMBER), postHeaders()),
                Void.class));

        ResponseEntity<CompanyFinancialApiDTO> response = restTemplate.exchange(
                ANNUAL_REPORT_URL, HttpMethod.GET, withSession(session), CompanyFinancialApiDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().incomeStatement().revenue()).isEqualByComparingTo(new BigDecimal("12000000"));
    }

    @Test
    void fetchLatestAnnualReport_shouldReturnUnauthorized_whenNotLoggedIn() {
        ResponseEntity<ApiError> response = restTemplate.getForEntity(ANNUAL_REPORT_URL, ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void fetchLatestAnnualReport_shouldReturnForbidden_whenLoggedInAsCaseWorker() {
        String session = sessionCookie(restTemplate.exchange(
                "/api/auth/login/caseworker", HttpMethod.POST,
                new HttpEntity<>(new CaseWorkerLoginRequest(WORKER_EMAIL, WORKER_PASSWORD), postHeaders()),
                Void.class));

        ResponseEntity<ApiError> response = restTemplate.exchange(
                ANNUAL_REPORT_URL, HttpMethod.GET, withSession(session), ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    private String csrfCookie() {
        return cookie(restTemplate.postForEntity("/api/auth/logout", null, Void.class), "XSRF-TOKEN");
    }

    private String cookie(ResponseEntity<?> response, String name) {
        List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        if (cookies == null) {
            return null;
        }
        return cookies.stream()
                .map(value -> value.split(";", 2)[0])
                .filter(value -> value.startsWith(name + "="))
                .findFirst()
                .orElse(null);
    }

    private HttpHeaders postHeaders() {
        String csrf = csrfCookie();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("X-XSRF-TOKEN", csrf.substring("XSRF-TOKEN=".length()));
        headers.add(HttpHeaders.COOKIE, csrf);
        return headers;
    }

    private String sessionCookie(ResponseEntity<?> response) {
        return cookie(response, "JSESSIONID");
    }

    private HttpEntity<Void> withSession(String sessionCookie) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, sessionCookie);
        return new HttpEntity<>(headers);
    }
}
