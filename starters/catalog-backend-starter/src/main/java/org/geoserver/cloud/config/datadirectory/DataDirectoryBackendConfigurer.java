/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.datadirectory;

import java.io.File;
import java.nio.file.Path;
import javax.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.catalog.CatalogFacade;
import org.geoserver.cloud.config.catalog.GeoServerBackendConfigurer;
import org.geoserver.cloud.config.catalog.GeoServerBackendProperties;
import org.geoserver.config.DefaultGeoServerLoader;
import org.geoserver.config.GeoServerFacade;
import org.geoserver.config.GeoServerLoader;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.ResourceStore;
import org.geoserver.wcs.WCSXStreamLoader;
import org.geoserver.wfs.WFSXStreamLoader;
import org.geoserver.wms.WMSXStreamLoader;
import org.geoserver.wps.WPSXStreamLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

/** */
@Configuration(proxyBeanMethods = true)
@Slf4j
public class DataDirectoryBackendConfigurer implements GeoServerBackendConfigurer {

    private @Autowired @Getter ApplicationContext context;
    private @Autowired GeoServerBackendProperties configProperties;

    public @PostConstruct void log() {
        log.info("Loading geoserver config backend with {}", getClass().getSimpleName());
    }

    public @Override @Bean CatalogFacade catalogFacade() {
        return new org.geoserver.catalog.plugin.DefaultCatalogFacade();
    }

    public @Override @Bean GeoServerFacade geoserverFacade() {
        return new org.geoserver.config.plugin.DefaultGeoServerFacade();
    }

    @DependsOn({
        "extensions",
        "wmsxStreamLoader",
        "wfsxStreamLoader",
        "wcsxStreamLoader",
        "wpsxStreamLoader"
    })
    public @Override @Bean GeoServerLoader geoServerLoaderImpl() {
        GeoServerResourceLoader resourceLoader = resourceLoader();
        return new DefaultGeoServerLoader(resourceLoader);
    }

    public @Override @Bean GeoServerResourceLoader resourceLoader() {
        ResourceStore resourceStore = resourceStoreImpl();
        GeoServerResourceLoader resourceLoader = new GeoServerResourceLoader(resourceStore);
        Path location = configProperties.getDataDirectory().getLocation();
        log.debug("geoserver.backend.data-directory.location:" + location);
        File dataDirectory = location.toFile();
        resourceLoader.setBaseDirectory(dataDirectory);
        return resourceLoader;
    }

    public @Override @Bean ResourceStore resourceStoreImpl() {
        Path path = configProperties.getDataDirectory().getLocation();
        File dataDirectory = path.toFile();
        return new NoServletContextDataDirectoryResourceStore(dataDirectory);
    }

    public @Bean WMSXStreamLoader wmsxStreamLoader() {
        return new WMSXStreamLoader(resourceLoader());
    }

    public @Bean WFSXStreamLoader wfsxStreamLoader() {
        return new WFSXStreamLoader(resourceLoader());
    }

    public @Bean WCSXStreamLoader wcsxStreamLoader() {
        return new WCSXStreamLoader(resourceLoader());
    }

    public @Bean WPSXStreamLoader wpsxStreamLoader() {
        return new WPSXStreamLoader(resourceLoader());
    }
}
