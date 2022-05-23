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
        if (dto instanceof Workspace) return (I) WORKSPACE_MAPPER.map((Workspace) dto);
        if (dto instanceof Namespace) return (I) NAMESPACE_MAPPER.map((Namespace) dto);
        if (dto instanceof Store) return (I) STORE_MAPPER.map((Store) dto);
        if (dto instanceof Resource) return (I) RESOURCE_MAPPER.map((Resource) dto);
        if (dto instanceof Published) return (I) PUBLISHED_MAPPER.map((Published) dto);
        if (dto instanceof Style) return (I) STYLE_MAPPER.map((Style) dto);
        if (dto instanceof Map) return (I) MAP_MAPPER.map((Map) dto);

        throw new IllegalArgumentException(
                "Unknown CatalogInfoDto type: " + dto.getClass().getCanonicalName());
    }

    default CatalogInfoDto map(CatalogInfo info) {
        if (info == null) return null;
        if (info instanceof WorkspaceInfo) return WORKSPACE_MAPPER.map((WorkspaceInfo) info);
        if (info instanceof NamespaceInfo) return NAMESPACE_MAPPER.map((NamespaceInfo) info);
        if (info instanceof StoreInfo) return STORE_MAPPER.map((StoreInfo) info);
        if (info instanceof ResourceInfo) return RESOURCE_MAPPER.map((ResourceInfo) info);
        if (info instanceof PublishedInfo) return PUBLISHED_MAPPER.map((PublishedInfo) info);
        if (info instanceof StyleInfo) return STYLE_MAPPER.map((StyleInfo) info);
        if (info instanceof MapInfo) return MAP_MAPPER.map((MapInfo) info);

        throw new IllegalArgumentException(
                "Unknown CatalogInfo type: " + info.getClass().getCanonicalName());
    }
}
