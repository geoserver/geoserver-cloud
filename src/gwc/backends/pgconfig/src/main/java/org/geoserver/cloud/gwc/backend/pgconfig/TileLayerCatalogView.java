/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.gwc.backend.pgconfig;

import lombok.NonNull;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.impl.AbstractCatalogDecorator;

/**
 * The catalog given to a tile layer built from a layer or group already read: the catalog itself, except that a lookup
 * of that layer or group by id returns the object already read.
 *
 * <p>GeoServer's tile layer takes either its layer or group, and then has no catalog, or the catalog, and then looks up
 * its layer or group by id. It needs the catalog: the capabilities of a workspace use it to remove the workspace prefix
 * from the layer names, and the capabilities of vector tile formats read the feature types through its resource pool.
 * This view gives it the catalog without a lookup per tile layer, and the tile layer keeps its own copy of the layer or
 * group.
 */
class TileLayerCatalogView extends AbstractCatalogDecorator {

    private final PublishedInfo published;

    TileLayerCatalogView(@NonNull Catalog catalog, @NonNull PublishedInfo published) {
        super(catalog);
        this.published = published;
    }

    @Override
    public LayerInfo getLayer(String id) {
        if (published instanceof LayerInfo layer && layer.getId().equals(id)) {
            return layer;
        }
        return super.getLayer(id);
    }

    @Override
    public LayerGroupInfo getLayerGroup(String id) {
        if (published instanceof LayerGroupInfo group && group.getId().equals(id)) {
            return group;
        }
        return super.getLayerGroup(id);
    }

    // the decorator compares the catalogs: two views of one catalog differ by the layer or group they hold
    @Override
    public boolean equals(Object other) {
        return other instanceof TileLayerCatalogView view && super.equals(view) && published.equals(view.published);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + published.hashCode();
    }
}
