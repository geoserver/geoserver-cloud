/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.gwc.integration;

import org.geoserver.catalog.Catalog;
import org.geoserver.cloud.autoconfigure.catalog.event.ConditionalOnCatalogEvents;
import org.geoserver.cloud.autoconfigure.gwc.ConditionalOnGeoWebCacheEnabled;
import org.geoserver.cloud.autoconfigure.gwc.core.GeoWebCacheAutoConfiguration;
import org.geoserver.cloud.event.catalog.CatalogInfoAdded;
import org.geoserver.cloud.gwc.catalog.CatalogTileLayerProjector;
import org.geoserver.gwc.GWC;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/** Configures GWC-owned projection of GeoServer catalog events into tile-layer state. */
@AutoConfiguration(after = GeoWebCacheAutoConfiguration.class)
@ConditionalOnGeoWebCacheEnabled
@ConditionalOnCatalogEvents
@ConditionalOnClass({CatalogInfoAdded.class, GWC.class})
@ConditionalOnBean({Catalog.class, GWC.class})
public class CatalogEventTileLayerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    CatalogTileLayerProjector catalogTileLayerProjector(Catalog catalog, GWC gwc) {
        return new CatalogTileLayerProjector(catalog, gwc);
    }
}
