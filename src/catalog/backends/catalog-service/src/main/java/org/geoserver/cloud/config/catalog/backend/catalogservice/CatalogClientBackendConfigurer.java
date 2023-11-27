/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.catalog.backend.catalogservice;

import lombok.extern.slf4j.Slf4j;

import org.geoserver.GeoServerConfigurationLock;
import org.geoserver.catalog.plugin.ExtendedCatalogFacade;
import org.geoserver.cloud.catalog.client.impl.CatalogClientCatalogFacade;
import org.geoserver.cloud.catalog.client.impl.CatalogClientConfiguration;
import org.geoserver.cloud.catalog.client.impl.CatalogClientGeoServerFacade;
import org.geoserver.cloud.catalog.client.impl.CatalogClientResourceStore;
import org.geoserver.cloud.config.catalog.backend.core.GeoServerBackendConfigurer;
import org.geoserver.config.GeoServerFacade;
import org.geoserver.config.GeoServerLoader;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.config.UpdateSequence;
import org.geoserver.platform.resource.FileSystemResourceStore;
import org.geoserver.platform.resource.ResourceStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

import java.io.File;

@Configuration(proxyBeanMethods = true)
@EnableConfigurationProperties(CatalogClientProperties.class)
@Import(CatalogClientConfiguration.class)
@Slf4j(topic = "org.geoserver.cloud.config.catalogclient")
public class CatalogClientBackendConfigurer extends GeoServerBackendConfigurer {

    private @Autowired CatalogClientCatalogFacade catalogClientFacade;
    private @Autowired CatalogClientGeoServerFacade configClientFacade;
    private @Autowired CatalogClientResourceStore catalogServiceResourceStore;

    private @Autowired CatalogClientProperties catalogClientConfig;

    public CatalogClientBackendConfigurer() {
        log.info(
                "Loading geoserver config backend with {}",
                CatalogClientBackendConfigurer.class.getSimpleName());
    }

    protected @Bean @Override UpdateSequence updateSequence() {
        throw new UnsupportedOperationException("implement");
    }

    protected @Bean @Override GeoServerConfigurationLock configurationLock() {
        throw new UnsupportedOperationException("implement");
    }

    protected @Bean @Override ExtendedCatalogFacade catalogFacade() {
        return catalogClientFacade;
    }

    protected @Bean @Override GeoServerFacade geoserverFacade() {
        return configClientFacade;
    }

    @Bean(name = {"resourceStoreImpl"})
    protected @Override CatalogClientResourceStore resourceStoreImpl() {
        CatalogClientResourceStore store = catalogServiceResourceStore;
        File cacheDirectory = catalogClientConfig.getCacheDirectory();
        if (null != cacheDirectory) {
            store.setLocalCacheDirectory(cacheDirectory);
        }
        return store;
    }

    @DependsOn({
        "extensions",
        "wmsLoader",
        "wfsLoader",
        "wcsLoader",
        "wpsServiceLoader",
        "wmtsLoader"
    })
    protected @Bean @Override GeoServerLoader geoServerLoaderImpl() {
        return new CatalogClientGeoServerLoader(resourceLoader());
    }

    protected @Bean @Override GeoServerResourceLoader resourceLoader() {
        CatalogClientResourceStore resourceStore = resourceStoreImpl();
        GeoServerResourceLoader resourceLoader = new GeoServerResourceLoader(resourceStore);
        File cacheDirectory = catalogClientConfig.getCacheDirectory();
        if (null != cacheDirectory) {
            resourceLoader.setBaseDirectory(cacheDirectory);
        }
        return resourceLoader;
    }

    @Bean
    ResourceStore catalogServiceFallbackResourceStore(@Autowired Environment springEnv) {
        File dir =
                springEnv.getProperty(
                        "geoserver.backend.catalog-service.fallback-resource-directory",
                        File.class);
        if (dir == null) return null;
        dir.mkdirs();
        return new FileSystemResourceStore(dir);
    }
}
