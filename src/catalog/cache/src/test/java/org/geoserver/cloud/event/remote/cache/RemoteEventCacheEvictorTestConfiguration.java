/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.event.remote.cache;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.plugin.CatalogPlugin;
import org.geoserver.catalog.plugin.DefaultMemoryCatalogFacade;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerFacade;
import org.geoserver.config.plugin.GeoServerImpl;
import org.geoserver.config.plugin.RepositoryGeoServerFacadeImpl;
import org.geoserver.platform.config.DefaultUpdateSequence;
import org.geoserver.platform.config.UpdateSequence;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.freemarker.FreeMarkerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = true)
@EnableAutoConfiguration(
        exclude = {
            SecurityAutoConfiguration.class,
            UserDetailsServiceAutoConfiguration.class,
            DataSourceAutoConfiguration.class,
            FreeMarkerAutoConfiguration.class
        })
public class RemoteEventCacheEvictorTestConfiguration {

    @Bean
    UpdateSequence defaultUpdateSequence(GeoServer geoserver) {
        return new DefaultUpdateSequence(geoserver);
    }

    @Bean(name = {"rawCatalog"})
    Catalog rawCatalog(@Qualifier("catalogFacade") CatalogFacade facade) {
        return new CatalogPlugin(facade);
    }

    @Bean(name = {"catalog"})
    Catalog catalog(@Qualifier("rawCatalog") Catalog raw) {
        return raw;
    }

    @Bean(name = "geoServer")
    GeoServer geoServer(
            @Qualifier("catalog") Catalog catalog,
            @Qualifier("geoserverFacade") GeoServerFacade facade) {

        GeoServerImpl geoServerImpl = new GeoServerImpl(facade);
        geoServerImpl.setCatalog(catalog);
        return geoServerImpl;
    }

    @Bean(name = "catalogFacade")
    CatalogFacade catalogFacade() {
        return new DefaultMemoryCatalogFacade();
    }

    @Bean(name = "geoserverFacade")
    GeoServerFacade geoserverFacade() {
        return new RepositoryGeoServerFacadeImpl();
    }
}
