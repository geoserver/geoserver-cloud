/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.jackson.databind.catalog.mapper;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Generated;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WMTSStoreInfo;
import org.geoserver.jackson.databind.catalog.ConnectionParameters;
import org.geoserver.jackson.databind.catalog.dto.CoverageStoreInfoDto;
import org.geoserver.jackson.databind.catalog.dto.DataStoreInfoDto;
import org.geoserver.jackson.databind.catalog.dto.StoreInfoDto;
import org.geoserver.jackson.databind.catalog.dto.WMSStoreInfoDto;
import org.geoserver.jackson.databind.catalog.dto.WMTSStoreInfoDto;
import org.mapstruct.AnnotateWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = CatalogInfoMapperConfig.class)
@AnnotateWith(value = Generated.class)
public interface StoreMapper {
    default StoreInfoDto map(StoreInfo o) {
        if (o == null) {
            return null;
        } else if (o instanceof DataStoreInfo ds) {
            return map(ds);
        } else if (o instanceof CoverageStoreInfo cs) {
            return map(cs);
        } else if (o instanceof WMSStoreInfo wms) {
            return map(wms);
        } else if (o instanceof WMTSStoreInfo wmts) {
            return map(wmts);
        }

        throw new IllegalArgumentException("Unknown StoreInfo type: " + o);
    }

    default StoreInfo map(StoreInfoDto o) {
        if (o == null) {
            return null;
        } else if (o instanceof DataStoreInfoDto ds) {
            return map(ds);
        } else if (o instanceof CoverageStoreInfoDto cs) {
            return map(cs);
        } else if (o instanceof WMSStoreInfoDto wms) {
            return map(wms);
        } else if (o instanceof WMTSStoreInfoDto wmts) {
            return map(wmts);
        }

        throw new IllegalArgumentException("Unknown Store type: " + o);
    }

    /**
     * Convert ConnectionParameters to a standard Serializable map for StoreInfo.
     */
    default Map<String, Serializable> connectionParamsFromDto(ConnectionParameters params) {
        return (params == null) ? new LinkedHashMap<>() : params.toSerializableMap();
    }

    /**
     * Convert a standard Serializable map to ConnectionParameters for Store DTO.
     */
    default ConnectionParameters connectionParamsToDto(Map<String, Serializable> params) {
        return (params == null) ? new ConnectionParameters() : new ConnectionParameters(params);
    }

    @Mapping(target = "error", ignore = true)
    @Mapping(target = "catalog", ignore = true)
    @Mapping(target = "connectionParameters", expression = "java(connectionParamsFromDto(o.getConnectionParameters()))")
    DataStoreInfo map(DataStoreInfoDto o);

    @Mapping(target = "connectionParameters", expression = "java(connectionParamsToDto(o.getConnectionParameters()))")
    DataStoreInfoDto map(DataStoreInfo o);

    @Mapping(target = "error", ignore = true)
    @Mapping(target = "catalog", ignore = true)
    @Mapping(target = "connectionParameters", expression = "java(connectionParamsFromDto(o.getConnectionParameters()))")
    CoverageStoreInfo map(CoverageStoreInfoDto o);

    @Mapping(target = "connectionParameters", expression = "java(connectionParamsToDto(o.getConnectionParameters()))")
    CoverageStoreInfoDto map(CoverageStoreInfo o);

    @Mapping(target = "error", ignore = true)
    @Mapping(target = "catalog", ignore = true)
    @Mapping(target = "connectionParameters", expression = "java(connectionParamsFromDto(o.getConnectionParameters()))")
    WMSStoreInfo map(WMSStoreInfoDto o);

    @Mapping(target = "connectionParameters", expression = "java(connectionParamsToDto(o.getConnectionParameters()))")
    WMSStoreInfoDto map(WMSStoreInfo o);

    @Mapping(target = "error", ignore = true)
    @Mapping(target = "catalog", ignore = true)
    @Mapping(target = "connectionParameters", expression = "java(connectionParamsFromDto(o.getConnectionParameters()))")
    WMTSStoreInfo map(WMTSStoreInfoDto o);

    @Mapping(target = "connectionParameters", expression = "java(connectionParamsToDto(o.getConnectionParameters()))")
    WMTSStoreInfoDto map(WMTSStoreInfo o);
}
