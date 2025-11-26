/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.config.catalog.backend.datadirectory;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
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
import org.springframework.context.annotation.Primary;

/**
 * Spring configuration for GeoServer Cloud's traditional file-based data directory backend.
 *
 * <h2>Overview</h2>
 * <p>This configuration provides a GeoServer catalog and configuration backend that stores data in
 * a traditional file-based data directory structure, similar to vanilla GeoServer. The implementation
 * uses an in-memory catalog facade backed by file-based persistence through GeoServer's standard
 * XML serialization mechanisms.
 *
 * <h2>Storage Architecture</h2>
 * <ul>
 *  <li><b>Catalog</b> - In-memory facade ({@code DefaultMemoryCatalogFacade}) with XML file persistence
 *  <li><b>Configuration</b> - In-memory repository facade ({@code RepositoryGeoServerFacadeImpl}) with XML file persistence
 *  <li><b>Resources</b> - File-based resource store for styles, icons, templates, and other assets
 *  <li><b>Locking</b> - File-based locking for configuration consistency across nodes
 * </ul>
 *
 * <h2>Eventual Consistency Support</h2>
 * <p>In multi-node deployments, this backend can be configured with eventual consistency enforcement to
 * handle out-of-order distributed event delivery. When enabled via {@link DataDirectoryProperties},
 * the catalog facade is wrapped with {@link EventuallyConsistentCatalogFacade} which:
 * <ul>
 *  <li>Defers catalog operations when dependencies are not yet available
 *  <li>Implements configurable retry logic for query operations during convergence
 *  <li>Tracks pending operations and resolves them when dependencies arrive
 * </ul>
 *
 * <h2>Configuration</h2>
 * <p>Backend behavior is controlled through {@link DataDirectoryProperties}, typically configured via:
 * <pre>
 * geoserver.backend.data-directory.location=/path/to/datadir
 * geoserver.backend.data-directory.eventual-consistency.enabled=true
 * geoserver.backend.data-directory.eventual-consistency.retries=100,200,500
 * </pre>
 *
 * <h2>Bean Dependencies</h2>
 * <p>This configuration uses {@code @Configuration(proxyBeanMethods = false)} for optimal performance
 * and flexibility, allowing bean methods to declare their dependencies as method parameters rather than
 * calling other {@code @Bean} methods directly.
 *
 * <h2>Optimization</h2>
 * <p>Uses {@link CloudDataDirectoryGeoServerLoader} which provides optimized parallel loading of
 * catalog and configuration data compared to vanilla GeoServer's sequential loader.
 *
 * @since 1.0
 * @see GeoServerBackendConfigurer
 * @see DataDirectoryProperties
 * @see EventuallyConsistentCatalogFacade
 * @see CloudDataDirectoryGeoServerLoader
 */
@Configuration(proxyBeanMethods = false)
@Slf4j(topic = "org.geoserver.cloud.config.datadirectory")
public class DataDirectoryBackendConfiguration extends GeoServerBackendConfigurer {

    public DataDirectoryBackendConfiguration(DataDirectoryProperties dataDirectoryConfig) {

        log.info(
                "Loading geoserver config backend with {} from {}",
                DataDirectoryBackendConfiguration.class.getSimpleName(),
                dataDirectoryConfig.dataDirectory());
    }

    @Bean
    ModuleStatusImpl moduleStatus() {
        ModuleStatusImpl module = new ModuleStatusImpl("gs-cloud-backend-datadir", "DataDirectory loader");
        module.setAvailable(true);
        module.setEnabled(true);
        return module;
    }

    @Bean
    CatalogPlugin rawCatalog(
            CatalogProperties catalogProperties,
            GeoServerResourceLoader resourceLoader,
            ExtendedCatalogFacade catalogFacade,
            GeoServerConfigurationLock configurationLock) {

        boolean isolated = catalogProperties.isIsolated();
        CatalogPlugin rawCatalog = new LockingCatalog(configurationLock, catalogFacade, isolated);
        rawCatalog.setResourceLoader(resourceLoader);
        return rawCatalog;
    }

