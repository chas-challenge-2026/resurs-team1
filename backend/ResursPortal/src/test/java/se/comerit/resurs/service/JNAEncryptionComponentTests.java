package se.comerit.resurs.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.MountableFile;
import se.comerit.resurs.jna.JNAEncryptionComponent;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class JNAEncryptionComponentTests {

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

    @Autowired
    private JNAEncryptionComponent JNAEncryptionComponent;

    @Test
    void encodeAndDecode() {
        String plaintext = "Hello from JNA";

        System.out.println("PlainText: "+ plaintext);
        byte[] encrypted = JNAEncryptionComponent.encode(plaintext);
        System.out.println("Cipher: "+ new String(Base64.getEncoder().encode(encrypted)));
        String decrypted = JNAEncryptionComponent.decode(encrypted);

        assertEquals(plaintext, decrypted);
    }
}