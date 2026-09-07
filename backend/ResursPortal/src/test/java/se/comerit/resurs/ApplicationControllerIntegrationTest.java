
package se.comerit.resurs;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.containers.PostgreSQLContainer;
import se.comerit.resurs.dto.CreditApplicationDTO;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApplicationControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:17")
                    .withDatabaseName("resurs_test")
                    .withUsername("test")
                    .withPassword("test");


    @Autowired
    private RestTestClient restTestClient;


    // ============================================================
    // Create application -> retrieve application
    // ============================================================

    @Test
    void shouldCreateAndRetrieveApplication() {

        /*
         * POST /apply
         */
        var response = restTestClient
                .post()
                .uri("/apply")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body("""
                        orgNumber=556677-8899&
                        companyName=Test+Company&
                        authorizedSignatory=Test+Person&
                        egetKapital=500000&
                        totaltKapital=1000000&
                        omsattningstillgangar=400000&
                        kortfristigaSkulder=200000&
                        totalaSkulder=500000&
                        rorelseresultat=100000&
                        nettoomsattning=2000000&
                        requestedAmount=250000&
                        purpose=Expansion&
                        operativtKassaflode=150000&
                        investeringsKassaflode=-50000&
                        ranteKostnader=10000&
                        bransch=IT
                        """)
                .exchange()
                .expectStatus()
                .isCreated()
                .returnResult(CreditApplicationDTO.class);


        /*
         * The controller should return:
         *
         * Location: http://localhost:<port>/application/<id>
         */
        URI location = response.getResponseHeaders().getLocation();

        assertNotNull(location);

        assertTrue(
                location.getPath().matches("/application/\\d+"),
                "Expected Location to match /application/{id}, but was: "
                        + location
        );


        /*
         * GET /application/{id}
         */
        restTestClient
                .get()
                .uri(location)
                .exchange()
                .expectStatus()
                .isOk();
    }


    // ============================================================
    // Unauthenticated create
    // ============================================================

    @Test
    void shouldReturn401WhenCreatingApplicationWithoutSession() {

        restTestClient
                .post()
                .uri("/apply")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body("""
                        orgNumber=556677-8899&
                        companyName=Test+Company&
                        authorizedSignatory=Test+Person&
                        egetKapital=500000&
                        totaltKapital=1000000&
                        omsattningstillgangar=400000&
                        kortfristigaSkulder=200000&
                        totalaSkulder=500000&
                        rorelseresultat=100000&
                        nettoomsattning=2000000&
                        requestedAmount=250000&
                        purpose=Expansion
                        """)
                .exchange()
                .expectStatus()
                .isUnauthorized();
    }


    // ============================================================
    // Unauthenticated get
    // ============================================================

    @Test
    void shouldReturn401WhenGettingApplicationWithoutSession() {

        restTestClient
                .get()
                .uri("/application/1")
                .exchange()
                .expectStatus()
                .isUnauthorized();
    }
}

