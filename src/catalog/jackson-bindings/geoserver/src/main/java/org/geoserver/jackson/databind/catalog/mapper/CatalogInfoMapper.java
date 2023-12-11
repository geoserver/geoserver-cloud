/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.jackson.databind.catalog.mapper;

import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.MapInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.jackson.databind.catalog.dto.CatalogInfoDto;
import org.geoserver.jackson.databind.catalog.dto.Map;
import org.geoserver.jackson.databind.catalog.dto.Namespace;
import org.geoserver.jackson.databind.catalog.dto.Published;
import org.geoserver.jackson.databind.catalog.dto.Resource;
import org.geoserver.jackson.databind.catalog.dto.Store;
import org.geoserver.jackson.databind.catalog.dto.Style;
import org.geoserver.jackson.databind.catalog.dto.Workspace;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(config = CatalogInfoMapperConfig.class)
public interface CatalogInfoMapper {
    public static final MapMapper MAP_MAPPER = Mappers.getMapper(MapMapper.class);
    public static final StyleMapper STYLE_MAPPER = Mappers.getMapper(StyleMapper.class);
    public static final PublishedMapper PUBLISHED_MAPPER = Mappers.getMapper(PublishedMapper.class);
    public static final ResourceMapper RESOURCE_MAPPER = Mappers.getMapper(ResourceMapper.class);
    public static final StoreMapper STORE_MAPPER = Mappers.getMapper(StoreMapper.class);
    public static final NamespaceMapper NAMESPACE_MAPPER = Mappers.getMapper(NamespaceMapper.class);
    public static final WorkspaceMapper WORKSPACE_MAPPER = Mappers.getMapper(WorkspaceMapper.class);

    @SuppressWarnings("unchecked")
    default <I extends CatalogInfo> I map(CatalogInfoDto dto) {
        if (dto == null) return null;
        if (dto instanceof Workspace ws) return (I) WORKSPACE_MAPPER.map(ws);
        if (dto instanceof Namespace ns) return (I) NAMESPACE_MAPPER.map(ns);
        if (dto instanceof Store store) return (I) STORE_MAPPER.map(store);
        if (dto instanceof Resource res) return (I) RESOURCE_MAPPER.map(res);
        if (dto instanceof Published published) return (I) PUBLISHED_MAPPER.map(published);
        if (dto instanceof Style style) return (I) STYLE_MAPPER.map(style);
        if (dto instanceof Map map) return (I) MAP_MAPPER.map(map);

        throw new IllegalArgumentException(
                "Unknown CatalogInfoDto type: %s".formatted(dto.getClass().getCanonicalName()));
    }

    default CatalogInfoDto map(CatalogInfo info) {
        if (info == null) return null;
        if (info instanceof WorkspaceInfo ws) return WORKSPACE_MAPPER.map(ws);
        if (info instanceof NamespaceInfo ns) return NAMESPACE_MAPPER.map(ns);
        if (info instanceof StoreInfo store) return STORE_MAPPER.map(store);
        if (info instanceof ResourceInfo res) return RESOURCE_MAPPER.map(res);
        if (info instanceof PublishedInfo published) return PUBLISHED_MAPPER.map(published);
        if (info instanceof StyleInfo style) return STYLE_MAPPER.map(style);
        if (info instanceof MapInfo map) return MAP_MAPPER.map(map);
        if (info instanceof CatalogInfo) return null;
        throw new IllegalArgumentException(
                "Unknown CatalogInfo type: %s".formatted(info.getClass().getCanonicalName()));
    }
}
