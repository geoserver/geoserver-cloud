/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.jackson.databind.catalog;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import lombok.extern.slf4j.Slf4j;

import org.geoserver.catalog.AttributeTypeInfo;
import org.geoserver.catalog.AuthorityURLInfo;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.CoverageDimensionInfo;
import org.geoserver.catalog.DataLinkInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.KeywordInfo;
import org.geoserver.catalog.LayerIdentifierInfo;
import org.geoserver.catalog.LegendInfo;
import org.geoserver.catalog.MetadataLinkInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.impl.ClassMappings;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.catalog.plugin.Query;
import org.geoserver.jackson.databind.catalog.dto.AttributeType;
import org.geoserver.jackson.databind.catalog.dto.AuthorityURL;
import org.geoserver.jackson.databind.catalog.dto.CoverageDimension;
import org.geoserver.jackson.databind.catalog.dto.DataLink;
import org.geoserver.jackson.databind.catalog.dto.Dimension;
import org.geoserver.jackson.databind.catalog.dto.GridGeometryDto;
import org.geoserver.jackson.databind.catalog.dto.Keyword;
import org.geoserver.jackson.databind.catalog.dto.LayerIdentifier;
import org.geoserver.jackson.databind.catalog.dto.Legend;
import org.geoserver.jackson.databind.catalog.dto.MetadataLink;
import org.geoserver.jackson.databind.catalog.dto.MetadataMapDto;
import org.geoserver.jackson.databind.catalog.dto.PatchDto;
import org.geoserver.jackson.databind.catalog.dto.QueryDto;
import org.geoserver.jackson.databind.catalog.dto.VirtualTableDto;
import org.geoserver.jackson.databind.catalog.mapper.GeoServerValueObjectsMapper;
import org.geoserver.jackson.databind.mapper.PatchMapper;
import org.geotools.api.coverage.grid.GridGeometry;
import org.geotools.jackson.databind.filter.GeoToolsFilterModule;
import org.geotools.jackson.databind.geojson.GeoToolsGeoJsonModule;
import org.geotools.jackson.databind.util.MapperDeserializer;
import org.geotools.jackson.databind.util.MapperSerializer;
import org.geotools.jdbc.VirtualTable;
import org.mapstruct.factory.Mappers;

import java.util.Arrays;
import java.util.function.Function;

/**
 * Jackson {@link com.fasterxml.jackson.databind.Module} to handle GeoServer {@link CatalogInfo}
 * bindings.
 *
 * <p>Depends on {@link GeoToolsGeoJsonModule} and {@link GeoToolsFilterModule}.
 *
 * <p>To register the module for a specific {@link ObjectMapper}, either:
 *
 * <pre>
 * <code>
 * ObjectMapper objectMapper = ...
 * objectMapper.findAndRegisterModules();
 * </code>
 * </pre>
 *
 * Or:
 *
 * <pre>
 * <code>
 * ObjectMapper objectMapper = ...
 * objectMapper.registerModule(new GeoServerCatalogModule());
 * objectMapper.registerModule(new GeoToolsGeoJsonModule());
 * objectMapper.registerModule(new GeoToolsFilterModule());
 * </code>
 * </pre>
 */
@Slf4j(topic = "org.geoserver.jackson.databind.catalog")
public class GeoServerCatalogModule extends SimpleModule {
    private static final long serialVersionUID = -8756800180255446679L;

    static final PatchMapper PATCH_MAPPER = Mappers.getMapper(PatchMapper.class);
    static final GeoServerValueObjectsMapper VALUE_MAPPER =
            Mappers.getMapper(GeoServerValueObjectsMapper.class);

    public GeoServerCatalogModule() {
        super(GeoServerCatalogModule.class.getSimpleName(), new Version(1, 0, 0, null, null, null));

        log.debug("registering jackson de/serializers for all GeoServer CatalogInfo types");

        registerCatalogInfoCodecs();
        registerValueMappers();
    }

