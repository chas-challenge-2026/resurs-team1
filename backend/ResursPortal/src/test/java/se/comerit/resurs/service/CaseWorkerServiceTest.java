package se.comerit.resurs.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;
import se.comerit.resurs.dto.caseworker.CaseWorkerResponse;
import se.comerit.resurs.dto.caseworker.CreateCaseWorkerRequest;
import se.comerit.resurs.dto.caseworker.UpdateCaseWorkerRequest;
import se.comerit.resurs.persistence.CaseWorkerRepository;
import se.comerit.resurs.persistence.model.CaseWorker;
import se.comerit.resurs.security.PasswordHasher;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
* Den här CaseWorkerServiceTest klassen testar CRUD flödet i CaseWorkerService mot en riktig databas(Testcontainers)
* Den täcker:
* - create: giltig request, dubblett-epost
* - getById: existerande id, okänt id
* - update: giltig ändring, samma e-post som sig själv, dubblett mot annan, okänt id
* - delete: existerande id, okänt id
* */
@SpringBootTest
@Testcontainers
public class CaseWorkerServiceTest {
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
                    .withCopyFileToContainer(MountableFile.forHostPath(SEED_SQL),
                            "/docker-entrypoint-initdb.d/seed.sql");

    @Autowired
    private CaseWorkerService caseWorkerService;

    @Autowired
    private CaseWorkerRepository caseWorkerRepository;

    @Autowired
    private PasswordHasher passwordHasher;

    private String uniqueEmail() {
        return UUID.randomUUID() + "@resurs.se";
    }

    private CaseWorker createCaseWorker(String email) {
        CaseWorker caseWorker = new CaseWorker();
        caseWorker.setName("Test Handläggare");
        caseWorker.setEmail(email);
        caseWorker.setPasswordHash(passwordHasher.md5Hash("password123"));
        return caseWorkerRepository.saveAndFlush(caseWorker);
    }

    @Test
    void create_validRequest_savesAndReturnsCaseWorker() {
        String email = uniqueEmail();
        CreateCaseWorkerRequest request = new CreateCaseWorkerRequest("Karin", email, "hemligt123");

        CaseWorkerResponse response = caseWorkerService.create(request);

        assertNotNull(response.id());
        assertEquals("Karin", response.name());
        assertEquals(email, response.email());

        CaseWorker saved = caseWorkerRepository.findById(response.id()).orElseThrow();
        assertEquals(passwordHasher.md5Hash("hemligt123"), saved.getPasswordHash());
    }

    @Test
    void create_duplicateEmail_throwsException() {
        String email = uniqueEmail();
        createCaseWorker(email);

        CreateCaseWorkerRequest request = new CreateCaseWorkerRequest("Ny person", email, "hemligt123");

        assertThrows(IllegalArgumentException.class, () -> caseWorkerService.create(request));
    }

    @Test
    void getById_existingId_returnsCaseWorker() {
        CaseWorker caseWorker = createCaseWorker(uniqueEmail());

        CaseWorkerResponse response = caseWorkerService.getById(caseWorker.getId());

        assertEquals(caseWorker.getId(), response.id());
        assertEquals(caseWorker.getName(), response.name());
        assertEquals(caseWorker.getEmail(), response.email());
    }

    @Test
    void getById_unknownId_throwsException() {
        assertThrows(NoSuchElementException.class, () -> caseWorkerService.getById(999_999L));
    }

    @Test
    void update_validRequest_updatesNameAndEmail() {
        CaseWorker caseWorker = createCaseWorker(uniqueEmail());
        String newEmail = uniqueEmail();
        UpdateCaseWorkerRequest request = new UpdateCaseWorkerRequest("Nytt namn", newEmail);

        caseWorkerService.update(caseWorker.getId(), request);

        CaseWorker updated = caseWorkerRepository.findById(caseWorker.getId()).orElseThrow();
        assertEquals("Nytt namn", updated.getName());
        assertEquals(newEmail, updated.getEmail());
    }

    @Test
    void update_sameEmailAsSelf_doesNotThrow() {
        String email = uniqueEmail();
        CaseWorker caseWorker = createCaseWorker(email);
        UpdateCaseWorkerRequest request = new UpdateCaseWorkerRequest("Nytt namn", email);

        assertDoesNotThrow(() -> caseWorkerService.update(caseWorker.getId(), request));
    }

    @Test
    void update_duplicateEmail_throwsException() {
        String takenEmail = uniqueEmail();
        createCaseWorker(takenEmail);
        CaseWorker caseWorkerToUpdate = createCaseWorker(uniqueEmail());

        UpdateCaseWorkerRequest request = new UpdateCaseWorkerRequest("Nytt namn", takenEmail);

        assertThrows(IllegalArgumentException.class, () -> caseWorkerService.update(caseWorkerToUpdate.getId(), request));
    }

    @Test
    void update_unknownId_throwsException() {
        UpdateCaseWorkerRequest request = new UpdateCaseWorkerRequest("Nytt namn", uniqueEmail());

        assertThrows(NoSuchElementException.class, () -> caseWorkerService.update(999_999L, request));
    }

    @Test
    void delete_existingId_removesCaseWorker() {
        CaseWorker caseWorker = createCaseWorker(uniqueEmail());

        caseWorkerService.delete(caseWorker.getId());

        assertTrue(caseWorkerRepository.findById(caseWorker.getId()).isEmpty());
    }

    @Test
    void delete_unknownId_throwsException() {
        assertThrows(NoSuchElementException.class, () -> caseWorkerService.delete(999_999L));
    }
}
