package se.comerit.resurs;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.MountableFile;
import se.comerit.resurs.persistence.CompanyRepository;
import se.comerit.resurs.persistence.model.Company;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CompanyCryptoIntegrationTest {

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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

    @Test
    void shouldEncryptAndDecryptOrgNumber() {
        String orgNumber = "556123-4567";

        Company company = new Company();
        company.setOrg_number(orgNumber);

        Company saved = companyRepository.saveAndFlush(company);

        // JPA returns the original plaintext value
        Company loaded = companyRepository.findById(saved.getId())
                .orElseThrow();

        assertEquals(orgNumber, loaded.getOrg_number());

        // Read the raw database value, bypassing JPA converters
        String storedValue = jdbcTemplate.queryForObject(
                "SELECT org_number FROM companies WHERE id = ?",
                String.class,
                saved.getId()
        );

        System.out.println(storedValue);

        assertNotNull(storedValue);
        assertNotEquals(orgNumber, storedValue);
    }
}