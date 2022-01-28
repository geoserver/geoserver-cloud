/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.gwc;

import org.geoserver.cloud.autoconfigure.bus.ConditionalOnGeoServerRemoteEventsEnabled;
import org.geoserver.cloud.gwc.bus.RemoteTileLayerEvent;
import org.geoserver.cloud.gwc.bus.TileLayerRemoteEventBroadcaster;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.cloud.bus.BusAutoConfiguration;
import org.springframework.cloud.bus.jackson.RemoteApplicationEventScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** @since 1.0 */
@Configuration(proxyBeanMethods = false)
@AutoConfigureAfter(BusAutoConfiguration.class)
@ConditionalOnGeoServerRemoteEventsEnabled
@RemoteApplicationEventScan(basePackageClasses = {RemoteTileLayerEvent.class})
public class GwcEventBusAutoConfiguration {

    public @Bean TileLayerRemoteEventBroadcaster tileLayerRemoteEventBroadcaster() {
        return new TileLayerRemoteEventBroadcaster();
    }
}
