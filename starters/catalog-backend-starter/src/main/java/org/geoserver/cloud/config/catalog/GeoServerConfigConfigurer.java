/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.catalog;

import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.config.GeoServerFacade;
import org.geoserver.config.plugin.GeoServerImpl;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.ResourceStore;
import org.geoserver.platform.resource.ResourceStoreFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

/** Configurer interface for the {@link GeoServer} config subsystem. */
public interface GeoServerConfigConfigurer {

    ApplicationContext getContext();

    default @Bean(name = "geoServer") GeoServerImpl geoServer(@Qualifier("catalog") Catalog catalog)
            throws Exception {
        GeoServerFacade facade = geoserverFacade();
        GeoServerImpl gs = new GeoServerImpl(facade);
        gs.setCatalog(catalog);
        return gs;
    }

    public @Bean GeoServerFacade geoserverFacade();

    /**
     * {@link ResourceStore} named {@code resourceStoreImpl}, as looked up in the application
     * context by {@link ResourceStoreFactory}. With this, we don't need a bean called
     * "dataDirectoryResourceStore" at all.
     */
    @Bean
    ResourceStore resourceStoreImpl();

    @Bean
    GeoServerResourceLoader resourceLoader();

    // <bean id="dataDirectory" class="org.geoserver.config.GeoServerDataDirectory">
    // <constructor-arg ref="resourceLoader"/>
    // </bean>
    default @Bean GeoServerDataDirectory dataDirectory() {
        return new GeoServerDataDirectory(resourceLoader());
    }
}
