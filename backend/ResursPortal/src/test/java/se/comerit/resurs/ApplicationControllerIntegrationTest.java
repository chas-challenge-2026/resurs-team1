
package se.comerit.resurs;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

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
    void submitApplication_withoutSession_returns401() {

        restTestClient
                .post()
                .uri("/application/apply")
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
                .uri("/application/1")
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
                .uri("/application")
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
                .uri("/application/dashboard")
                .exchange()
                .expectStatus()
                .isUnauthorized();
    }
}

