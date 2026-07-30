/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.backend.pgconfig.resource;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;
import javax.sql.DataSource;
import org.geoserver.cloud.backend.pgconfig.support.PgConfigTestContainer;
import org.geoserver.cloud.backend.pgconfig.support.PgconfigTestDatabaseSupport;
import org.geoserver.cloud.config.catalog.backend.pgconfig.PgconfigBackendProperties;
import org.geoserver.platform.resource.Resource.Type;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.integration.jdbc.lock.DefaultLockRepository;
import org.springframework.integration.jdbc.lock.JdbcLockRegistry;
import org.springframework.integration.jdbc.lock.LockRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.util.FileSystemUtils;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class DbBackedFileSynchronizerIT {

    @Container
    static PgConfigTestContainer container = new PgConfigTestContainer();

    @RegisterExtension
    PgconfigTestDatabaseSupport db = new PgconfigTestDatabaseSupport(container);

    @TempDir
    File tmpDir;

    private PgconfigResourceStore store;
    private FileSystemResourceStoreCache cache;
    private File cacheDirectory;
    private JdbcTemplate template;

    private DbBackedFileSynchronizer synchronizer;

    @BeforeEach
    void setUp() throws Exception {
        template = db.getTemplate();
        cacheDirectory = new File(tmpDir, "cache");
        cache = newCache(cacheDirectory);
        store = newStore(cache);
        synchronizer = new DbBackedFileSynchronizer(store, cache);
    }

    /**
     * Builds a cache over a given local directory; paired with {@link #newStore(FileSystemResourceStoreCache)} this
     * lets a test build a second store/cache pair over the same database, simulating a second pod reading what a first
     * pod wrote.
     */
    private FileSystemResourceStoreCache newCache(File directory) throws IOException {
        Files.createDirectories(directory.toPath());
        return FileSystemResourceStoreCache.ofProvidedDirectory(
                directory.toPath(),
                PgconfigResourceStore.antPathMatcher(PgconfigBackendProperties.defaultDbBackedFilePatterns()));
    }

    private PgconfigResourceStore newStore(FileSystemResourceStoreCache cache) {
        PgconfigLockProvider lockProvider = new PgconfigLockProvider(pgconfigLockRegistry());
        return new PgconfigResourceStore(
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
        // initialize like the pgconfigLockRepository bean in PgconfigBackendConfiguration does, the
        // transaction templates are required to acquire locks
        lockRepository.setTransactionManager(new DataSourceTransactionManager(dataSource));
        lockRepository.afterPropertiesSet();
        lockRepository.afterSingletonsInstantiated();
        return lockRepository;
    }

    @Test
    void pushesRawWrittenWhitelistedFilesToDb() throws Exception {
        // the uploaded zip files went through the Resource API
        try (OutputStream out = store.get("data/ws/mosaic/indexer.properties").out()) {
            out.write("CanBeEmpty=true".getBytes(StandardCharsets.UTF_8));
        }
        // gt-imagemosaic writes these with raw file I/O
        Path localDir = cache.localPath("data/ws/mosaic");
        Files.createDirectories(localDir);
        Files.writeString(localDir.resolve("mosaic.properties"), "Levels=1");
        Files.write(localDir.resolve("sample_image.dat"), new byte[] {1, 2, 3});
        Files.writeString(localDir.resolve("granule.tif"), "not a real tif");

        synchronizer.sync("data/ws/mosaic");

        assertThat(store.get("data/ws/mosaic/mosaic.properties").getType()).isEqualTo(Type.RESOURCE);
        assertThat(store.get("data/ws/mosaic/sample_image.dat").getContents()).containsExactly(1, 2, 3);
        // non-whitelisted files are not pushed
        Integer tifRows = template.queryForObject(
                "SELECT count(*) FROM resourcestore WHERE path = 'data/ws/mosaic/granule.tif'", Integer.class);
        assertThat(tifRows).isZero();
    }

    @Test
    void skipsFilesNotNewerThanDb() throws Exception {
        try (OutputStream out = store.get("data/ws/mosaic2/coverage.properties").out()) {
            out.write("Levels=1".getBytes(StandardCharsets.UTF_8));
        }
        long dbMtime = store.get("data/ws/mosaic2/coverage.properties").lastmodified();

        synchronizer.sync("data/ws/mosaic2");

        assertThat(store.get("data/ws/mosaic2/coverage.properties").lastmodified())
                .isEqualTo(dbMtime);
    }

    @Test
    void materializePullsDbBackedFilesIntoASecondPodsCache() throws Exception {
        // pod A: a file written through the Resource API, and one gt-imagemosaic wrote raw that the
        // synchronizer already pushed to the database
        try (OutputStream out = store.get("data/ws/mosaic3/indexer.properties").out()) {
            out.write("CanBeEmpty=true".getBytes(StandardCharsets.UTF_8));
        }
        Path localDirA = cache.localPath("data/ws/mosaic3");
        Files.createDirectories(localDirA);
        Files.writeString(localDirA.resolve("coverage.properties"), "Levels=1");
        synchronizer.sync("data/ws/mosaic3");

        // pod B: a fresh cache directory over the same database, nothing materialized yet; a fresh
        // DbBackedFileSynchronizer instance has no throttle history, proving the throttle never
        // blocks the first materialization of a path
        FileSystemResourceStoreCache cacheB = newCache(new File(tmpDir, "cacheB"));
        PgconfigResourceStore storeB = newStore(cacheB);
        DbBackedFileSynchronizer synchronizerB = new DbBackedFileSynchronizer(storeB, cacheB);

        synchronizerB.materialize("data/ws/mosaic3");

        Path localDirB = cacheB.localPath("data/ws/mosaic3");
        assertThat(Files.readString(localDirB.resolve("indexer.properties"))).isEqualTo("CanBeEmpty=true");
        assertThat(Files.readString(localDirB.resolve("coverage.properties"))).isEqualTo("Levels=1");
    }

    @Test
    void materializeThrottlesRepeatedCallsToTheSamePath() throws Exception {
        try (OutputStream out = store.get("data/ws/mosaic5/indexer.properties").out()) {
            out.write("CanBeEmpty=true".getBytes(StandardCharsets.UTF_8));
        }
        Path materialized = cache.localPath("data/ws/mosaic5").resolve("indexer.properties");

        synchronizer.materialize("data/ws/mosaic5");
        assertThat(Files.readString(materialized)).isEqualTo("CanBeEmpty=true");

        // another pod updates the database copy; within the throttle window, with the local copy still
        // present, the database is not re-queried and the local file keeps its content
        FileSystemResourceStoreCache cacheB = newCache(new File(tmpDir, "cacheB-throttle"));
        PgconfigResourceStore storeB = newStore(cacheB);
        try (OutputStream out = storeB.get("data/ws/mosaic5/indexer.properties").out()) {
            out.write("CanBeEmpty=false".getBytes(StandardCharsets.UTF_8));
        }
        synchronizer.materialize("data/ws/mosaic5");

        assertThat(Files.readString(materialized)).isEqualTo("CanBeEmpty=true");
    }

    @Test
    void materializeBypassesThrottleWhenLocalDbBackedFilesAreMissing() throws Exception {
        try (OutputStream out = store.get("data/ws/mosaic6/indexer.properties").out()) {
            out.write("CanBeEmpty=true".getBytes(StandardCharsets.UTF_8));
        }
        Path materialized = cache.localPath("data/ws/mosaic6").resolve("indexer.properties");

        synchronizer.materialize("data/ws/mosaic6");
        assertThat(materialized).exists();

        // the local copy going missing (cache eviction, or a read racing the store's creation on another
        // pod) must defeat the throttle: the next call re-pulls from the database instead of leaving the
        // reader with no config files for the length of the throttle window
        Files.delete(materialized);
        synchronizer.materialize("data/ws/mosaic6");

        assertThat(materialized).exists();
        assertThat(Files.readString(materialized)).isEqualTo("CanBeEmpty=true");
    }

    @Test
    void syncDeletionsRemovesDbRowsOfLocallyDeletedFiles() throws Exception {
        Path localDir = cache.localPath("data/ws/mosaic7");
        Files.createDirectories(localDir);
        Files.writeString(localDir.resolve("mosaic7.properties"), "Levels=1");
        Files.write(localDir.resolve("sample_image.dat"), new byte[] {1});
        synchronizer.sync("data/ws/mosaic7");
        assertThat(store.get("data/ws/mosaic7/mosaic7.properties").getType()).isEqualTo(Type.RESOURCE);
        assertThat(store.get("data/ws/mosaic7/sample_image.dat").getType()).isEqualTo(Type.RESOURCE);

        // gt-imagemosaic removed one file locally (e.g. removeCoverage); its row must go, its sibling's stays
        Files.delete(localDir.resolve("mosaic7.properties"));
        synchronizer.syncDeletions("data/ws/mosaic7");

        assertThat(store.get("data/ws/mosaic7/mosaic7.properties").getType()).isEqualTo(Type.UNDEFINED);
        assertThat(store.get("data/ws/mosaic7/sample_image.dat").getType()).isEqualTo(Type.RESOURCE);
    }

    @Test
    void storeRecreatedAtSamePathPushesAfterSyncDeletions() throws Exception {
        Path localDir = cache.localPath("data/ws/mosaic8");
        Files.createDirectories(localDir);
        Files.writeString(localDir.resolve("mosaic8.properties"), "Levels=1");
        synchronizer.sync("data/ws/mosaic8");

        // the whole store is deleted locally, like reader.delete(true) does, and the deletion reconciled
        FileSystemUtils.deleteRecursively(localDir);
        synchronizer.syncDeletions("data/ws/mosaic8");
        assertThat(store.get("data/ws/mosaic8/mosaic8.properties").getType()).isEqualTo(Type.UNDEFINED);

        // a store recreated at the same path with file mtimes not newer than the deleted store's must
        // still push: syncDeletions reset the path's newest-synced mtime threshold
        Files.createDirectories(localDir);
        Path recreated = Files.writeString(localDir.resolve("mosaic8.properties"), "Levels=2");
        assertThat(recreated.toFile().setLastModified(System.currentTimeMillis() - 60_000))
                .isTrue();
        synchronizer.sync("data/ws/mosaic8");

        assertThat(store.get("data/ws/mosaic8/mosaic8.properties").getContents())
                .isEqualTo("Levels=2".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void syncThrottledSkipsRecentlySyncedPathWhileSyncPropagatesImmediately() throws Exception {
        Path localDir = cache.localPath("data/ws/mosaic9");
        Files.createDirectories(localDir);
        Path configFile = Files.writeString(localDir.resolve("mosaic9.properties"), "Levels=1");

        synchronizer.syncThrottled("data/ws/mosaic9");
        assertThat(store.get("data/ws/mosaic9/mosaic9.properties").getType()).isEqualTo(Type.RESOURCE);

        // a raw rewrite with a newer mtime is not pushed by the read path within the throttle window
        Files.writeString(configFile, "Levels=2");
        assertThat(configFile.toFile().setLastModified(System.currentTimeMillis() + 60_000))
                .isTrue();
        synchronizer.syncThrottled("data/ws/mosaic9");
        assertThat(store.get("data/ws/mosaic9/mosaic9.properties").getContents())
                .isEqualTo("Levels=1".getBytes(StandardCharsets.UTF_8));

        // but a mutation sync always propagates immediately
        synchronizer.sync("data/ws/mosaic9");
        assertThat(store.get("data/ws/mosaic9/mosaic9.properties").getContents())
                .isEqualTo("Levels=2".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void concurrentMaterializeCallsAreSingleFlighted() throws Exception {
        try (OutputStream out = store.get("data/ws/mosaic10/indexer.properties").out()) {
            out.write("CanBeEmpty=true".getBytes(StandardCharsets.UTF_8));
        }
        // wipe the local copy: every caller then needs the pull, and only one thread should perform it
        // while the others wait on it
        FileSystemUtils.deleteRecursively(cache.localPath("data/ws/mosaic10"));

        List<CompletableFuture<Void>> callers = IntStream.range(0, 8)
                .mapToObj(i -> CompletableFuture.runAsync(() -> synchronizer.materialize("data/ws/mosaic10")))
                .toList();
        callers.forEach(CompletableFuture::join);

        Path materialized = cache.localPath("data/ws/mosaic10").resolve("indexer.properties");
        assertThat(Files.readString(materialized)).isEqualTo("CanBeEmpty=true");
    }
}
