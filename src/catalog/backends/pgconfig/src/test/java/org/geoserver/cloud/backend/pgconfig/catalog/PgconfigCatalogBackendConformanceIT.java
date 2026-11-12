/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.backend.pgconfig.catalog;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.catalog.plugin.CatalogConformanceTest;
import org.geoserver.catalog.plugin.CatalogPlugin;
import org.geoserver.catalog.plugin.CatalogPluginStyleResourcePersister;
import org.geoserver.catalog.plugin.ExtendedCatalogFacade;
import org.geoserver.catalog.plugin.requestcache.RequestCacheScope;
import org.geoserver.catalog.plugin.requestcache.RequestCachingCatalogFacade;
import org.geoserver.cloud.backend.pgconfig.PgconfigBackendBuilder;
import org.geoserver.cloud.backend.pgconfig.cache.EntityCache;
import org.geoserver.cloud.backend.pgconfig.cache.ZstdCodec;
import org.geoserver.cloud.backend.pgconfig.resource.FileSystemResourceStoreCache;
import org.geoserver.cloud.backend.pgconfig.resource.PgconfigLockProvider;
import org.geoserver.cloud.backend.pgconfig.resource.PgconfigResourceStore;
import org.geoserver.cloud.backend.pgconfig.resource.ResourceChangePublisher;
import org.geoserver.cloud.backend.pgconfig.support.PgConfigTestContainer;
import org.geoserver.cloud.backend.pgconfig.support.PgconfigTestDatabaseSupport;
import org.geoserver.cloud.config.catalog.backend.pgconfig.PgconfigBackendProperties;
import org.geoserver.cloud.config.catalog.backend.pgconfig.PgconfigGeoServerResourceLoader;
import org.geoserver.platform.resource.SimpleResourceNotificationDispatcher;
import org.geotools.util.logging.Logging;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.integration.jdbc.lock.DefaultLockRepository;
import org.springframework.integration.jdbc.lock.JdbcLockRegistry;
import org.springframework.integration.jdbc.lock.LockRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** @since 1.4 */
@Testcontainers(disabledWithoutDocker = true)
@Execution(value = ExecutionMode.CONCURRENT)
class PgconfigCatalogBackendConformanceIT extends CatalogConformanceTest {

    @Container
    static PgConfigTestContainer container = new PgConfigTestContainer();

    @RegisterExtension
    PgconfigTestDatabaseSupport db = new PgconfigTestDatabaseSupport(container);

    /** The codecs of the caches of a test: a nested suite also runs the setup of this class, and opens two. */
    private final List<ZstdCodec> codecs = new ArrayList<>();

    @Override
    protected CatalogImpl createCatalog(File cacheDirectory) {
        PgconfigCatalogFacade facade = new PgconfigCatalogFacade(db.getTemplate(), newEntityCache());
        return newCatalog(cacheDirectory, PgconfigBackendBuilder.resolving(facade));
    }

    private CatalogImpl newCatalog(File cacheDirectory, ExtendedCatalogFacade facade) {
        JdbcTemplate template = db.getTemplate();
        PgconfigLockProvider lockProvider = new PgconfigLockProvider(pgconfigLockRegistry());
        FileSystemResourceStoreCache cache = FileSystemResourceStoreCache.ofProvidedDirectory(
                cacheDirectory.toPath(),
                PgconfigResourceStore.antPathMatcher(PgconfigBackendProperties.defaultDbBackedFilePatterns()));
        PgconfigResourceStore resourceStore = new PgconfigResourceStore(
                cache,
                template,
                lockProvider,
                PgconfigResourceStore.defaultIgnoredResources(),
                PgconfigResourceStore.antPathMatcher(PgconfigBackendProperties.defaultDbBackedFilePatterns()),
                new DataSourceTransactionManager(db.getDataSource()),
                new ResourceChangePublisher(new SimpleResourceNotificationDispatcher(), event -> {}));

        PgconfigGeoServerResourceLoader resourceLoader = new PgconfigGeoServerResourceLoader(resourceStore);
        CatalogPlugin catalog = new CatalogPlugin();
        catalog.setFacade(facade);
        catalog.setResourceLoader(resourceLoader);
        final boolean backupSldFiles = false;
        catalog.addListener(new CatalogPluginStyleResourcePersister(catalog, backupSldFiles));
        return catalog;
    }

    /** The cache as the backend configures it by default, initialized as after the catalog loader ran. */
    private EntityCache newEntityCache() {
        ZstdCodec codec = ZstdCodec.withBundledDictionary(ZstdCodec.DEFAULT_LEVEL);
        codecs.add(codec);
        long maximumWeight = EntityCache.maximumWeightFor(5);
        EntityCache cache = new EntityCache("gs-catalog", maximumWeight, Duration.ofSeconds(30), codec);
        cache.setInitialized(true);
        return cache;
    }

    @AfterEach
    void closeCodecs() {
        codecs.forEach(ZstdCodec::close);
        codecs.clear();
    }

    private JdbcLockRegistry pgconfigLockRegistry() {
        return new JdbcLockRegistry(pgconfigLockRepository());
    }

    LockRepository pgconfigLockRepository() {
        DataSource dataSource = db.getDataSource();
        DefaultLockRepository lockRepository = new DefaultLockRepository(dataSource, "test-instance");
        // override default table prefix "INT" by "RESOURCE_" (matching table definition
        // RESOURCE_LOCK in init.XXX.sql
        lockRepository.setPrefix("RESOURCE_");
        // initialize like the pgconfigLockRepository bean in PgconfigBackendConfiguration does, the
        // transaction templates are required to acquire locks
        lockRepository.setTransactionManager(new DataSourceTransactionManager(dataSource));
        lockRepository.afterPropertiesSet();
        lockRepository.afterSingletonsInstantiated();
        return lockRepository;
    }

    @Disabled(
            """
            revisit, seems to be just a problem of ordering or equals with the \
            returned ft/ft2 where mockito is not throwing the expected exception
            """)
    @Override
    public void testSaveDataStoreRollbacksBothStoreAndResources() {}

    /**
     * The suite through the cache of a request, as the backend configuration builds the catalog facade. Each test runs
     * within one request scope, as a request of the REST API or of the web UI reads and writes the catalog.
     */
    @Nested
    class WithinRequest extends CatalogConformanceTest {

        @Override
        protected CatalogImpl createCatalog(File cacheDirectory) {
            PgconfigCatalogFacade facade = new PgconfigCatalogFacade(db.getTemplate(), newEntityCache());
            RequestCachingCatalogFacade requestCaching = PgconfigBackendBuilder.requestCaching(facade);
            return newCatalog(cacheDirectory, requestCaching);
        }

        @BeforeEach
        void openRequest() {
            RequestCacheScope.open(RequestCacheScope.DEFAULT_MAX_ENTRIES);
        }

        @AfterEach
        void closeRequest() {
            RequestCacheScope.close();
        }

        @Disabled("as in the enclosing class")
        @Override
        public void testSaveDataStoreRollbacksBothStoreAndResources() {}
    }

    static @BeforeAll void beforeAll() throws Exception {
        try {
            Logging.ALL.setLoggerFactory("org.geotools.util.logging.CommonsLoggerFactory");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }
}
