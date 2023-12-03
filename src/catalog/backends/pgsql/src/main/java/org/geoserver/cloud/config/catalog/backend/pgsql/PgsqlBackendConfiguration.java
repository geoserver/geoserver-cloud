/*
 * (c) 2023 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.catalog.backend.pgsql;

import org.geoserver.GeoServerConfigurationLock;
import org.geoserver.catalog.plugin.CatalogPlugin;
import org.geoserver.catalog.plugin.ExtendedCatalogFacade;
import org.geoserver.catalog.plugin.locking.LockProviderGeoServerConfigurationLock;
import org.geoserver.cloud.backend.pgsql.PgsqlBackendBuilder;
import org.geoserver.cloud.backend.pgsql.catalog.PgsqlCatalogFacade;
import org.geoserver.cloud.backend.pgsql.config.PgsqlConfigRepository;
import org.geoserver.cloud.backend.pgsql.config.PgsqlGeoServerFacade;
import org.geoserver.cloud.backend.pgsql.config.PgsqlUpdateSequence;
import org.geoserver.cloud.backend.pgsql.resource.FileSystemResourceStoreCache;
import org.geoserver.cloud.backend.pgsql.resource.PgsqlLockProvider;
import org.geoserver.cloud.backend.pgsql.resource.PgsqlResourceStore;
import org.geoserver.cloud.config.catalog.backend.core.CatalogProperties;
import org.geoserver.cloud.config.catalog.backend.core.GeoServerBackendConfigurer;
import org.geoserver.cloud.config.catalog.backend.pgsql.DatabaseMigrationConfiguration.Migrations;
import org.geoserver.config.GeoServerLoader;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.LockProvider;
import org.geoserver.platform.resource.ResourceStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.jdbc.lock.DefaultLockRepository;
import org.springframework.integration.jdbc.lock.JdbcLockRegistry;
import org.springframework.integration.jdbc.lock.LockRepository;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;

/**
 * @since 1.4
 */
@Configuration(proxyBeanMethods = true)
public class PgsqlBackendConfiguration extends GeoServerBackendConfigurer {

    private String instanceId;
    private DataSource dataSource;
    private CatalogProperties catalogProperties;

    PgsqlBackendConfiguration(
            @Value("${info.instance-id:}") String instanceId,
            @Qualifier("pgsqlConfigDatasource") DataSource dataSource,
            Migrations migrations,
            CatalogProperties catalogProperties) {
        this.instanceId = instanceId;
        this.dataSource = dataSource;
        this.catalogProperties = catalogProperties;
    }

    @Bean
    CatalogPlugin rawCatalog() {
        boolean isolated = catalogProperties.isIsolated();
        CatalogPlugin rawCatalog = new CatalogPlugin(isolated);

        PgsqlCatalogFacade rawFacade = catalogFacade();

        ExtendedCatalogFacade facade =
                PgsqlBackendBuilder.createResolvingCatalogFacade(rawCatalog, rawFacade);
        rawCatalog.setFacade(facade);

        GeoServerResourceLoader resourceLoader = resourceLoader();
        rawCatalog.setResourceLoader(resourceLoader);
        return rawCatalog;
    }

    @Bean(name = "pgsqlCongigJdbcTemplate")
    JdbcTemplate template() {
        JdbcTemplate template = new JdbcTemplate(dataSource);
        return template;
    }

    protected @Bean @Override GeoServerConfigurationLock configurationLock() {
        LockProvider lockProvider = pgsqlLockProvider();
        return new LockProviderGeoServerConfigurationLock(lockProvider);
    }

    protected @Bean @Override PgsqlUpdateSequence updateSequence() {
        return new PgsqlUpdateSequence(dataSource, geoserverFacade());
    }

    protected @Bean @Override PgsqlCatalogFacade catalogFacade() {
        return new PgsqlCatalogFacade(template());
    }

    protected @Bean @Override GeoServerLoader geoServerLoaderImpl() {
        return new PgsqlGeoServerLoader(resourceLoader(), configurationLock());
    }

    @Bean
    PgsqlConfigRepository configRepository() {
        return new PgsqlConfigRepository(template());
    }

    protected @Bean @Override PgsqlGeoServerFacade geoserverFacade() {
        return new PgsqlGeoServerFacade(configRepository());
    }

    protected @Bean @Override ResourceStore resourceStoreImpl() {
        FileSystemResourceStoreCache resourceStoreCache = pgsqlFileSystemResourceStoreCache();
        JdbcTemplate template = template();
        PgsqlLockProvider lockProvider = pgsqlLockProvider();
        return new PgsqlResourceStore(resourceStoreCache, template, lockProvider);
    }

    @Bean
    FileSystemResourceStoreCache pgsqlFileSystemResourceStoreCache() {
        return FileSystemResourceStoreCache.newTempDirInstance();
    }

    protected @Bean @Override PgsqlGeoServerResourceLoader resourceLoader() {
        return new PgsqlGeoServerResourceLoader(resourceStoreImpl());
    }

    @Bean
    PgsqlLockProvider pgsqlLockProvider() {
        return new PgsqlLockProvider(pgsqlLockRegistry());
    }

    /**
     * @return
     */
    private LockRegistry pgsqlLockRegistry() {
        return new JdbcLockRegistry(pgsqlLockRepository());
    }

    @Bean
    LockRepository pgsqlLockRepository() {
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