    @Bean(name = "geoServer")
    LockingGeoServer geoServer(@Qualifier("catalog") Catalog catalog, GeoServerConfigurationLock configurationLock) {
        LockingGeoServer gs = new LockingGeoServer(configurationLock, geoserverFacade());
        gs.setCatalog(catalog);
        return gs;
    }

    @Primary
    @Bean
    UpdateSequence updateSequence(
            @Qualifier("resourceStoreImpl") ResourceStore resourceStore, GeoServerResourceLoader resourceLoader) {
        GeoServerDataDirectory dd = new GeoServerDataDirectory(resourceLoader);
        XStreamPersisterFactory xpf = new XStreamPersisterFactory();
        return new DataDirectoryUpdateSequence(resourceStore, dd, xpf);
    }

    @Bean
    GeoServerConfigurationLock configurationLock(@Qualifier("resourceStoreImpl") ResourceStore resourceStoreImpl) {
        LockProvider lockProvider = resourceStoreImpl.getLockProvider();
        return new LockProviderGeoServerConfigurationLock(lockProvider);
    }

    @Bean
    ExtendedCatalogFacade catalogFacade(
            DataDirectoryProperties config, Optional<EventualConsistencyEnforcer> converger) {
        ExtendedCatalogFacade facade = new org.geoserver.catalog.plugin.DefaultMemoryCatalogFacade();
        if (converger.isPresent()) {
            log.info("Data directory catalog facade eventual consistency enforcement enabled");
            facade = buildEventuallyConsistentCatalogFacade(facade, converger.orElseThrow(), config);
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
            ExtendedCatalogFacade facade, EventualConsistencyEnforcer tracker, DataDirectoryProperties config) {
        int[] waitMillis = new int[] {}; // no retries
        EventualConsistencyConfig ecConfig = config.getEventualConsistency();
        List<Integer> retries = ecConfig.getRetries();
        if (retries != null && !retries.isEmpty()) {
            waitMillis = retries.stream().mapToInt(Integer::intValue).toArray();
            log.info("Data directory catalog facade eventual consistency retries in ms: {}", retries);
        }
        tracker.setRawFacade(facade);
        return new EventuallyConsistentCatalogFacade(facade, tracker, waitMillis);
    }

    @Bean
    RepositoryGeoServerFacade geoserverFacade() {
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
    GeoServerLoader geoServerLoaderImpl(
            @Qualifier("rawCatalog") Catalog rawCatalog,
            LockingGeoServer geoServer,
            GeoServerSecurityManager securityManager,
            GeoServerResourceLoader resourceLoader) {
        log.info("Using optimized parallel data directory config loader");
        GeoServerDataDirectory dataDirectory = new GeoServerDataDirectory(resourceLoader);
        return new CloudDataDirectoryGeoServerLoader(dataDirectory, geoServer, securityManager);
    }

    @Bean
    GeoServerResourceLoader resourceLoader(
            @Qualifier("resourceStoreImpl") ResourceStore resourceStoreImpl, DataDirectoryProperties config) {
        GeoServerResourceLoader resourceLoader = new GeoServerResourceLoader(resourceStoreImpl);
        final @NonNull Path datadir = config.dataDirectory();
        log.debug("geoserver.backend.data-directory.location: {}", datadir);
        resourceLoader.setBaseDirectory(datadir.toFile());
        return resourceLoader;
    }

    @Bean(name = {"resourceStoreImpl"})
    ResourceStore resourceStoreImpl(DataDirectoryProperties config) {
        File dataDirectory = config.dataDirectory().toFile();
        NoServletContextDataDirectoryResourceStore store =
                new NoServletContextDataDirectoryResourceStore(dataDirectory);
        store.setLockProvider(new NoServletContextFileLockProvider(dataDirectory));
        return store;
    }
}
