/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.repository;

import lombok.Getter;
import lombok.NonNull;
import org.geoserver.catalog.MapInfo;
import org.geoserver.catalog.plugin.CatalogInfoRepository.MapRepository;
import org.geoserver.cloud.catalog.client.feign.MapClient;

public class CloudMapRepository extends CatalogServiceClientRepository<MapInfo, MapClient>
        implements MapRepository {

    private final @Getter Class<MapInfo> infoType = MapInfo.class;

    protected CloudMapRepository(@NonNull MapClient client) {
        super(client);
    }
}
