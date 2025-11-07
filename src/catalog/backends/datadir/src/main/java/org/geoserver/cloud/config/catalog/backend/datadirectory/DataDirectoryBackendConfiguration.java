/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.config.catalog.backend.datadirectory;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

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
        ModuleStatusImpl module = new ModuleStatusImpl("gs-cloud-backend-datadir", "DataDirectory loader");
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
        ExtendedCatalogFacade facade = new org.geoserver.catalog.plugin.DefaultMemoryCatalogFacade();
        if (converger.isPresent()) {
            log.info("Data directory catalog facade eventual consistency enforcement enabled");
            facade = buildEventuallyConsistentCatalogFacade(facade, converger.orElseThrow());
        } else {
            log.info("Data directory catalog facade eventual consistency enforcement disabled");
        }
        return facade;
    }

    /**
     * Wraps the catalog facade with eventual consistency enforcement capabilities.
     *
     * <p>This method decorates the base catalog facade with an {@link EventuallyConsistentCatalogFacade}
     * that provides resilience against out-of-order distributed event delivery in multi-node deployments.
     * The wrapper manages deferred catalog operations when dependencies are not yet available and
     * implements retry logic for query operations during convergence periods.
     *
     * <p>The retry behavior is configured via {@link EventualConsistencyConfig#getRetries()}, which
     * specifies wait intervals in milliseconds between retry attempts. If not configured or empty,
     * no retries are performed (though operations may still be deferred until dependencies arrive).
     *
     * <p>The enforcer is configured with a reference to the raw facade to enable direct access
     * for resolving pending operations once dependencies become available.
     *
     * @param facade the base catalog facade to wrap
     * @param tracker the enforcer that tracks pending operations and manages convergence state
     * @return the facade wrapped with eventual consistency enforcement, or the original facade if
     *     retries are not configured
     */
    private EventuallyConsistentCatalogFacade buildEventuallyConsistentCatalogFacade(
            ExtendedCatalogFacade facade, EventualConsistencyEnforcer tracker) {
        int[] waitMillis = new int[] {}; // no retries
        EventualConsistencyConfig ecConfig = dataDirectoryConfig.getEventualConsistency();
        List<Integer> retries = ecConfig.getRetries();
        if (retries != null && !retries.isEmpty()) {
            waitMillis = retries.stream().mapToInt(Integer::intValue).toArray();
            log.info("Data directory catalog facade eventual consistency retries in ms: {}", retries);
        }
        tracker.setRawFacade(facade);
        return new EventuallyConsistentCatalogFacade(facade, tracker, waitMillis);
    }

    protected @Bean @Override RepositoryGeoServerFacade geoserverFacade() {
        return new org.geoserver.config.plugin.RepositoryGeoServerFacadeImpl();
    }

    /**
     * Contributes the optimized parallel {@link GeoServerLoader}
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
    @Override
    public GeoServerLoader geoServerLoaderImpl(GeoServerSecurityManager securityManager) {
        log.info("Using optimized parallel data directory config loader");
        GeoServerResourceLoader resourceLoader = resourceLoader();
        GeoServerDataDirectory dataDirectory = new GeoServerDataDirectory(resourceLoader);
        Catalog rawCatalog = rawCatalog();
        LockingGeoServer geoserver = geoServer(rawCatalog);

        return new CloudDataDirectoryGeoServerLoader(dataDirectory, geoserver, securityManager);
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
        Objects.requireNonNull(path, "geoserver.backend.data-directory.location config property resolves to null");
        return path;
    }
}
