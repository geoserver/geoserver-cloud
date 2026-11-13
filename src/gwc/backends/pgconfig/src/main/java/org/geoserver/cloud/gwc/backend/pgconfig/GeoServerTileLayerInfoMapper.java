/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.gwc.backend.pgconfig;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;
import lombok.Generated;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.cloud.gwc.backend.pgconfig.TileLayerInfo.Bounds;
import org.geoserver.cloud.gwc.backend.pgconfig.TileLayerInfo.GridSubset;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.gwc.layer.GeoServerTileLayerInfo;
import org.geoserver.gwc.layer.GeoServerTileLayerInfoImpl;
import org.geowebcache.config.XMLGridSubset;
import org.geowebcache.grid.BoundingBox;
import org.mapstruct.AnnotateWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

/** @since 1.7 */
@Mapper(uses = ParameterFilterMapper.class, unmappedTargetPolicy = ReportingPolicy.ERROR)
@AnnotateWith(value = Generated.class)
interface GeoServerTileLayerInfoMapper {

    default TileLayerInfo map(GeoServerTileLayer tileLayer) {
        checkNotNull(tileLayer.getInfo(), "GeoServerTileLayerInfo is null");
        checkNotNull(tileLayer.getInfo().getId(), "GeoServerTileLayerInfo.info.id is null");
        final GeoServerTileLayerInfo info = tileLayer.getInfo();
        return map(info);
    }

    /**
     * Builds the info of a tile layer from its GWC settings and its layer or group. The layer or group gives the id and
     * the prefixed name. Each call returns a new instance, with new grid subsets, parameter filters and expiration
     * rules: the caller may change it without changing the settings.
     */
    default GeoServerTileLayerInfoImpl toInfo(TileLayerInfo settings, PublishedInfo published) {
        Objects.requireNonNull(published, "publishedInfo");
        GeoServerTileLayerInfoImpl info = map(settings);
        info.setId(published.getId());
        info.setName(published.prefixedName());
        return info;
    }

    /** Maps the GWC settings alone, without id and name: {@link #toInfo} adds them. */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "name", ignore = true)
    @Mapping(target = "autoCacheStyles", ignore = true)
    // watch out GeoServerTileLayerInfoImpl.setMetaTilingX/Y asserts the arg is > 0
    @Mapping(target = "metaTilingX", conditionExpression = "java(info.getMetaTilingX() > 0)")
    @Mapping(target = "metaTilingY", conditionExpression = "java(info.getMetaTilingY() > 0)")
    GeoServerTileLayerInfoImpl map(TileLayerInfo info);

    TileLayerInfo map(GeoServerTileLayerInfo info);

    default BoundingBox map(Bounds bbox) {
        if (bbox == null) {
            return null;
        }
        return new BoundingBox(bbox.getMinX(), bbox.getMinY(), bbox.getMaxX(), bbox.getMaxY());
    }

    Bounds map(BoundingBox bbox);

    XMLGridSubset map(GridSubset gridSubset);

    GridSubset map(XMLGridSubset gridSubset);
}
