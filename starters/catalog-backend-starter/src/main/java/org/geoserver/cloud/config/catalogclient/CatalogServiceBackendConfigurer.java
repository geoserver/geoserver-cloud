/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.catalogclient;

import java.io.File;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.catalog.CatalogFacade;
import org.geoserver.cloud.catalog.client.impl.CatalogClientConfiguration;
import org.geoserver.cloud.catalog.client.impl.CatalogServiceCatalogFacade;
import org.geoserver.cloud.catalog.client.impl.CatalogServiceGeoServerFacade;
import org.geoserver.cloud.catalog.client.impl.CatalogServiceResourceStore;
import org.geoserver.cloud.catalog.client.reactivefeign.ResourceStoreFallbackFactory;
import org.geoserver.cloud.config.catalog.GeoServerBackendConfigurer;
import org.geoserver.cloud.config.catalog.GeoServerBackendProperties;
import org.geoserver.config.GeoServerFacade;
import org.geoserver.config.GeoServerLoader;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.FileSystemResourceStore;
import org.geoserver.platform.resource.ResourceStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(CatalogClientConfiguration.class)
@Slf4j
public class CatalogServiceBackendConfigurer implements GeoServerBackendConfigurer {

    private @Autowired @Getter ApplicationContext context;

    private @Autowired CatalogServiceCatalogFacade catalogClientFacade;
    private @Autowired CatalogServiceGeoServerFacade configClientFacade;
    private @Autowired CatalogServiceResourceStore catalogServiceResourceStore;
    private @Autowired ResourceStoreFallbackFactory resourceStoreFallbackFactory;

    private @Autowired GeoServerBackendProperties configProps;

    public @Override @Bean CatalogFacade catalogFacade() {
        return catalogClientFacade;
    }

    public @Override @Bean GeoServerFacade geoserverFacade() {
        return configClientFacade;
    }

    public @Override @Bean CatalogServiceResourceStore resourceStoreImpl() {
        CatalogServiceResourceStore store = catalogServiceResourceStore;
        File cacheDirectory = configProps.getCatalogService().getCacheDirectory();
        if (null != cacheDirectory) {
            store.setLocalCacheDirectory(cacheDirectory);
        }
        return store;
    }

    public @Override @Bean GeoServerLoader geoServerLoaderImpl() {
        return new CatalogServiceGeoServerLoader(resourceLoader());
    }

    public @Override @Bean GeoServerResourceLoader resourceLoader() {
        ResourceStore fallbackResourceStore = catalogServiceFallbackResourceStore();
        if (fallbackResourceStore != null) {
            log.info(
                    "Using fallback ResourceStore {}",
                    fallbackResourceStore.getClass().getCanonicalName());
            resourceStoreFallbackFactory.setFallback(fallbackResourceStore);
        }
        CatalogServiceResourceStore resourceStore = resourceStoreImpl();
        GeoServerResourceLoader resourceLoader = new GeoServerResourceLoader(resourceStore);
        File cacheDirectory = configProps.getCatalogService().getCacheDirectory();
        if (null != cacheDirectory) {
            resourceLoader.setBaseDirectory(cacheDirectory);
        }
        return resourceLoader;
    }

    public @Bean ResourceStore catalogServiceFallbackResourceStore() {
        File dir =
                getContext()
                        .getEnvironment()
                        .getProperty(
                                "geoserver.backend.catalog-service.fallback-resource-directory",
                                File.class);
        if (dir == null) return null;
        dir.mkdirs();
        return new FileSystemResourceStore(dir);
    }
}
