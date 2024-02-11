/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.catalog.backend.datadirectory;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import org.geoserver.GeoServerConfigurationLock;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.plugin.CatalogPlugin;
import org.geoserver.catalog.plugin.ExtendedCatalogFacade;
import org.geoserver.catalog.plugin.locking.LockProviderGeoServerConfigurationLock;
import org.geoserver.catalog.plugin.locking.LockingCatalog;
import org.geoserver.catalog.plugin.locking.LockingGeoServer;
import org.geoserver.cloud.catalog.backend.datadir.EventualConsistencyEnforcer;
import org.geoserver.cloud.catalog.backend.datadir.EventuallyConsistentCatalogFacade;
import org.geoserver.cloud.config.catalog.backend.core.CatalogProperties;
import org.geoserver.cloud.config.catalog.backend.core.GeoServerBackendConfigurer;
import org.geoserver.cloud.config.catalog.backend.datadirectory.DataDirectoryProperties.EventualConsistencyConfig;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.config.GeoServerLoader;
import org.geoserver.config.plugin.RepositoryGeoServerFacade;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.ModuleStatusImpl;
import org.geoserver.platform.config.UpdateSequence;
import org.geoserver.platform.resource.LockProvider;
import org.geoserver.platform.resource.ResourceStore;
import org.geoserver.security.GeoServerSecurityManager;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** */
@Configuration(proxyBeanMethods = true)
@Slf4j(topic = "org.geoserver.cloud.config.datadirectory")
public class DataDirectoryBackendConfiguration extends GeoServerBackendConfigurer {

    private final CatalogProperties catalogProperties;

    private final DataDirectoryProperties dataDirectoryConfig;

    private final Optional<EventualConsistencyEnforcer> converger;

    public DataDirectoryBackendConfiguration(
            DataDirectoryProperties dataDirectoryConfig,
            CatalogProperties catalogProperties,
            Optional<EventualConsistencyEnforcer> converger) {
        this.dataDirectoryConfig = dataDirectoryConfig;
        this.catalogProperties = catalogProperties;
        this.converger = converger;
        log.info(
                "Loading geoserver config backend with {} from {}",
                DataDirectoryBackendConfiguration.class.getSimpleName(),
                dataDirectoryConfig.getLocation());
    }

    @Bean
    ModuleStatusImpl moduleStatus() {
        ModuleStatusImpl module =
                new ModuleStatusImpl("gs-cloud-backend-datadir", "DataDirectory loader");
        module.setAvailable(true);
        module.setEnabled(true);
        return module;
    }

    @Bean
    CatalogPlugin rawCatalog() {
        boolean isolated = catalogProperties.isIsolated();
        GeoServerConfigurationLock configurationLock = configurationLock();
        ExtendedCatalogFacade catalogFacade = catalogFacade();
        GeoServerResourceLoader resourceLoader = resourceLoader();
        CatalogPlugin rawCatalog = new LockingCatalog(configurationLock, catalogFacade, isolated);
        rawCatalog.setResourceLoader(resourceLoader);
        return rawCatalog;
    }

    @Bean(name = "geoServer")
    LockingGeoServer geoServer(@Qualifier("catalog") Catalog catalog) {

        GeoServerConfigurationLock configurationLock = configurationLock();
        LockingGeoServer gs = new LockingGeoServer(configurationLock, geoserverFacade());
        gs.setCatalog(catalog);
        return gs;
    }

    protected @Bean @Override UpdateSequence updateSequence() {
        ResourceStore resourceStore = resourceStoreImpl();
        GeoServerDataDirectory dd = new GeoServerDataDirectory(resourceLoader());
        XStreamPersisterFactory xpf = new XStreamPersisterFactory();
        return new DataDirectoryUpdateSequence(resourceStore, dd, xpf);
    }

    protected @Bean @Override GeoServerConfigurationLock configurationLock() {
        LockProvider lockProvider = resourceStoreImpl().getLockProvider();
        return new LockProviderGeoServerConfigurationLock(lockProvider);
    }

