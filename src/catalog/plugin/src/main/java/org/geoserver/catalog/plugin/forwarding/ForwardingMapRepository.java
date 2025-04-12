/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.catalog.plugin.forwarding;

import org.geoserver.catalog.MapInfo;
import org.geoserver.catalog.plugin.CatalogInfoRepository.MapRepository;

/**
 * A decorator for {@link MapRepository} that forwards all method calls to an underlying repository.
 *
 * @since 1.0
 */
public class ForwardingMapRepository extends ForwardingCatalogRepository<MapInfo, MapRepository>
        implements MapRepository {

    public ForwardingMapRepository(MapRepository subject) {
        super(subject);
    }
}
