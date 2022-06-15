/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.datadirectory;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import org.geoserver.catalog.plugin.DefaultMemoryCatalogFacade;
import org.geoserver.catalog.plugin.ExtendedCatalogFacade;
import org.geoserver.cloud.autoconfigure.catalog.event.ConditionalOnCatalogEvents;
import org.geoserver.cloud.config.catalog.DataDirectoryProperties;
import org.geoserver.cloud.config.catalog.GeoServerBackendConfigurer;
import org.geoserver.cloud.config.catalog.GeoServerBackendProperties;
import org.geoserver.config.GeoServerLoader;
import org.geoserver.config.plugin.RepositoryGeoServerFacade;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.ResourceStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import java.io.File;
import java.nio.file.Path;
import java.util.Objects;

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
    @ConditionalOnCatalogEvents
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

    @DependsOn({
        "extensions",
        "wmsLoader",
        "wfsLoader",
        "wcsLoader",
        "wpsServiceLoader",
        "wmtsLoader"
    })
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

    @Bean(name = {"resourceStoreImpl"})
    public @Override ResourceStore resourceStoreImpl() {
        final @NonNull File dataDirectory = dataDirectoryFile().toFile();
        NoServletContextDataDirectoryResourceStore store =
                new NoServletContextDataDirectoryResourceStore(dataDirectory);
        store.setLockProvider(new NoServletContextFileLockProvider(dataDirectory));
        return store;
    }

    private Path dataDirectoryFile() {
        DataDirectoryProperties dataDirectoryConfig = this.backendConfig.getDataDirectory();
        Path path = dataDirectoryConfig.getLocation();
        Objects.requireNonNull(
                path, "geoserver.backend.data-directory.location config property resolves to null");
        return path;
    }
}
