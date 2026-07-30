/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.config.catalog.backend.pgconfig;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import javax.sql.DataSource;
import org.geoserver.cloud.backend.pgconfig.resource.FileSystemResourceStoreCache;
import org.geoserver.cloud.backend.pgconfig.resource.PgconfigLockProvider;
import org.geoserver.cloud.backend.pgconfig.resource.PgconfigResourceStore;
import org.geoserver.cloud.backend.pgconfig.support.PgConfigTestContainer;
import org.geoserver.cloud.backend.pgconfig.support.PgconfigTestDatabaseSupport;
import org.geoserver.platform.resource.Resource.Type;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.integration.jdbc.lock.DefaultLockRepository;
import org.springframework.integration.jdbc.lock.JdbcLockRegistry;
import org.springframework.integration.jdbc.lock.LockRepository;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

@org.testcontainers.junit.jupiter.Testcontainers(disabledWithoutDocker = true)
class PgconfigGeoServerResourceLoaderIT {

    @org.testcontainers.junit.jupiter.Container
    static PgConfigTestContainer container = new PgConfigTestContainer();

    @RegisterExtension
    PgconfigTestDatabaseSupport db = new PgconfigTestDatabaseSupport(container);

    @TempDir
    File cacheDirectory;

    private PgconfigResourceStore store;

    @BeforeEach
    void setUp() {
        PgconfigLockProvider lockProvider = new PgconfigLockProvider(new JdbcLockRegistry(lockRepository()));
        FileSystemResourceStoreCache cache = FileSystemResourceStoreCache.ofProvidedDirectory(
                cacheDirectory.toPath(),
                PgconfigResourceStore.antPathMatcher(PgconfigBackendProperties.defaultDbBackedFilePatterns()));
        store = new PgconfigResourceStore(
                cache,
                db.getTemplate(),
                lockProvider,
                PgconfigResourceStore.defaultIgnoredResources(),
                PgconfigResourceStore.antPathMatcher(PgconfigBackendProperties.defaultDbBackedFilePatterns()));
    }

    private LockRepository lockRepository() {
        DataSource dataSource = db.getDataSource();
        DefaultLockRepository lockRepository = new DefaultLockRepository(dataSource);
        lockRepository.setPrefix("RESOURCE_");
        // initialize like the pgconfigLockRepository bean in PgconfigBackendConfiguration does, the
        // transaction templates are required to acquire locks
        lockRepository.setTransactionManager(new DataSourceTransactionManager(dataSource));
        lockRepository.afterPropertiesSet();
        lockRepository.afterSingletonsInstantiated();
        return lockRepository;
    }

    /**
     * The security directory must exist before any {@code AbstractAccessRuleDAO} bean constructs.
     *
     * <p>The access rule DAOs (e.g. {@code rest.properties}, {@code layers.properties}) load their rules at bean
     * construction time and permanently keep an empty rule set when the security directory does not exist at that
     * point, because their re-load check depends on a property-file watcher that is only created when the directory
     * exists. GeoServer's security config migration creates the directory only after all beans are constructed. On a
     * fresh pgconfig database that ordering left the REST API denying every request with 403.
     */
    @Test
    void createsSecurityDirectoryOnFreshDatabase() {
        assertThat(store.get("security").getType()).isEqualTo(Type.UNDEFINED);

        new PgconfigGeoServerResourceLoader(store);

        assertThat(store.get("security").getType()).isEqualTo(Type.DIRECTORY);
    }
}
