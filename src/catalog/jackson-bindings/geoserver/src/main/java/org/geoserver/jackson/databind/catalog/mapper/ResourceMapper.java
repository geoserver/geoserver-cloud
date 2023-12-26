/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.jackson.databind.catalog.mapper;

import lombok.Generated;

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
import org.mapstruct.AnnotateWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = CatalogInfoMapperConfig.class)
@AnnotateWith(value = Generated.class)
public interface ResourceMapper {

    default Resource map(ResourceInfo o) {
        if (o == null) return null;
        if (o instanceof FeatureTypeInfo ft) return map(ft);
        if (o instanceof CoverageInfo cov) return map(cov);
        if (o instanceof WMSLayerInfo wms) return map(wms);
        if (o instanceof WMTSLayerInfo wmts) return map(wmts);
        throw new IllegalArgumentException("Unknown ResourceInfo type: %s".formatted(o));
    }

    default ResourceInfo map(Resource o) {
        if (o == null) return null;
        if (o instanceof FeatureType ft) return map(ft);
        if (o instanceof Coverage cov) return map(cov);
        if (o instanceof WMSLayer wms) return map(wms);
        if (o instanceof WMTSLayer wmts) return map(wmts);
        throw new IllegalArgumentException("Unknown Resource type: %s".formatted(o));
    }

    @Mapping(target = "catalog", ignore = true)
    @Mapping(target = "featureType", ignore = true)
    @Mapping(source = "store", target = "store", qualifiedByName = "dataStoreInfo")
    FeatureTypeInfo map(FeatureType o);

    FeatureType map(FeatureTypeInfo o);

    @Mapping(target = "catalog", ignore = true)
    @Mapping(source = "store", target = "store", qualifiedByName = "coverageStoreInfo")
    CoverageInfo map(Coverage o);

    Coverage map(CoverageInfo o);

    @Mapping(target = "catalog", ignore = true)
    @Mapping(target = "remoteStyleInfos", ignore = true)
    @Mapping(target = "styles", ignore = true)
    @Mapping(target = "allAvailableRemoteStyles", ignore = true)
    @Mapping(source = "store", target = "store", qualifiedByName = "wmsStoreInfo")
    WMSLayerInfo map(WMSLayer o);

    WMSLayer map(WMSLayerInfo o);

    @Mapping(target = "catalog", ignore = true)
    @Mapping(source = "store", target = "store", qualifiedByName = "wmtsStoreInfo")
    WMTSLayerInfo map(WMTSLayer o);

    WMTSLayer map(WMTSLayerInfo o);
}
