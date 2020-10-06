/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.repository;

import lombok.Getter;
import org.geoserver.catalog.MapInfo;
import org.geoserver.catalog.plugin.CatalogInfoRepository.MapRepository;

public class CatalogClientMapRepository extends CatalogClientRepository<MapInfo>
        implements MapRepository {

    private final @Getter Class<MapInfo> contentType = MapInfo.class;
}
