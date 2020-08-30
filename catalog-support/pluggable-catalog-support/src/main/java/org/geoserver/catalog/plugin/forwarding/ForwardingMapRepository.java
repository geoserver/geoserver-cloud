/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin.forwarding;

import org.geoserver.catalog.MapInfo;
import org.geoserver.catalog.plugin.CatalogInfoRepository.MapRepository;

public class ForwardingMapRepository extends ForwardingCatalogRepository<MapInfo>
        implements MapRepository {

    public ForwardingMapRepository(MapRepository subject) {
        super(subject);
    }
}