    @SuppressWarnings("unchecked")
    protected void registerCatalogInfoCodecs() {
        this.addSerializer(CatalogInfo.class);
        this.addDeserializer(CatalogInfo.class);
        Arrays.stream(ClassMappings.values())
                .map(ClassMappings::getInterface)
                .filter(CatalogInfo.class::isAssignableFrom)
                .map(c -> (Class<? extends CatalogInfo>) c)
                .distinct()
                .sorted((c1, c2) -> c1.getSimpleName().compareTo(c2.getSimpleName()))
                .forEach(
                        c -> {
                            this.addSerializer(c);
                            this.addDeserializer(c);
                        });
    }

    private <T extends CatalogInfo> void addSerializer(Class<T> clazz) {
        log.trace("registering serializer for {}", clazz.getSimpleName());
        super.addSerializer(new CatalogInfoSerializer<>(clazz));
    }

    private <T extends CatalogInfo> void addDeserializer(Class<T> clazz) {
        log.trace("registering deserializer for {}", clazz.getSimpleName());
        super.addDeserializer(clazz, new CatalogInfoDeserializer<>());
    }

    /**
     * @param <T> object model type
     * @param <D> DTO type
     */
    private <T, D> void addMapperSerializer(
            Class<T> type,
            Function<T, D> serializerMapper,
            Class<D> dtoType,
            Function<D, T> deserializerMapper) {

        MapperSerializer<T, D> serializer = new MapperSerializer<>(type, serializerMapper);
        MapperDeserializer<D, T> deserializer =
                new MapperDeserializer<>(dtoType, deserializerMapper);
        super.addSerializer(type, serializer);
        super.addDeserializer(type, deserializer);
    }

    private void registerValueMappers() {
        addMapperSerializer(
                Patch.class, PATCH_MAPPER::patchToDto, PatchDto.class, PATCH_MAPPER::dtoToPatch);

        addMapperSerializer(
                KeywordInfo.class, VALUE_MAPPER::keyword, Keyword.class, VALUE_MAPPER::keyword);

        addMapperSerializer(
                VirtualTable.class,
                VALUE_MAPPER::virtualTableToDto,
                VirtualTableDto.class,
                VALUE_MAPPER::dtoToVirtualTable);
        addMapperSerializer(
                MetadataLinkInfo.class,
                VALUE_MAPPER::infoToDto,
                MetadataLink.class,
                VALUE_MAPPER::dtoToInfo);
        addMapperSerializer(
                LegendInfo.class, VALUE_MAPPER::infoToDto, Legend.class, VALUE_MAPPER::dtoToInfo);
        addMapperSerializer(
                LayerIdentifierInfo.class,
                VALUE_MAPPER::infoToDto,
                LayerIdentifier.class,
                VALUE_MAPPER::dtoToInfo);
        addMapperSerializer(
                DataLinkInfo.class,
                VALUE_MAPPER::infoToDto,
                DataLink.class,
                VALUE_MAPPER::dtoToInfo);
        addMapperSerializer(
                DimensionInfo.class,
                VALUE_MAPPER::infoToDto,
                Dimension.class,
                VALUE_MAPPER::dtoToInfo);
        addMapperSerializer(
                CoverageDimensionInfo.class,
                VALUE_MAPPER::infoToDto,
                CoverageDimension.class,
                VALUE_MAPPER::dtoToInfo);
        addMapperSerializer(
                AuthorityURLInfo.class,
                VALUE_MAPPER::infoToDto,
                AuthorityURL.class,
                VALUE_MAPPER::dtoToInfo);
        addMapperSerializer(
                GridGeometry.class,
                VALUE_MAPPER::gridGeometry2DToDto,
                GridGeometryDto.class,
                VALUE_MAPPER::dtoToGridGeometry2D);

        addMapperSerializer(
                Query.class, VALUE_MAPPER::queryToDto, QueryDto.class, VALUE_MAPPER::dtoToQuery);

        addMapperSerializer(
                AttributeTypeInfo.class,
                VALUE_MAPPER::infoToDto,
                AttributeType.class,
                VALUE_MAPPER::dtoToInfo);

        addMapperSerializer(
                MetadataMap.class,
                VALUE_MAPPER::metadataMap,
                MetadataMapDto.class,
                VALUE_MAPPER::metadataMap);
    }
}