    protected @Bean @Override ExtendedCatalogFacade catalogFacade() {
        var memory = new org.geoserver.catalog.plugin.DefaultMemoryCatalogFacade();
        if (converger.isEmpty()) {
            return memory;
        }
        log.info("Data directory catalog facade eventual consistency enforcement enabled");

        int[] waitMillis = new int[] {}; // no retries
        EventualConsistencyConfig ecConfig = dataDirectoryConfig.getEventualConsistency();
        if (ecConfig != null && ecConfig.isEnabled()) {
            List<Integer> retries = ecConfig.getRetries();
            if (retries != null && !retries.isEmpty()) {
                waitMillis = retries.stream().mapToInt(Integer::intValue).toArray();
                log.info(
                        "Data directory catalog facade eventual consistency retries in ms: {}",
                        retries);
            }
        }
        EventualConsistencyEnforcer tracker = converger.orElseThrow();
        tracker.setRawFacade(memory);
        return new EventuallyConsistentCatalogFacade(memory, tracker, waitMillis);
    }

    protected @Bean @Override RepositoryGeoServerFacade geoserverFacade() {
        return new org.geoserver.config.plugin.RepositoryGeoServerFacadeImpl();
    }

    /**
     * Contributes the default {@link GeoServerLoader} if {@code
     * geoserver.backend.data-directory.parallel-loader=false}
     */
    @DependsOn({
        "extensions",
        "wmsLoader",
        "wfsLoader",
        "wcsLoader",
        "wpsServiceLoader",
        "wmtsLoader",
        "geoServerSecurityManager"
    })
    @Bean(name = "geoServerLoaderImpl")
    @ConditionalOnProperty(
            name = "geoserver.backend.data-directory.parallel-loader",
            havingValue = "false",
            matchIfMissing = false)
    protected @Override GeoServerLoader geoServerLoaderImpl() {
        log.info("Using default data directory config loader");
        UpdateSequence updateSequence = updateSequence();
        GeoServerResourceLoader resourceLoader = resourceLoader();
        Catalog rawCatalog = rawCatalog();
        LockingGeoServer geoserver = geoServer(rawCatalog);
        return new DataDirectoryGeoServerLoader(
                updateSequence, resourceLoader, geoserver, rawCatalog);
    }

    /**
     * Contributes the optimized parallel {@link GeoServerLoader} if {@code
     * geoserver.backend.data-directory.parallel-loader=true} (default behavior).
     */
    @DependsOn({
        "extensions",
        "wmsLoader",
        "wfsLoader",
        "wcsLoader",
        "wpsServiceLoader",
        "wmtsLoader",
        "geoServerSecurityManager"
    })
    @Bean(name = "geoServerLoaderImpl")
    @ConditionalOnProperty(
            name = "geoserver.backend.data-directory.parallel-loader",
            havingValue = "true",
            matchIfMissing = true)
    GeoServerLoader geoServerLoaderImplParallel(GeoServerSecurityManager securityManager) {
        log.info("Using optimized parallel data directory config loader");
        UpdateSequence updateSequence = updateSequence();
        GeoServerResourceLoader resourceLoader = resourceLoader();
        Catalog rawCatalog = rawCatalog();
        LockingGeoServer geoserver = geoServer(rawCatalog);

        return new ParallelDataDirectoryGeoServerLoader(
                updateSequence, resourceLoader, geoserver, rawCatalog, securityManager);
    }

    protected @Bean @Override GeoServerResourceLoader resourceLoader() {
        ResourceStore resourceStoreImpl = resourceStoreImpl();
        GeoServerResourceLoader resourceLoader = new GeoServerResourceLoader(resourceStoreImpl);
        final @NonNull Path datadir = dataDirectoryFile();
        log.debug("geoserver.backend.data-directory.location: {}", datadir);
        resourceLoader.setBaseDirectory(datadir.toFile());
        return resourceLoader;
    }

    @Bean(name = {"resourceStoreImpl"})
    protected @Override ResourceStore resourceStoreImpl() {
        final @NonNull File dataDirectory = dataDirectoryFile().toFile();
        NoServletContextDataDirectoryResourceStore store =
                new NoServletContextDataDirectoryResourceStore(dataDirectory);
        store.setLockProvider(new NoServletContextFileLockProvider(dataDirectory));
        return store;
    }

    private Path dataDirectoryFile() {
        DataDirectoryProperties config = this.dataDirectoryConfig;
        Path path = config.getLocation();
        Objects.requireNonNull(
                path, "geoserver.backend.data-directory.location config property resolves to null");
        return path;
    }
}
