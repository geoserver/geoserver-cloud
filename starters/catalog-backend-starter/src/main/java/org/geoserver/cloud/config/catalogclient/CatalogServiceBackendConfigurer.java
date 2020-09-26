/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.catalogclient;

import lombok.Getter;
import org.geoserver.catalog.CatalogFacade;
import org.geoserver.cloud.catalog.client.impl.CatalogClientConfiguration;
import org.geoserver.cloud.catalog.client.impl.CatalogServiceCatalogFacade;
import org.geoserver.cloud.config.catalog.GeoServerBackendConfigurer;
import org.geoserver.config.DefaultGeoServerLoader;
import org.geoserver.config.GeoServerFacade;
import org.geoserver.config.GeoServerLoader;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.ResourceStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(CatalogClientConfiguration.class)
public class CatalogServiceBackendConfigurer implements GeoServerBackendConfigurer {

    private @Autowired @Getter ApplicationContext context;

    public @Override @Bean CatalogFacade catalogFacade() {
        return context.getBean(CatalogServiceCatalogFacade.class);
    }

    public @Override @Bean GeoServerFacade geoserverFacade() {
        return new org.geoserver.config.plugin.DefaultGeoServerFacade();
    }

    public @Override @Bean GeoServerLoader geoServerLoaderImpl() {
        GeoServerResourceLoader resourceLoader = resourceLoader();
        return new DefaultGeoServerLoader(resourceLoader);
    }

    public @Override @Bean GeoServerResourceLoader resourceLoader() {
        //        ResourceStore resourceStore = resourceStoreImpl();
        //        GeoServerResourceLoader resourceLoader = new
        // GeoServerResourceLoader(resourceStore);
        //        Path location = configProperties.getDataDirectory().getLocation();
        //        File dataDirectory = location.toFile();
        //        resourceLoader.setBaseDirectory(dataDirectory);
        //        return resourceLoader;
        return null;
    }

    public @Override @Bean ResourceStore resourceStoreImpl() {
        //        Path path = configProperties.getDataDirectory().getLocation();
        //        File dataDirectory = path.toFile();
        //        return new NoServletContextDataDirectoryResourceStore(dataDirectory);
        return null;
    }
}
