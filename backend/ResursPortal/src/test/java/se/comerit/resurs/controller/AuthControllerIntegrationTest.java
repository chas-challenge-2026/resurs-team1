package se.comerit.resurs.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;
import se.comerit.resurs.dto.auth.CompanyLoginRequest;
import se.comerit.resurs.dto.auth.CompanyLoginResponse;
import se.comerit.resurs.dto.auth.CurrentUserResponse;
import se.comerit.resurs.exception.ApiError;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integrationstest för inloggningen: HTTP -> AuthController -> AuthService -> BankID-mock + företagsvalidering.
 *
 * Till skillnad från AuthServiceTests går anropen hela vägen via HTTP med TestRestTemplate, så testet täcker
 * även sessionen: inloggningen svarar med en JSESSIONID-cookie som måste skickas med i nästa anrop.
 *
 * Bolagen och personnumren kommer från MockCompanyData och MockBankIdClient, alltså samma mockdata som appen kör med.
 * Postgres-containern initieras med infra/seed.sql, precis som i DocumentControllerIntegrationTest.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class AuthControllerIntegrationTest {

    private static final Path SEED_SQL = Paths.get("").toAbsolutePath()
            .resolve("../../infra/seed.sql").normalize();

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:12")
            .withCopyFileToContainer(MountableFile.forHostPath(SEED_SQL), "/docker-entrypoint-initdb.d/seed.sql");

    // Bolaget och firmatecknaren finns i MockCompanyData, personnumret även i MockBankIdClient
    private static final String ORG_NUMBER = "556000-1234";
    private static final String COMPANY_NAME = "Fasen Elteknik AB";
    private static final String SIGNATORY_PERSONAL_NUMBER = "197503121234";

    // Verifieras av BankID men är inte firmatecknare för ORG_NUMBER
    private static final String OTHER_PERSONAL_NUMBER = "198206245678";

    // Okänt för BankID
    private static final String UNKNOWN_PERSONAL_NUMBER = "000000000000";

    // Handläggaren kommer från infra/seed.sql
    private static final String WORKER_EMAIL = "karin@resurs.se";
    private static final String WORKER_PASSWORD = "password123";

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void loginCompany_shouldReturnCompanyFromRegistry_whenSignatoryIsVerified() {
        ResponseEntity<CompanyLoginResponse> response = loginCompany(SIGNATORY_PERSONAL_NUMBER);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().orgNumber()).isEqualTo(ORG_NUMBER);
        assertThat(response.getBody().role()).isEqualTo("company");
        assertThat(response.getBody().companyName()).isEqualTo(COMPANY_NAME);
    }

    @Test
    void loginCompany_shouldStartSession_whenLoginSucceeds() {
        ResponseEntity<CompanyLoginResponse> login = loginCompany(SIGNATORY_PERSONAL_NUMBER);
        String session = sessionCookie(login);

        assertThat(session).isNotNull();

        ResponseEntity<CurrentUserResponse> me = restTemplate.exchange(
                "/api/auth/me", HttpMethod.GET, withSession(session), CurrentUserResponse.class);

        assertThat(me.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(me.getBody()).isNotNull();
        assertThat(me.getBody().role()).isEqualTo("company");
        assertThat(me.getBody().orgNumber()).isEqualTo(ORG_NUMBER);
        assertThat(me.getBody().displayName()).isEqualTo(COMPANY_NAME);
    }

    @Test
    void me_shouldReturnUnauthorized_whenNoSessionIsSent() {
        ResponseEntity<ApiError> response = restTemplate.getForEntity("/api/auth/me", ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void logout_shouldEndSession() {
        String session = sessionCookie(loginCompany(SIGNATORY_PERSONAL_NUMBER));

        ResponseEntity<Void> logout = restTemplate.exchange(
                "/api/auth/logout", HttpMethod.POST, withSession(session), Void.class);
        assertThat(logout.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<ApiError> me = restTemplate.exchange(
                "/api/auth/me", HttpMethod.GET, withSession(session), ApiError.class);
        assertThat(me.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void loginCompany_shouldFail_whenBankIdDoesNotKnowThePersonalNumber() {
        ResponseEntity<ApiError> response = restTemplate.postForEntity(
                "/api/auth/login/company",
                new CompanyLoginRequest(ORG_NUMBER, UNKNOWN_PERSONAL_NUMBER),
                ApiError.class);

        // BankIdVerificationFailedException saknar egen @ExceptionHandler och fångas av den generella.
        // Byt till UNAUTHORIZED när GlobalExceptionHandler hanterar den.
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void loginCompany_shouldFail_whenPersonIsNotSignatoryForTheCompany() {
        ResponseEntity<ApiError> response = restTemplate.postForEntity(
                "/api/auth/login/company",
                new CompanyLoginRequest(ORG_NUMBER, OTHER_PERSONAL_NUMBER),
                ApiError.class);

        // CompanyValidationFailedException saknar egen @ExceptionHandler och fångas av den generella.
        // Byt till FORBIDDEN när GlobalExceptionHandler hanterar den.
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void loginCompany_shouldReturnBadRequest_whenPersonalNumberIsMissing() {
        ResponseEntity<ApiError> response = restTemplate.postForEntity(
                "/api/auth/login/company",
                new CompanyLoginRequest(ORG_NUMBER, ""),
                ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }



    private ResponseEntity<CompanyLoginResponse> loginCompany(String personalNumber) {
        return restTemplate.postForEntity(
                "/api/auth/login/company",
                new CompanyLoginRequest(ORG_NUMBER, personalNumber),
                CompanyLoginResponse.class);
    }

    /** TestRestTemplate sparar inga cookies, så JSESSIONID plockas ur svaret och skickas med manuellt. */
    private String sessionCookie(ResponseEntity<?> response) {
        String cookie = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        return cookie == null ? null : cookie.split(";", 2)[0];
    }

    private HttpEntity<Void> withSession(String sessionCookie) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, sessionCookie);
        return new HttpEntity<>(headers);
    }
}
