/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.jackson.databind.catalog.mapper;

import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMTSLayerInfo;
import org.geoserver.jackson.databind.catalog.dto.Coverage;
import org.geoserver.jackson.databind.catalog.dto.FeatureType;
import org.geoserver.jackson.databind.catalog.dto.Resource;
import org.geoserver.jackson.databind.catalog.dto.WMSLayer;
import org.geoserver.jackson.databind.catalog.dto.WMTSLayer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = CatalogInfoMapperConfig.class)
public interface ResourceMapper {

    default Resource map(ResourceInfo o) {
        if (o == null) return null;
        if (o instanceof FeatureTypeInfo) return map((FeatureTypeInfo) o);
        if (o instanceof CoverageInfo) return map((CoverageInfo) o);
        if (o instanceof WMSLayerInfo) return map((WMSLayerInfo) o);
        if (o instanceof WMTSLayerInfo) return map((WMTSLayerInfo) o);
        throw new IllegalArgumentException("Unknown ResourceInfo type: " + o);
    }

    default ResourceInfo map(Resource o) {
        if (o == null) return null;
        if (o instanceof FeatureType) return map((FeatureType) o);
        if (o instanceof Coverage) return map((Coverage) o);
        if (o instanceof WMSLayer) return map((WMSLayer) o);
        if (o instanceof WMTSLayer) return map((WMTSLayer) o);
        throw new IllegalArgumentException("Unknown Resource type: " + o);
    }

    @Mapping(target = "catalog", ignore = true)
    @Mapping(target = "featureType", ignore = true)
    FeatureTypeInfo map(FeatureType o);

    FeatureType map(FeatureTypeInfo o);

    @Mapping(target = "catalog", ignore = true)
    CoverageInfo map(Coverage o);

    Coverage map(CoverageInfo o);

    @Mapping(target = "catalog", ignore = true)
    @Mapping(target = "remoteStyleInfos", ignore = true)
    @Mapping(target = "styles", ignore = true)
    @Mapping(target = "allAvailableRemoteStyles", ignore = true)
    WMSLayerInfo map(WMSLayer o);

    WMSLayer map(WMSLayerInfo o);

    @Mapping(target = "catalog", ignore = true)
    WMTSLayerInfo map(WMTSLayer o);

    WMTSLayer map(WMTSLayerInfo o);
}
