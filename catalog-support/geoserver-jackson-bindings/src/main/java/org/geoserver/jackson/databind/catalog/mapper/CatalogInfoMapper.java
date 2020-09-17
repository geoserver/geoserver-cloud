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

@Mapper(config = SpringCatalogInfoMapperConfig.class)
public interface CatalogInfoMapper {

    @SuppressWarnings("unchecked")
    default <I extends CatalogInfo> I map(CatalogInfoDto dto) {
        if (dto == null) return null;
        if (dto instanceof Workspace)
            return (I) Mappers.getMapper(WorkspaceMapper.class).map((Workspace) dto);
        if (dto instanceof Namespace)
            return (I) Mappers.getMapper(NamespaceMapper.class).map((Namespace) dto);
        if (dto instanceof Store) return (I) Mappers.getMapper(StoreMapper.class).map((Store) dto);
        if (dto instanceof Resource)
            return (I) Mappers.getMapper(ResourceMapper.class).map((Resource) dto);
        if (dto instanceof Published)
            return (I) Mappers.getMapper(PublishedMapper.class).map((Published) dto);
        if (dto instanceof Style) return (I) Mappers.getMapper(StyleMapper.class).map((Style) dto);
        if (dto instanceof Map) return (I) Mappers.getMapper(MapMapper.class).map((Map) dto);

        throw new IllegalArgumentException(
                "Unknown CatalogInfoDto type: " + dto.getClass().getCanonicalName());
    }

    default CatalogInfoDto map(CatalogInfo info) {
        if (info == null) return null;
        if (info instanceof WorkspaceInfo)
            return Mappers.getMapper(WorkspaceMapper.class).map((WorkspaceInfo) info);
        if (info instanceof NamespaceInfo)
            return Mappers.getMapper(NamespaceMapper.class).map((NamespaceInfo) info);
        if (info instanceof StoreInfo)
            return Mappers.getMapper(StoreMapper.class).map((StoreInfo) info);
        if (info instanceof ResourceInfo)
            return Mappers.getMapper(ResourceMapper.class).map((ResourceInfo) info);
        if (info instanceof PublishedInfo)
            return Mappers.getMapper(PublishedMapper.class).map((PublishedInfo) info);
        if (info instanceof StyleInfo)
            return Mappers.getMapper(StyleMapper.class).map((StyleInfo) info);
        if (info instanceof MapInfo) return Mappers.getMapper(MapMapper.class).map((MapInfo) info);

        throw new IllegalArgumentException(
                "Unknown CatalogInfo type: " + info.getClass().getCanonicalName());
    }
}
