package se.comerit.resurs.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.cache.CacheManager;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;
import se.comerit.resurs.persistence.model.Branch;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
class BranchRepositoryCacheIntegrationTest {

    private static final Path SEED_SQL = Paths.get("").toAbsolutePath()
            .resolve("../../infra/seed.sql").normalize();

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withCopyFileToContainer(MountableFile.forHostPath(SEED_SQL), "/docker-entrypoint-initdb.d/seed.sql");

    private static final String SEEDED_BRANCH = "BYGG";
    private static final String UNKNOWN_BRANCH = "OKAND_BRANSCH";
    private static final String BRANCH_CACHE = "branches";

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void clearCache() {
        cacheManager.getCache(BRANCH_CACHE).clear();
    }

    @Test
    void findByBranchName_calledTwice_returnsCachedInstance() {
        Optional<Branch> first = branchRepository.findByBranchName(SEEDED_BRANCH);
        Optional<Branch> second = branchRepository.findByBranchName(SEEDED_BRANCH);

        assertThat(first).isPresent();
        assertThat(second).isPresent();
        assertThat(second.get()).isSameAs(first.get());
    }

    @Test
    void findByBranchName_whenFound_storesBranchInCache() {
        branchRepository.findByBranchName(SEEDED_BRANCH);

        assertThat(cacheManager.getCache(BRANCH_CACHE).get(SEEDED_BRANCH)).isNotNull();
    }

    @Test
    void findByBranchName_whenNotFound_doesNotCacheEmptyResult() {
        assertThat(branchRepository.findByBranchName(UNKNOWN_BRANCH)).isEmpty();

        assertThat(cacheManager.getCache(BRANCH_CACHE).get(UNKNOWN_BRANCH)).isNull();
    }
}
