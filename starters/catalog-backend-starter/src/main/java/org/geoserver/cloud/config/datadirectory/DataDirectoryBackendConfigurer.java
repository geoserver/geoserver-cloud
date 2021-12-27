/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.datadirectory;

import java.io.File;
import java.nio.file.Path;
import java.util.Objects;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.catalog.plugin.DefaultMemoryCatalogFacade;
import org.geoserver.catalog.plugin.ExtendedCatalogFacade;
import org.geoserver.cloud.autoconfigure.bus.ConditionalOnGeoServerRemoteEventsEnabled;
import org.geoserver.cloud.config.catalog.DataDirectoryProperties;
import org.geoserver.cloud.config.catalog.GeoServerBackendConfigurer;
import org.geoserver.cloud.config.catalog.GeoServerBackendProperties;
import org.geoserver.config.GeoServerLoader;
import org.geoserver.config.plugin.RepositoryGeoServerFacade;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.FileLockProvider;
import org.geoserver.platform.resource.ResourceStore;
import org.geoserver.wcs.WCSXStreamLoader;
import org.geoserver.wfs.WFSXStreamLoader;
import org.geoserver.wms.WMSXStreamLoader;
import org.geoserver.wps.WPSXStreamLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

/** */
@Configuration(proxyBeanMethods = true)
@Slf4j(topic = "org.geoserver.cloud.config.datadirectory")
public class DataDirectoryBackendConfigurer implements GeoServerBackendConfigurer {

    private GeoServerBackendProperties backendConfig;

    @Autowired
    public DataDirectoryBackendConfigurer(GeoServerBackendProperties backendConfig) {
        this.backendConfig = backendConfig;
        log.info(
                "Loading geoserver config backend with {}",
                DataDirectoryBackendConfigurer.class.getSimpleName());
    }

    @Autowired
    @ConditionalOnGeoServerRemoteEventsEnabled
    public @Bean DataDirectoryRemoteEventProcessor dataDirectoryRemoteEventProcessor(
            @Qualifier("geoserverFacade") RepositoryGeoServerFacade configFacade,
            @Qualifier("catalogFacade") ExtendedCatalogFacade catalogFacade) {
        return new DataDirectoryRemoteEventProcessor(configFacade, catalogFacade);
    }

    public /* @Override */ @Bean DefaultMemoryCatalogFacade catalogFacade() {
        return new org.geoserver.catalog.plugin.DefaultMemoryCatalogFacade();
    }

    public /* @Override */ @Bean RepositoryGeoServerFacade geoserverFacade() {
        return new org.geoserver.config.plugin.RepositoryGeoServerFacadeImpl();
    }

    @DependsOn({"extensions", "wmsLoader", "wfsLoader", "wcsLoader", "wpsServiceLoader"})
    public @Override @Bean GeoServerLoader geoServerLoaderImpl() {
        return new DataDirectoryGeoServerLoader(resourceLoader());
    }

    public @Override @Bean GeoServerResourceLoader resourceLoader() {
        ResourceStore resourceStoreImpl = resourceStoreImpl();
        GeoServerResourceLoader resourceLoader = new GeoServerResourceLoader(resourceStoreImpl);
        final @NonNull Path datadir = dataDirectoryFile();
        log.debug("geoserver.backend.data-directory.location:" + datadir);
        resourceLoader.setBaseDirectory(datadir.toFile());
        return resourceLoader;
    }

    public @Override @Bean ResourceStore resourceStoreImpl() {
        final @NonNull File dataDirectory = dataDirectoryFile().toFile();
        NoServletContextDataDirectoryResourceStore store =
                new NoServletContextDataDirectoryResourceStore(dataDirectory);
        store.setLockProvider(new FileLockProvider(dataDirectory));
        return store;
    }

    private Path dataDirectoryFile() {
        DataDirectoryProperties dataDirectoryConfig = this.backendConfig.getDataDirectory();
        Path path = dataDirectoryConfig.getLocation();
        Objects.requireNonNull(
                path, "geoserver.backend.data-directory.location config property resolves to null");
        return path;
    }

    /**
     * Provide {@code wmsLoader} if not loaded from {@code
     * gs-wms-<version>.jar!/applicationContext.xml#wmsLoader}
     */
    public @Bean WMSXStreamLoader wmsLoader(GeoServerResourceLoader resourceLoader) {
        return new WMSXStreamLoader(resourceLoader);
    }

    /**
     * Provide {@code wfsLoader} if not loaded from {@code
     * gs-wfs-<version>.jar!/applicationContext.xml#wfsLoader}
     */
    public @Bean WFSXStreamLoader wfsLoader(GeoServerResourceLoader resourceLoader) {
        return new WFSXStreamLoader(resourceLoader);
    }

    /**
     * Provide {@code wcsLoader} if not loaded from {@code
     * gs-wcs-<version>.jar!/applicationContext.xml#wcsLoader}
     */
    public @Bean WCSXStreamLoader wcsLoader(GeoServerResourceLoader resourceLoader) {
        return new WCSXStreamLoader(resourceLoader);
    }

    /**
     * Provide {@code wcsLoader} if not loaded from {@code
     * gs-wps-<version>.jar!/applicationContext.xml#wpsServiceLoader}
     */
    public @Bean WPSXStreamLoader wpsServiceLoader(GeoServerResourceLoader resourceLoader) {
        return new WPSXStreamLoader(resourceLoader);
    }
}
