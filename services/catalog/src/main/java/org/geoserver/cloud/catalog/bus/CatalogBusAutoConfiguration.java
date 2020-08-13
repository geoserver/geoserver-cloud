package org.geoserver.cloud.catalog.bus;

import javax.annotation.PostConstruct;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.catalog.Catalog;
import org.geoserver.cloud.catalog.bus.events.CatalogRemoteEvent;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.cloud.bus.BusAutoConfiguration;
import org.springframework.cloud.bus.BusProperties;
import org.springframework.cloud.bus.ConditionalOnBusEnabled;
import org.springframework.cloud.bus.jackson.RemoteApplicationEventScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnBusEnabled
@AutoConfigureAfter(BusAutoConfiguration.class)
@RemoteApplicationEventScan(basePackageClasses = CatalogRemoteEvent.class)
@Slf4j
public class CatalogBusAutoConfiguration {

    public CatalogBusAutoConfiguration() {}

    private @PostConstruct void logInit() {
        log.info("Configuring GeoServer Catalog distributed events");
    }

    public @Bean CatalogRemoteEventBroadcaster catalogRemoteEventBroadcaster(
            @NonNull BusProperties busProperties, @Qualifier("rawCatalog") Catalog rawCatalog) {
        CatalogRemoteEventBroadcaster broadcaster = new CatalogRemoteEventBroadcaster();
        rawCatalog.addListener(broadcaster);
        return broadcaster;
    }

    public @Bean CatalogRemoteEventProcessor catalogRemoteEventProcessor(
            @Qualifier("rawCatalog") Catalog rawCatalog) {
        return new CatalogRemoteEventProcessor(rawCatalog);
    }
}
