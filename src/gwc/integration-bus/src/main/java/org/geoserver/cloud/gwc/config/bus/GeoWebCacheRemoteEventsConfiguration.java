/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.gwc.config.bus;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import org.geoserver.cloud.gwc.bus.GeoWebCacheRemoteEventsBroker;
import org.geoserver.cloud.gwc.bus.RemoteGeoWebCacheEvent;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.cloud.bus.BusAutoConfiguration;
import org.springframework.cloud.bus.ServiceMatcher;
import org.springframework.cloud.bus.event.RemoteApplicationEvent;
import org.springframework.cloud.bus.jackson.RemoteApplicationEventScan;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @since 1.0
 */
@Configuration(proxyBeanMethods = false)
@AutoConfigureAfter(BusAutoConfiguration.class)
@RemoteApplicationEventScan(basePackageClasses = {RemoteGeoWebCacheEvent.class})
public class GeoWebCacheRemoteEventsConfiguration {

    @Bean
    GeoWebCacheRemoteEventsBroker tileLayerRemoteEventBroadcaster( //
            ApplicationEventPublisher eventPublisher, ServiceMatcher busServiceMatcher) {

        Supplier<String> originServiceId = busServiceMatcher::getBusId;
        Function<RemoteApplicationEvent, Boolean> selfServiceCheck = busServiceMatcher::isFromSelf;
        Consumer<Object> publisher = eventPublisher::publishEvent;
        return new GeoWebCacheRemoteEventsBroker(originServiceId, selfServiceCheck, publisher);
    }
}
