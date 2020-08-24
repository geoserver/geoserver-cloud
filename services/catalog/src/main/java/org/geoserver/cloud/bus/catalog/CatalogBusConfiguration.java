/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.bus.catalog;

import javax.annotation.PostConstruct;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.catalog.Catalog;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.bus.BusProperties;
import org.springframework.cloud.bus.ConditionalOnBusEnabled;
import org.springframework.cloud.bus.jackson.RemoteApplicationEventScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnBusEnabled
@RemoteApplicationEventScan(basePackageClasses = CatalogRemoteEvent.class)
@Slf4j
public class CatalogBusConfiguration {

    public CatalogBusConfiguration() {}

    private @PostConstruct void logInit() {
        log.info("Configuring GeoServer Catalog distributed events");
    }

    public @Bean CatalogRemoteEventBroadcaster catalogRemoteEventBroadcaster(
            @NonNull BusProperties busProperties, @Qualifier("rawCatalog") Catalog rawCatalog) {
        CatalogRemoteEventBroadcaster broadcaster = new CatalogRemoteEventBroadcaster();
        rawCatalog.addListener(broadcaster);
        return broadcaster;
    }

    public @Bean ResourcePoolRemoteEventProcessor catalogRemoteEventProcessor(
            @Qualifier("rawCatalog") Catalog rawCatalog) {
        return new ResourcePoolRemoteEventProcessor(rawCatalog);
    }
}
