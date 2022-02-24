/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.jackson.databind.catalog;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import lombok.extern.slf4j.Slf4j;

import org.geoserver.catalog.AuthorityURLInfo;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.CoverageDimensionInfo;
import org.geoserver.catalog.DataLinkInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.KeywordInfo;
import org.geoserver.catalog.LayerIdentifierInfo;
import org.geoserver.catalog.LegendInfo;
import org.geoserver.catalog.MetadataLinkInfo;
import org.geoserver.catalog.impl.ClassMappings;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.catalog.plugin.Query;
import org.geoserver.jackson.databind.catalog.dto.AuthorityURL;
import org.geoserver.jackson.databind.catalog.dto.CRS;
import org.geoserver.jackson.databind.catalog.dto.CoverageDimension;
import org.geoserver.jackson.databind.catalog.dto.DataLink;
import org.geoserver.jackson.databind.catalog.dto.Dimension;
import org.geoserver.jackson.databind.catalog.dto.Envelope;
import org.geoserver.jackson.databind.catalog.dto.GridGeometryDto;
import org.geoserver.jackson.databind.catalog.dto.Keyword;
import org.geoserver.jackson.databind.catalog.dto.LayerIdentifier;
import org.geoserver.jackson.databind.catalog.dto.Legend;
import org.geoserver.jackson.databind.catalog.dto.MetadataLink;
import org.geoserver.jackson.databind.catalog.dto.NumberRangeDto;
import org.geoserver.jackson.databind.catalog.dto.PatchDto;
import org.geoserver.jackson.databind.catalog.dto.QueryDto;
import org.geoserver.jackson.databind.catalog.dto.VersionDto;
import org.geoserver.jackson.databind.catalog.dto.VirtualTableDto;
import org.geoserver.jackson.databind.catalog.mapper.ValueMappers;
import org.geoserver.jackson.databind.mapper.SharedMappers;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.jackson.databind.filter.GeoToolsFilterModule;
import org.geotools.jackson.databind.geojson.GeoToolsGeoJsonModule;
import org.geotools.jackson.databind.util.MapperDeserializer;
import org.geotools.jackson.databind.util.MapperSerializer;
import org.geotools.jdbc.VirtualTable;
import org.geotools.measure.Measure;
import org.geotools.util.NumberRange;
import org.mapstruct.factory.Mappers;
import org.opengis.coverage.grid.GridGeometry;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.InternationalString;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
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

    static final SharedMappers SHARED_MAPPER = Mappers.getMapper(SharedMappers.class);
    static final ValueMappers VALUE_MAPPER = Mappers.getMapper(ValueMappers.class);

    private final CatalogInfoDeserializer<CatalogInfo> deserializer =
            new CatalogInfoDeserializer<>();

    public GeoServerCatalogModule() {
        super(GeoServerCatalogModule.class.getSimpleName(), new Version(1, 0, 0, null, null, null));

        log.debug("registering jackson de/serializers for all GeoServer CatalogInfo types");

        registerCatalogInfoCodecs();
        registerCatalogInfoValueMappers();
        registerSharedMappers();
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

    @SuppressWarnings("unchecked")
    private <T extends CatalogInfo> void addDeserializer(Class<T> clazz) {
        log.trace("registering deserializer for {}", clazz.getSimpleName());
        super.addDeserializer(clazz, (CatalogInfoDeserializer<T>) deserializer);
    }

    private <T, DTO> void addMapperSerializer(
            Class<T> type,
            Function<T, DTO> serializerMapper,
            Class<DTO> dtoType,
            Function<DTO, T> deserializerMapper) {

        MapperSerializer<T, DTO> serializer = new MapperSerializer<>(type, serializerMapper);
        MapperDeserializer<DTO, T> deserializer =
                new MapperDeserializer<>(dtoType, deserializerMapper);
        super.addSerializer(type, serializer);
        super.addDeserializer(type, deserializer);
    }

    private void registerCatalogInfoValueMappers() {
        addMapperSerializer(
                Measure.class,
                VALUE_MAPPER::measureToString,
                String.class,
                VALUE_MAPPER::stringToMeasure);
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
                NumberRange.class,
                VALUE_MAPPER::numberRangeToDto,
                NumberRangeDto.class,
                VALUE_MAPPER::dtoToNumberRange);
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
                Measure.class,
                VALUE_MAPPER::measureToString,
                String.class,
                VALUE_MAPPER::stringToMeasure);
        addMapperSerializer(
                GridGeometry.class,
                VALUE_MAPPER::gridGeometry2DToDto,
                GridGeometryDto.class,
                VALUE_MAPPER::dtoToGridGeometry2D);

        addMapperSerializer(
                Query.class, VALUE_MAPPER::queryToDto, QueryDto.class, VALUE_MAPPER::dtoToQuery);

        addMapperSerializer(
                Locale.class,
                VALUE_MAPPER::localeToString,
                String.class,
                VALUE_MAPPER::stringToLocale);

        addMapperSerializer(
                InternationalString.class,
                VALUE_MAPPER::internationalStringToDto,
                Map.class,
                VALUE_MAPPER::dtoToInternationalString);
    }

    private void registerSharedMappers() {
        addMapperSerializer(
                CoordinateReferenceSystem.class, SHARED_MAPPER::crs, CRS.class, SHARED_MAPPER::crs);

        addMapperSerializer(
                ReferencedEnvelope.class,
                SHARED_MAPPER::referencedEnvelopeToDto,
                Envelope.class,
                SHARED_MAPPER::dtoToReferencedEnvelope);

        addMapperSerializer(
                Patch.class, SHARED_MAPPER::patchToDto, PatchDto.class, SHARED_MAPPER::dtoToPatch);

        addMapperSerializer(
                org.geotools.util.Version.class,
                SHARED_MAPPER::versionToDto,
                VersionDto.class,
                SHARED_MAPPER::dtoToVersion);

        addMapperSerializer(
                KeywordInfo.class,
                SHARED_MAPPER::keywordToDto,
                Keyword.class,
                SHARED_MAPPER::dtoToKeyword);
    }
}
