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
import org.geoserver.cloud.backend.pgconfig.coverage.MosaicSyncingResourcePool;
import org.geoserver.cloud.backend.pgconfig.resource.DbBackedFileSynchronizer;
import org.geoserver.cloud.backend.pgconfig.resource.FileSystemResourceStoreCache;
import org.geoserver.cloud.backend.pgconfig.resource.PgconfigLockProvider;
import org.geoserver.cloud.backend.pgconfig.resource.PgconfigResourceStore;
import org.geoserver.cloud.config.catalog.backend.core.GeoServerBackendConfigurer;
import org.geoserver.cloud.config.catalog.backend.core.RawCatalogCustomizer;
import org.geoserver.cloud.config.catalog.backend.pgconfig.DatabaseMigrationConfiguration.Migrations;
import org.geoserver.config.GeoServerLoader;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.LockProvider;
import org.geoserver.platform.resource.ResourceStore;
import org.geoserver.security.GeoServerSecurityManager;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.jdbc.lock.DefaultLockRepository;
import org.springframework.integration.jdbc.lock.JdbcLockRegistry;
import org.springframework.integration.jdbc.lock.LockRepository;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;

/**
 * @since 1.4
 */
@Configuration(proxyBeanMethods = true)
@EnableConfigurationProperties(PgconfigBackendProperties.class)
@Slf4j(topic = "org.geoserver.cloud.config.catalog.backend.pgconfig")
public class PgconfigBackendConfiguration extends GeoServerBackendConfigurer {

    private String instanceId;
    private DataSource dataSource;
    private PgconfigBackendProperties configProperties;

    /**
     * @param instanceId used as client-id for the {@link #pgconfigLockRepository() LockRepository}
     * @param dataSource DataSource for {@link #template()}, {@link #pgconfigLockRepository()}, and
     *     {@link #updateSequence()}
     * @param catalogProperties properties for {@link #rawCatalog()}
     * @param migrations required to make sure the migrations ran before this configuration takes
     *     place
     */
    PgconfigBackendConfiguration(
            @Value("${info.instance-id:}") String instanceId,
            @Qualifier("pgconfigDataSource") DataSource dataSource,
            PgconfigBackendProperties configProperties,
            Migrations migrations) {
        this.instanceId = instanceId;
        this.dataSource = dataSource;
        this.configProperties = configProperties;
        log.info(
                "Loading geoserver config backend with {}. {}",
                PgconfigBackendConfiguration.class.getSimpleName(),
                migrations);
    }

    @Bean
    @Override
    protected ExtendedCatalogFacade catalogFacade() {
        return new PgconfigBackendBuilder(dataSource).createCatalogFacade();
    }

    @Bean(name = "pcconfigJdbcTemplate")
    JdbcTemplate template() {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    @Override
    protected GeoServerConfigurationLock configurationLock() {
        LockProvider lockProvider = pgconfigLockProvider();
        return new LockProviderGeoServerConfigurationLock(lockProvider);
    }

    @Bean
    @Override
    protected PgconfigUpdateSequence updateSequence() {
        return new PgconfigUpdateSequence(dataSource, geoserverFacade());
    }

    @Bean
    @Override
    protected GeoServerLoader geoServerLoaderImpl(GeoServerSecurityManager securityManager) {
        log.debug("Creating GeoServerLoader {}", PgconfigGeoServerLoader.class.getSimpleName());
        return new PgconfigGeoServerLoader(resourceLoader(), configurationLock());
    }

    @Bean
    PgconfigConfigRepository configRepository() {
        return new PgconfigConfigRepository(template());
    }

    @Bean
    @Override
    protected PgconfigGeoServerFacade geoserverFacade() {
        return new PgconfigGeoServerFacade(configRepository());
    }

    @Bean
    @Override
    protected PgconfigResourceStore resourceStoreImpl() {
        log.debug("Creating ResourceStore {}", PgconfigResourceStore.class.getSimpleName());
        FileSystemResourceStoreCache resourceStoreCache = pgconfigFileSystemResourceStoreCache();
        JdbcTemplate template = template();
        PgconfigLockProvider lockProvider = pgconfigLockProvider();
        Predicate<String> localOnlyFilter = PgconfigResourceStore.defaultIgnoredResources();
        Predicate<String> dbBackedFilePatterns =
                PgconfigResourceStore.antPathMatcher(configProperties.getDbBackedFilePatterns());
        return new PgconfigResourceStore(
                resourceStoreCache, template, lockProvider, localOnlyFilter, dbBackedFilePatterns);
    }

    @Bean
    FileSystemResourceStoreCache pgconfigFileSystemResourceStoreCache() {
        Predicate<String> noClobber = PgconfigResourceStore.antPathMatcher(configProperties.getDbBackedFilePatterns());
        return FileSystemResourceStoreCache.newTempDirInstance(noClobber);
    }

    /**
     * Keeps db-backed files written with raw file I/O (e.g. ImageMosaic config files rewritten by gt-imagemosaic)
     * consistent between each pod's local resource cache and the resource store, making them visible to every pod.
     *
     * @see MosaicSyncingResourcePool
     */
    @Bean
    DbBackedFileSynchronizer pgconfigDbBackedFileSynchronizer() {
        PgconfigResourceStore resourceStore = resourceStoreImpl();
        return new DbBackedFileSynchronizer(resourceStore, pgconfigFileSystemResourceStoreCache());
    }

    /**
     * Installs {@link MosaicSyncingResourcePool} on the raw catalog, making REST-created ImageMosaics work across pods
     * on the pgconfig backend.
     */
    @Bean
    RawCatalogCustomizer pgconfigMosaicSyncCustomizer() {
        DbBackedFileSynchronizer synchronizer = pgconfigDbBackedFileSynchronizer();
        return rawCatalog -> rawCatalog.setResourcePool(new MosaicSyncingResourcePool(rawCatalog, synchronizer));
    }

    @Bean
    @Override
    protected GeoServerResourceLoader resourceLoader() {
        log.debug("Creating GeoServerResourceLoader {}", PgconfigGeoServerResourceLoader.class.getSimpleName());
        ResourceStore resourceStore = resourceStoreImpl();
        return new PgconfigGeoServerResourceLoader(resourceStore);
    }

    @Bean
    PgconfigLockProvider pgconfigLockProvider() {
        log.debug("Creating {}", PgconfigLockProvider.class.getSimpleName());
        return new PgconfigLockProvider(pgconfigLockRegistry());
    }

    /**
     * @return
     */
    private LockRegistry pgconfigLockRegistry() {
        log.debug("Creating {}", LockRegistry.class.getSimpleName());
        return new JdbcLockRegistry(pgconfigLockRepository());
    }

    @Bean
    LockRepository pgconfigLockRepository() {
        log.debug("Creating {} for instance {}", LockRepository.class.getSimpleName(), this.instanceId);
        String id = this.instanceId;
        DefaultLockRepository lockRepository;
        if (StringUtils.hasLength(id)) {
            lockRepository = new DefaultLockRepository(dataSource, id);
        } else {
            lockRepository = new DefaultLockRepository(dataSource);
        }
        // override default table prefix "INT" by "RESOURCE_" (matching table definition
        // RESOURCE_LOCK in init.XXX.sql
        lockRepository.setPrefix("RESOURCE_");
        // time in ms to expire dead locks (10k is the default)
        lockRepository.setTimeToLive(300_000);
        return lockRepository;
    }
}
