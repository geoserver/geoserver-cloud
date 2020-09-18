/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.repository;

import org.geoserver.catalog.MapInfo;
import org.geoserver.catalog.plugin.CatalogInfoRepository.MapRepository;
import org.geoserver.cloud.catalog.client.reactivefeign.ReactiveCatalogClient;
import lombok.Getter;
import lombok.NonNull;

public class CloudMapRepository extends CatalogServiceClientRepository<MapInfo>
        implements MapRepository {

    private final @Getter Class<MapInfo> infoType = MapInfo.class;

    protected CloudMapRepository(@NonNull ReactiveCatalogClient client) {
        super(client);
    }
}
