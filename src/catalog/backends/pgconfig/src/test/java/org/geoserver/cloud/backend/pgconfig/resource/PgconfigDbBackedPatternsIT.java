/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.backend.pgconfig.resource;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import javax.sql.DataSource;
import org.geoserver.cloud.backend.pgconfig.support.PgConfigTestContainer;
import org.geoserver.cloud.backend.pgconfig.support.PgconfigTestDatabaseSupport;
import org.geoserver.cloud.config.catalog.backend.pgconfig.PgconfigBackendProperties;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.integration.jdbc.lock.DefaultLockRepository;
import org.springframework.integration.jdbc.lock.JdbcLockRegistry;
import org.springframework.integration.jdbc.lock.LockRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.FileSystemUtils;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class PgconfigDbBackedPatternsIT {

    @Container
    static PgConfigTestContainer container = new PgConfigTestContainer();

    @RegisterExtension
    PgconfigTestDatabaseSupport db = new PgconfigTestDatabaseSupport(container);

    @TempDir
    File tmpDir;

    private PgconfigResourceStore store;
    private File cacheDirectory;
    private JdbcTemplate template;

    @BeforeEach
    void setUp() throws Exception {
        template = db.getTemplate();
        PgconfigLockProvider lockProvider = new PgconfigLockProvider(pgconfigLockRegistry());
        cacheDirectory = new File(tmpDir, "cache");
        Files.createDirectories(cacheDirectory.toPath());
        FileSystemResourceStoreCache cache = FileSystemResourceStoreCache.ofProvidedDirectory(
                cacheDirectory.toPath(),
                PgconfigResourceStore.antPathMatcher(PgconfigBackendProperties.defaultDbBackedFilePatterns()));
        store = new PgconfigResourceStore(
                cache,
                template,
                lockProvider,
                PgconfigResourceStore.defaultIgnoredResources(),
                PgconfigResourceStore.antPathMatcher(PgconfigBackendProperties.defaultDbBackedFilePatterns()));
    }

    private JdbcLockRegistry pgconfigLockRegistry() {
        return new JdbcLockRegistry(pgconfigLockRepository());
    }

    private LockRepository pgconfigLockRepository() {
        DataSource dataSource = db.getDataSource();
        DefaultLockRepository lockRepository = new DefaultLockRepository(dataSource, "test-instance");
        // override default table prefix "INT" by "RESOURCE_" (matching table RESOURCE_LOCK in flyway ddl scripts)
        lockRepository.setPrefix("RESOURCE_");
        return lockRepository;
    }

    @Test
    void whitelistedFileUnderDataIsDbBacked() {
        Resource resource = store.get("data/ws/store/indexer.properties");
        assertThat(resource).isInstanceOf(PgconfigResource.class);
        assertThat(resource.getType()).isEqualTo(Type.UNDEFINED);
    }

    @Test
    void nonWhitelistedFileUnderDataStaysLocal() {
        Resource resource = store.get("data/ws/store/granule.tif");
        assertThat(resource).isNotInstanceOf(PgconfigResource.class);
    }

    @Test
    void savingWhitelistedFileCreatesParentRowsAndRow() throws Exception {
        Resource resource = store.get("data/ws/store/indexer.properties");
        try (OutputStream out = resource.out()) {
            out.write("CanBeEmpty=true".getBytes(StandardCharsets.UTF_8));
        }
        assertThat(resource.getType()).isEqualTo(Type.RESOURCE);

        Integer dirRows = template.queryForObject(
                "SELECT count(*) FROM resourcestore WHERE path IN ('data','data/ws','data/ws/store')", Integer.class);
        assertThat(dirRows).isEqualTo(3);

        Resource reRead = store.get("data/ws/store/indexer.properties");
        assertThat(new String(reRead.getContents(), StandardCharsets.UTF_8)).isEqualTo("CanBeEmpty=true");
    }

    @Test
    void rootListingStillHidesDataDir() throws Exception {
        try (OutputStream out = store.get("data/ws/store/indexer.properties").out()) {
            out.write("x".getBytes(StandardCharsets.UTF_8));
        }
        assertThat(store.get("").list()).map(Resource::path).doesNotContain("data");
    }

    @Test
    void locallyNewerWhitelistedFileIsNotClobbered() throws Exception {
        String path = "data/ws/store/coverage.properties";
        Resource resource = store.get(path);
        try (OutputStream out = resource.out()) {
            out.write("Levels=1".getBytes(StandardCharsets.UTF_8));
        }
        File localFile = new File(cacheDirectory, path);
        assertThat(localFile).exists();

        // simulate a raw gt-imagemosaic write: newer content directly on the file
        Files.writeString(localFile.toPath(), "Levels=2");
        long future = System.currentTimeMillis() + 60_000;
        assertThat(localFile.setLastModified(future)).isTrue();

        File resolved = store.get(path).file();
        assertThat(Files.readString(resolved.toPath())).isEqualTo("Levels=2");
    }

    @Test
    void dbNewerWhitelistedFileIsRefreshed() throws Exception {
        String path = "data/ws/store/coverage.properties";
        try (OutputStream out = store.get(path).out()) {
            out.write("Levels=1".getBytes(StandardCharsets.UTF_8));
        }
        // second write through the Resource API bumps the DB mtime
        try (OutputStream out = store.get(path).out()) {
            out.write("Levels=3".getBytes(StandardCharsets.UTF_8));
        }
        File localFile = new File(cacheDirectory, path);
        // stale local copy: older mtime, stale content
        Files.writeString(localFile.toPath(), "Levels=1");
        assertThat(localFile.setLastModified(1000L)).isTrue();

        File resolved = store.get(path).file();
        assertThat(Files.readString(resolved.toPath())).isEqualTo("Levels=3");
    }

    @Test
    void adaptorGetRoutesWhitelistedChildThroughStore() {
        Resource dir = store.get("data/ws/store");
        Resource child = dir.get("indexer.properties");
        assertThat(child).isInstanceOf(PgconfigResource.class);
        Resource raster = dir.get("granule.tif");
        assertThat(raster).isNotInstanceOf(PgconfigResource.class);
    }

    @Test
    void adaptorDirMaterializesDbBackedChildren() throws Exception {
        try (OutputStream out = store.get("data/ws/store/indexer.properties").out()) {
            out.write("CanBeEmpty=true".getBytes(StandardCharsets.UTF_8));
        }
        // wipe the local cache copy to simulate a fresh pod
        File localDir = new File(cacheDirectory, "data/ws/store");
        FileSystemUtils.deleteRecursively(localDir);
        assertThat(localDir).doesNotExist();

        File dir = store.get("data/ws/store").dir();
        assertThat(new File(dir, "indexer.properties")).exists();
        assertThat(Files.readString(new File(dir, "indexer.properties").toPath()))
                .isEqualTo("CanBeEmpty=true");
    }

    @Test
    void adaptorListIncludesMaterializedDbChildren() throws Exception {
        try (OutputStream out = store.get("data/ws/store2/datastore.properties").out()) {
            out.write("SPI=x".getBytes(StandardCharsets.UTF_8));
        }
        File localDir = new File(cacheDirectory, "data/ws/store2");
        FileSystemUtils.deleteRecursively(localDir);

        List<Resource> children = store.get("data/ws/store2").list();
        assertThat(children).map(Resource::name).contains("datastore.properties");
    }

    @Test
    void removingWhitelistedFileDeletesLocalCacheCopy() throws Exception {
        String path = "data/ws/store3/coverage.properties";
        try (OutputStream out = store.get(path).out()) {
            out.write("Levels=1".getBytes(StandardCharsets.UTF_8));
        }
        File localFile = new File(cacheDirectory, path);
        assertThat(localFile).exists();

        assertThat(store.remove(path)).isTrue();

        assertThat(localFile).doesNotExist();
        Integer rows = template.queryForObject(
                "SELECT count(*) FROM resourcestore WHERE path = 'data/ws/store3/coverage.properties'", Integer.class);
        assertThat(rows).isZero();
    }

    @Test
    void removingHybridDirectoryDeletesDbBackedRows() throws Exception {
        try (OutputStream out = store.get("data/ws/store4/indexer.properties").out()) {
            out.write("CanBeEmpty=true".getBytes(StandardCharsets.UTF_8));
        }
        File localDir = new File(cacheDirectory, "data/ws/store4");
        Files.writeString(new File(localDir, "granule.tif").toPath(), "not a real tif");

        assertThat(store.remove("data/ws/store4")).isTrue();

        assertThat(localDir).doesNotExist();
        Integer rows = template.queryForObject(
                "SELECT count(*) FROM resourcestore WHERE path LIKE 'data/ws/store4%'", Integer.class);
        assertThat(rows).isZero();
    }

    @Test
    void movingHybridDirectoryRelocatesDbBackedRowsAndLocalFiles() throws Exception {
        try (OutputStream out = store.get("data/ws/store5/indexer.properties").out()) {
            out.write("CanBeEmpty=true".getBytes(StandardCharsets.UTF_8));
        }
        File localDir = new File(cacheDirectory, "data/ws/store5");
        Files.writeString(new File(localDir, "granule.tif").toPath(), "not a real tif");

        assertThat(store.move("data/ws/store5", "data/ws/store5_renamed")).isTrue();

        Integer oldRows = template.queryForObject(
                "SELECT count(*) FROM resourcestore WHERE path LIKE 'data/ws/store5/%' OR path = 'data/ws/store5'",
                Integer.class);
        assertThat(oldRows).isZero();
        Resource movedConfig = store.get("data/ws/store5_renamed/indexer.properties");
        assertThat(movedConfig).isInstanceOf(PgconfigResource.class);
        assertThat(new String(movedConfig.getContents(), StandardCharsets.UTF_8))
                .isEqualTo("CanBeEmpty=true");

        File movedDir = new File(cacheDirectory, "data/ws/store5_renamed");
        assertThat(new File(movedDir, "granule.tif")).exists();
        assertThat(localDir).doesNotExist();
    }
}
