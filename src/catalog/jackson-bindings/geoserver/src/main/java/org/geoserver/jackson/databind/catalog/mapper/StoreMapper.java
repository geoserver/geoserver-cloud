/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
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
import org.geoserver.jackson.databind.catalog.dto.CoverageStore;
import org.geoserver.jackson.databind.catalog.dto.DataStore;
import org.geoserver.jackson.databind.catalog.dto.Store;
import org.geoserver.jackson.databind.catalog.dto.WMSStore;
import org.geoserver.jackson.databind.catalog.dto.WMTSStore;
import org.mapstruct.AnnotateWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = CatalogInfoMapperConfig.class)
@AnnotateWith(value = Generated.class)
public interface StoreMapper {
    default Store map(StoreInfo o) {
        if (o == null) return null;
        if (o instanceof DataStoreInfo ds) return map(ds);
        if (o instanceof CoverageStoreInfo cs) return map(cs);
        if (o instanceof WMSStoreInfo wms) return map(wms);
        if (o instanceof WMTSStoreInfo wmts) return map(wmts);

        throw new IllegalArgumentException("Unknown StoreInfo type: " + o);
    }

    default StoreInfo map(Store o) {
        if (o == null) return null;
        if (o instanceof DataStore ds) return map(ds);
        if (o instanceof CoverageStore cs) return map(cs);
        if (o instanceof WMSStore wms) return map(wms);
        if (o instanceof WMTSStore wmts) return map(wmts);

        throw new IllegalArgumentException("Unknown Store type: " + o);
    }

    /**
     * Convert ConnectionParameters to a standard Serializable map for StoreInfo.
     */
    default Map<String, Serializable> connectionParamsFromDto(ConnectionParameters params) {
        if (params == null) return new LinkedHashMap<>();
        return params.toSerializableMap();
    }

    /**
     * Convert a standard Serializable map to ConnectionParameters for Store DTO.
     */
    default ConnectionParameters connectionParamsToDto(Map<String, Serializable> params) {
        if (params == null) return new ConnectionParameters();
        return new ConnectionParameters(params);
    }

    @Mapping(target = "error", ignore = true)
    @Mapping(target = "catalog", ignore = true)
    @Mapping(target = "connectionParameters", expression = "java(connectionParamsFromDto(o.getConnectionParameters()))")
    DataStoreInfo map(DataStore o);

    @Mapping(target = "connectionParameters", expression = "java(connectionParamsToDto(o.getConnectionParameters()))")
    DataStore map(DataStoreInfo o);

    @Mapping(target = "error", ignore = true)
    @Mapping(target = "catalog", ignore = true)
    @Mapping(target = "connectionParameters", expression = "java(connectionParamsFromDto(o.getConnectionParameters()))")
    CoverageStoreInfo map(CoverageStore o);

    @Mapping(target = "connectionParameters", expression = "java(connectionParamsToDto(o.getConnectionParameters()))")
    CoverageStore map(CoverageStoreInfo o);

    @Mapping(target = "error", ignore = true)
    @Mapping(target = "catalog", ignore = true)
    @Mapping(target = "connectionParameters", expression = "java(connectionParamsFromDto(o.getConnectionParameters()))")
    WMSStoreInfo map(WMSStore o);

    @Mapping(target = "connectionParameters", expression = "java(connectionParamsToDto(o.getConnectionParameters()))")
    WMSStore map(WMSStoreInfo o);

    @Mapping(target = "error", ignore = true)
    @Mapping(target = "catalog", ignore = true)
    @Mapping(target = "connectionParameters", expression = "java(connectionParamsFromDto(o.getConnectionParameters()))")
    WMTSStoreInfo map(WMTSStore o);

    @Mapping(target = "connectionParameters", expression = "java(connectionParamsToDto(o.getConnectionParameters()))")
    WMTSStore map(WMTSStoreInfo o);
}
