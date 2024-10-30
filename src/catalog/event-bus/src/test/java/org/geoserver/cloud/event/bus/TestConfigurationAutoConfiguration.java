/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.event.bus;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.plugin.CatalogPlugin;
import org.geoserver.config.DefaultGeoServerLoader;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerLoader;
import org.geoserver.config.plugin.GeoServerImpl;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.config.DefaultUpdateSequence;
import org.geoserver.platform.config.UpdateSequence;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;

@EnableAutoConfiguration
@SpringBootConfiguration
public class TestConfigurationAutoConfiguration {

    @Bean
    UpdateSequence testUpdateSequence(GeoServer gs) {
        return new DefaultUpdateSequence(gs);
    }

    @Bean
    XStreamPersisterFactory xStreamPersisterFactory() {
        return new XStreamPersisterFactory();
    }

    @Bean(name = {"catalog", "rawCatalog"})
    public Catalog catalog() {
        return new CatalogPlugin(false);
    }

    @Bean
    GeoServer geoServer(@Qualifier("catalog") Catalog catalog) {
        GeoServerImpl gs = new org.geoserver.config.plugin.GeoServerImpl();
        gs.setCatalog(catalog);
        return gs;
    }

    @Bean
    GeoServerResourceLoader geoServerResourceLoader() {
        return new GeoServerResourceLoader();
    }

    @Bean
    GeoServerLoader geoserverLoader(
            @Qualifier("geoServer") GeoServer geoServer,
            @Qualifier("geoServerResourceLoader") GeoServerResourceLoader geoServerResourceLoader) {
        DefaultGeoServerLoader loader = new DefaultGeoServerLoader(geoServerResourceLoader);
        loader.postProcessBeforeInitialization(geoServer, "geoserver");
        return loader;
    }
}
