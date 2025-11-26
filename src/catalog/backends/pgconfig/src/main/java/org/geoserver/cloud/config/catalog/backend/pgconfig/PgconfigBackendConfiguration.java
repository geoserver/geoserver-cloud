/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.config.catalog.backend.pgconfig;

import java.util.function.Predicate;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.GeoServerConfigurationLock;
import org.geoserver.catalog.plugin.ExtendedCatalogFacade;
import org.geoserver.catalog.plugin.locking.LockProviderGeoServerConfigurationLock;
import org.geoserver.cloud.backend.pgconfig.PgconfigBackendBuilder;
import org.geoserver.cloud.backend.pgconfig.config.PgconfigConfigRepository;
import org.geoserver.cloud.backend.pgconfig.config.PgconfigGeoServerFacade;
import org.geoserver.cloud.backend.pgconfig.config.PgconfigUpdateSequence;
import org.geoserver.cloud.backend.pgconfig.resource.FileSystemResourceStoreCache;
import org.geoserver.cloud.backend.pgconfig.resource.PgconfigLockProvider;
import org.geoserver.cloud.backend.pgconfig.resource.PgconfigResourceStore;
import org.geoserver.cloud.config.catalog.backend.core.GeoServerBackendConfigurer;
import org.geoserver.cloud.config.catalog.backend.pgconfig.DatabaseMigrationConfiguration.Migrations;
import org.geoserver.config.GeoServerLoader;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.LockProvider;
import org.geoserver.platform.resource.ResourceStore;
import org.geoserver.security.GeoServerSecurityManager;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.integration.jdbc.lock.DefaultLockRepository;
import org.springframework.integration.jdbc.lock.JdbcLockRegistry;
import org.springframework.integration.jdbc.lock.LockRepository;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.StringUtils;

/**
 * Spring configuration for GeoServer Cloud's PostgreSQL-based catalog and configuration backend.
 *
 * <h2>Overview</h2>
 * <p>This configuration provides a fully database-backed GeoServer implementation that stores all
 * catalog, configuration, and resource data in PostgreSQL. Unlike the traditional data directory
 * backend, this implementation enables true cloud-native deployments with:
 * <ul>
 *  <li>Stateless application servers (no local filesystem dependencies)
 *  <li>Horizontal scalability without shared filesystem requirements
 *  <li>ACID guarantees for catalog and configuration changes
 *  <li>Centralized resource storage with file system cache for performance
 * </ul>
 *
 * <h2>Storage Architecture</h2>
 * <ul>
 *  <li><b>Catalog</b> - PostgreSQL-backed catalog facade ({@link PgconfigBackendBuilder})
 *  <li><b>Configuration</b> - PostgreSQL repository ({@link PgconfigConfigRepository}, {@link PgconfigGeoServerFacade})
 *  <li><b>Resources</b> - Hybrid storage with PostgreSQL backend and local filesystem cache ({@link PgconfigResourceStore})
 *  <li><b>Locking</b> - Database-backed distributed locks via Spring Integration JDBC locks ({@link JdbcLockRegistry})
 *  <li><b>Update Sequence</b> - Database-tracked version for WMS/WFS capabilities caching ({@link PgconfigUpdateSequence})
 * </ul>
 *
 * <h2>Distributed Locking</h2>
 * <p>Provides cluster-wide coordination through {@link DefaultLockRepository} with:
 * <ul>
 *  <li>Instance-specific lock identification via {@code info.instance-id} property
 *  <li>Custom table prefix {@code RESOURCE_} (instead of default {@code INT_})
 *  <li>300-second lock timeout for dead lock recovery
 *  <li>Explicit initialization to ensure transaction template creation in all Spring contexts
 * </ul>
 *
 * <h2>Database Schema</h2>
 * <p>Requires database schema initialization via {@link DatabaseMigrationConfiguration}, which this
 * configuration depends on through constructor injection. Migrations create tables for:
 * <ul>
 *  <li>Catalog entities (workspaces, namespaces, stores, layers, styles, etc.)
 *  <li>Configuration objects (services, settings, logging)
 *  <li>Resource storage (binary content with metadata)
 *  <li>Distributed locks ({@code RESOURCE_LOCK} table)
 * </ul>
 *
 * <h2>Configuration</h2>
 * <p>Backend behavior is controlled through configuration properties:
 * <pre>
 * geoserver.backend.pgconfig.enabled=true
 * geoserver.backend.pgconfig.datasource.url=jdbc:postgresql://host:5432/geoserver
 * geoserver.backend.pgconfig.datasource.username=geoserver
 * geoserver.backend.pgconfig.datasource.password=secret
 * info.instance-id=${HOSTNAME:}  # For distributed locking identifier
 * </pre>
 *
 * <h2>Bean Dependencies</h2>
 * <p>This configuration uses {@code @Configuration(proxyBeanMethods = false)} for optimal performance
 * and flexibility, allowing bean methods to declare their dependencies as method parameters rather than
 * calling other {@code @Bean} methods directly. This enables proper dependency injection and avoids
 * issues with CGLIB proxies.
 *
 * @since 1.4
 * @see GeoServerBackendConfigurer
 * @see DatabaseMigrationConfiguration
 * @see PgconfigBackendBuilder
 * @see PgconfigResourceStore
 * @see JdbcLockRegistry
 */
