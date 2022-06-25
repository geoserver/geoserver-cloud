/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.event.bus;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.plugin.CatalogPlugin;
import org.geoserver.config.GeoServer;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.platform.config.DefaultUpdateSequence;
import org.geoserver.platform.config.UpdateSequence;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;

@EnableAutoConfiguration
@SpringBootConfiguration
public class TestConfigurationAutoConfiguration {

    public @Bean UpdateSequence testUpdateSequence() {
        return new DefaultUpdateSequence();
    }

    public @Bean XStreamPersisterFactory xStreamPersisterFactory() {
        return new XStreamPersisterFactory();
    }

    @Bean(name = {"catalog", "rawCatalog"})
    public Catalog catalog() {
        return new CatalogPlugin(false);
    }

    public @Bean GeoServer geoServer() {
        return new org.geoserver.config.plugin.GeoServerImpl();
    }
}
