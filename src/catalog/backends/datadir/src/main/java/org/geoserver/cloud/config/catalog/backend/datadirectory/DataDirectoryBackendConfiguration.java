/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.catalog.backend.datadirectory;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import org.geoserver.catalog.plugin.DefaultMemoryCatalogFacade;
import org.geoserver.cloud.config.catalog.backend.core.GeoServerBackendConfigurer;
import org.geoserver.config.GeoServerLoader;
import org.geoserver.config.plugin.RepositoryGeoServerFacade;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.ResourceStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import java.io.File;
import java.nio.file.Path;
import java.util.Objects;

/** */
@Configuration(proxyBeanMethods = true)
@EnableConfigurationProperties(DataDirectoryProperties.class)
@Slf4j(topic = "org.geoserver.cloud.config.datadirectory")
public class DataDirectoryBackendConfiguration implements GeoServerBackendConfigurer {

    private DataDirectoryProperties dataDirectoryConfig;

    @Autowired
    public DataDirectoryBackendConfiguration(DataDirectoryProperties dataDirectoryConfig) {
        this.dataDirectoryConfig = dataDirectoryConfig;
        log.info(
                "Loading geoserver config backend with {}",
                DataDirectoryBackendConfiguration.class.getSimpleName());
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
        DataDirectoryProperties dataDirectoryConfig = this.dataDirectoryConfig;
        Path path = dataDirectoryConfig.getLocation();
        Objects.requireNonNull(
                path, "geoserver.backend.data-directory.location config property resolves to null");
        return path;
    }
}