@Configuration(proxyBeanMethods = false)
@Slf4j(topic = "org.geoserver.cloud.config.catalog.backend.pgconfig")
public class PgconfigBackendConfiguration extends GeoServerBackendConfigurer {

    /**
     * Constructs the PostgreSQL backend configuration.
     *
     * <p>The {@link Migrations} parameter ensures that database schema initialization has completed
     * before this configuration creates any beans that depend on database tables.
     *
     * @param migrations database migration tracker that confirms schema is ready
     */
    PgconfigBackendConfiguration(Migrations migrations) {
        log.info(
                "Loading geoserver config backend with {}. {}",
                PgconfigBackendConfiguration.class.getSimpleName(),
                migrations);
    }

    @Bean
    ExtendedCatalogFacade catalogFacade(@Qualifier("pgconfigDataSource") DataSource dataSource) {
        return new PgconfigBackendBuilder(dataSource).createCatalogFacade();
    }

    @Bean(name = "pcconfigJdbcTemplate")
    JdbcTemplate pcconfigJdbcTemplate(@Qualifier("pgconfigDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    GeoServerConfigurationLock configurationLock(@Qualifier("pgconfigLockProvider") LockProvider lockProvider) {
        return new LockProviderGeoServerConfigurationLock(lockProvider);
    }

    @Primary
    @Bean
    @DependsOnDatabaseInitialization
    PgconfigUpdateSequence updateSequence(
            PgconfigGeoServerFacade geoserverFacade, @Qualifier("pgconfigDataSource") DataSource dataSource) {
        return new PgconfigUpdateSequence(dataSource, geoserverFacade);
    }

    @Bean
    GeoServerLoader geoServerLoaderImpl(
            GeoServerSecurityManager securityManager,
            GeoServerResourceLoader resourceLoader,
            GeoServerConfigurationLock configurationLock) {
        log.debug("Creating GeoServerLoader {}", PgconfigGeoServerLoader.class.getSimpleName());
        return new PgconfigGeoServerLoader(resourceLoader, configurationLock);
    }

    @Bean
    PgconfigConfigRepository configRepository(@Qualifier("pcconfigJdbcTemplate") JdbcTemplate template) {
        return new PgconfigConfigRepository(template);
    }

    @Bean
    PgconfigGeoServerFacade geoserverFacade(PgconfigConfigRepository configRepository) {
        return new PgconfigGeoServerFacade(configRepository);
    }

    @Bean
    ResourceStore resourceStoreImpl(
            @Qualifier("pgconfigLockProvider") PgconfigLockProvider lockProvider,
            FileSystemResourceStoreCache resourceStoreCache,
            @Qualifier("pcconfigJdbcTemplate") JdbcTemplate template) {
        log.debug("Creating ResourceStore {}", PgconfigResourceStore.class.getSimpleName());
        Predicate<String> localOnlyFilter = PgconfigResourceStore.defaultIgnoredResources();
        return new PgconfigResourceStore(resourceStoreCache, template, lockProvider, localOnlyFilter);
    }

    @Bean
    FileSystemResourceStoreCache pgconfigFileSystemResourceStoreCache() {
        return FileSystemResourceStoreCache.newTempDirInstance();
    }

    @Bean
    GeoServerResourceLoader resourceLoader(@Qualifier("resourceStoreImpl") ResourceStore resourceStore) {
        log.debug("Creating GeoServerResourceLoader {}", PgconfigGeoServerResourceLoader.class.getSimpleName());
        return new PgconfigGeoServerResourceLoader(resourceStore);
    }

    @Bean
    PgconfigLockProvider pgconfigLockProvider(@Qualifier("pgconfigLockRegistry") LockRegistry pgconfigLockRegistry) {
        log.debug("Creating {}", PgconfigLockProvider.class.getSimpleName());
        return new PgconfigLockProvider(pgconfigLockRegistry);
    }

    /**
     * @return
     */
    @Bean
    LockRegistry pgconfigLockRegistry(@Qualifier("pgconfigLockRepository") LockRepository pgconfigLockRepository) {
        log.debug("Creating {}", LockRegistry.class.getSimpleName());
        return new JdbcLockRegistry(pgconfigLockRepository);
    }

    @Bean
    LockRepository pgconfigLockRepository(
            @Qualifier("pgconfigDataSource") DataSource dataSource,
            @Value("${info.instance-id:}") String instanceId,
            @Qualifier("pgconfigTransactionManager") PlatformTransactionManager pgconfigTransactionManager) {

        log.debug("Creating {} for instance {}", LockRepository.class.getSimpleName(), instanceId);
        DefaultLockRepository lockRepository;
        if (StringUtils.hasLength(instanceId)) {
            lockRepository = new DefaultLockRepository(dataSource, instanceId);
        } else {
            lockRepository = new DefaultLockRepository(dataSource);
        }
        lockRepository.setTransactionManager(pgconfigTransactionManager);
        // override default table prefix "INT" by "RESOURCE_" (matching table definition
        // RESOURCE_LOCK in init.XXX.sql
        lockRepository.setPrefix("RESOURCE_");
        // time in ms to expire dead locks (10k is the default)
        lockRepository.setTimeToLive(300_000);
        // Explicitly initialize the lock repository to ensure transaction template is created
        lockRepository.afterSingletonsInstantiated();
        return lockRepository;
    }
}
