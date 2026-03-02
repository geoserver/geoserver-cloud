/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.jackson.databind.catalog.mapper;

import lombok.Generated;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMTSLayerInfo;
import org.geoserver.jackson.databind.catalog.dto.CoverageInfoDto;
import org.geoserver.jackson.databind.catalog.dto.FeatureTypeInfoDto;
import org.geoserver.jackson.databind.catalog.dto.ResourceInfoDto;
import org.geoserver.jackson.databind.catalog.dto.WMSLayerInfoDto;
import org.geoserver.jackson.databind.catalog.dto.WMTSLayerInfoDto;
import org.mapstruct.AnnotateWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = CatalogInfoMapperConfig.class)
@AnnotateWith(value = Generated.class)
public interface ResourceMapper {

    default ResourceInfoDto map(ResourceInfo o) {
        if (o == null) {
            return null;
        } else if (o instanceof FeatureTypeInfo ft) {
            return map(ft);
        } else if (o instanceof CoverageInfo cov) {
            return map(cov);
        } else if (o instanceof WMSLayerInfo wms) {
            return map(wms);
        } else if (o instanceof WMTSLayerInfo wmts) {
            return map(wmts);
        }
        throw new IllegalArgumentException("Unknown ResourceInfo type: %s".formatted(o));
    }

    default ResourceInfo map(ResourceInfoDto o) {
        if (o == null) {
            return null;
        } else if (o instanceof FeatureTypeInfoDto ft) {
            return map(ft);
        } else if (o instanceof CoverageInfoDto cov) {
            return map(cov);
        } else if (o instanceof WMSLayerInfoDto wms) {
            return map(wms);
        } else if (o instanceof WMTSLayerInfoDto wmts) {
            return map(wmts);
        }
        throw new IllegalArgumentException("Unknown Resource type: %s".formatted(o));
    }

    @Mapping(target = "catalog", ignore = true)
    @Mapping(target = "featureType", ignore = true)
    @Mapping(source = "store", target = "store", qualifiedByName = "dataStoreInfo")
    FeatureTypeInfo map(FeatureTypeInfoDto o);

    FeatureTypeInfoDto map(FeatureTypeInfo o);

    @Mapping(target = "catalog", ignore = true)
    @Mapping(source = "store", target = "store", qualifiedByName = "coverageStoreInfo")
    CoverageInfo map(CoverageInfoDto o);

    CoverageInfoDto map(CoverageInfo o);

    @Mapping(target = "catalog", ignore = true)
    @Mapping(target = "remoteStyleInfos", ignore = true)
    @Mapping(target = "styles", ignore = true)
    @Mapping(target = "allAvailableRemoteStyles", ignore = true)
    @Mapping(source = "store", target = "store", qualifiedByName = "wmsStoreInfo")
    WMSLayerInfo map(WMSLayerInfoDto o);

    WMSLayerInfoDto map(WMSLayerInfo o);

    @Mapping(target = "catalog", ignore = true)
    @Mapping(source = "store", target = "store", qualifiedByName = "wmtsStoreInfo")
    WMTSLayerInfo map(WMTSLayerInfoDto o);

    WMTSLayerInfoDto map(WMTSLayerInfo o);
}
