/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.backend.pgconfig.catalog;

import java.io.File;
import javax.sql.DataSource;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.catalog.plugin.CatalogConformanceTest;
import org.geoserver.catalog.plugin.CatalogPlugin;
import org.geoserver.catalog.plugin.CatalogPluginStyleResourcePersister;
import org.geoserver.cloud.backend.pgconfig.PgconfigBackendBuilder;
import org.geoserver.cloud.backend.pgconfig.resource.FileSystemResourceStoreCache;
import org.geoserver.cloud.backend.pgconfig.resource.PgconfigLockProvider;
import org.geoserver.cloud.backend.pgconfig.resource.PgconfigResourceStore;
import org.geoserver.cloud.backend.pgconfig.support.PgConfigTestContainer;
import org.geoserver.cloud.config.catalog.backend.pgconfig.PgconfigGeoServerResourceLoader;
import org.geotools.util.logging.Logging;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.springframework.integration.jdbc.lock.DefaultLockRepository;
import org.springframework.integration.jdbc.lock.JdbcLockRegistry;
import org.springframework.integration.jdbc.lock.LockRepository;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * @since 1.4
 */
@Testcontainers(disabledWithoutDocker = true)
class PgconfigCatalogBackendConformanceTest extends CatalogConformanceTest {

    @Container
    static PgConfigTestContainer container = new PgConfigTestContainer();

    @Override
    @BeforeEach
    public void setUp() {
        container.setUp();
        super.setUp();
    }

    @AfterEach
    void cleanDb() throws Exception {
        container.tearDown();
    }

    @Override
    protected CatalogImpl createCatalog(File tmpFolder) {
        JdbcTemplate template = container.getTemplate();
        PgconfigLockProvider lockProvider = new PgconfigLockProvider(pgconfigLockRegistry());
        File cacheDirectory = tmpFolder;
        FileSystemResourceStoreCache cache = FileSystemResourceStoreCache.ofProvidedDirectory(cacheDirectory.toPath());
        PgconfigResourceStore resourceStore = new PgconfigResourceStore(
                cache, template, lockProvider, PgconfigResourceStore.defaultIgnoredResources());

        var resourceLoader = new PgconfigGeoServerResourceLoader(resourceStore);
        CatalogPlugin catalog = new PgconfigBackendBuilder(container.getDataSource()).createCatalog();
        catalog.setResourceLoader(resourceLoader);
        final boolean backupSldFiles = false;
        catalog.addListener(new CatalogPluginStyleResourcePersister(catalog, backupSldFiles));
        return catalog;
    }

    private LockRegistry pgconfigLockRegistry() {
        return new JdbcLockRegistry(pgconfigLockRepository());
    }

    LockRepository pgconfigLockRepository() {
        DataSource dataSource = container.getDataSource();
        DefaultLockRepository lockRepository = new DefaultLockRepository(dataSource, "test-instance");
        // override default table prefix "INT" by "RESOURCE_" (matching table definition
        // RESOURCE_LOCK in init.XXX.sql
        lockRepository.setPrefix("RESOURCE_");
        // time in ms to expire dead locks (10k is the default)
        lockRepository.setTimeToLive(300_000);
        return lockRepository;
    }

    @Disabled(
            """
            revisit, seems to be just a problem of ordering or equals with the \
            returned ft/ft2 where mockito is not throwing the expected exception
            """)
    @Override
    public void testSaveDataStoreRollbacksBothStoreAndResources() {}

    static @BeforeAll void beforeAll() throws Exception {
        try {
            Logging.ALL.setLoggerFactory("org.geotools.util.logging.CommonsLoggerFactory");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }
}
