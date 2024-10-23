/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geotools.jackson.databind.filter;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.capability.FunctionName;
import org.geotools.api.filter.expression.Expression;
import org.geotools.api.filter.sort.SortBy;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.util.InternationalString;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.jackson.databind.dto.NameDto;
import org.geotools.jackson.databind.dto.NumberRangeDto;
import org.geotools.jackson.databind.dto.VersionDto;
import org.geotools.jackson.databind.filter.dto.Literal;
import org.geotools.jackson.databind.filter.dto.LiteralDeserializer;
import org.geotools.jackson.databind.filter.dto.LiteralSerializer;
import org.geotools.jackson.databind.filter.mapper.ExpressionMapper;
import org.geotools.jackson.databind.filter.mapper.FilterMapper;
import org.geotools.jackson.databind.filter.mapper.GeoToolsValueMappers;
import org.geotools.jackson.databind.geojson.GeoToolsGeoJsonModule;
import org.geotools.jackson.databind.util.MapperDeserializer;
import org.geotools.jackson.databind.util.MapperSerializer;
import org.geotools.measure.Measure;
import org.geotools.util.NumberRange;
import org.locationtech.jts.geom.Geometry;
import org.mapstruct.factory.Mappers;

/**
 * Jackson {@link com.fasterxml.jackson.databind.Module} to handle GeoTools {@link Filter} and
 * {@link Expression} bindings.
 *
 * <p>Depends on {@link GeoToolsGeoJsonModule} to being able of encoding and decoding JTS {@link
 * Geometry} literals.
 *
 * <p>When running a spring-boot application, being on the classpath should be enough to get this
 * module auto-registered to all {@link ObjectMapper}s, by means of being registered under {@code
 * META-INF/services/com.fasterxml.jackson.databind.Module}.
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
 * objectMapper.registerModule(new GeoToolsGeoJsonModule());
 * objectMapper.registerModule(new GeoToolsFilterModule());
 * </code>
 * </pre>
 */
@Slf4j
public class GeoToolsFilterModule extends SimpleModule {
    private static final long serialVersionUID = 4898575169880138758L;

    private static final FilterMapper FILTERS = Mappers.getMapper(FilterMapper.class);
    private static final ExpressionMapper EXPRESSIONS = Mappers.getMapper(ExpressionMapper.class);
    private static final GeoToolsValueMappers VALUES = Mappers.getMapper(GeoToolsValueMappers.class);

    public GeoToolsFilterModule() {
        super(GeoToolsFilterModule.class.getSimpleName(), new Version(1, 0, 0, null, null, null));

        log.debug("registering jackson de/serializers for geotools Filter and Expression");

        addMapperSerializer(
                Expression.class,
                EXPRESSIONS::map,
                org.geotools.jackson.databind.filter.dto.Expression.class,
                EXPRESSIONS::map);

        addSerializer(Literal.class, new LiteralSerializer(), new LiteralDeserializer());

        addMapperSerializer(
                Filter.class, FILTERS::map, org.geotools.jackson.databind.filter.dto.Filter.class, FILTERS::map);

        addMapperSerializer(
                SortBy.class, FILTERS::map, org.geotools.jackson.databind.filter.dto.SortBy.class, FILTERS::map);

        addMapperSerializer(
                FunctionName.class,
                EXPRESSIONS::map,
                org.geotools.jackson.databind.filter.dto.Expression.FunctionName.class,
                EXPRESSIONS::map);

        addCustomLiteralValueSerializers();
    }

    private <T> GeoToolsFilterModule addSerializer(Class<T> type, JsonSerializer<T> ser, JsonDeserializer<T> deser) {
        super.addSerializer(type, ser);
        super.addDeserializer(type, deser);
        return this;
    }

    /** */
    private void addCustomLiteralValueSerializers() {
        addMapperSerializer(
                CoordinateReferenceSystem.class, VALUES::crs, org.geotools.jackson.databind.dto.CRS.class, VALUES::crs);
        addMapperSerializer(
                CoordinateReferenceSystem.class, VALUES::crs, org.geotools.jackson.databind.dto.CRS.class, VALUES::crs);

        addMapperSerializer(
                ReferencedEnvelope.class,
                VALUES::referencedEnvelope,
                org.geotools.jackson.databind.dto.Envelope.class,
                VALUES::referencedEnvelope);
        addMapperSerializer(java.awt.Color.class, VALUES::awtColorToString, String.class, VALUES::stringToAwtColor);
        addMapperSerializer(org.geotools.api.feature.type.Name.class, VALUES::map, NameDto.class, VALUES::map);

        addMapperSerializer(
                org.geotools.util.Version.class, VALUES::versionToDto, VersionDto.class, VALUES::dtoToVersion);

        addMapperSerializer(
                InternationalString.class,
                VALUES::internationalStringToDto,
                Map.class,
                VALUES::dtoToInternationalString);

        addMapperSerializer(
                NumberRange.class, VALUES::numberRangeToDto, NumberRangeDto.class, VALUES::dtoToNumberRange);
        addMapperSerializer(Measure.class, VALUES::measureToString, String.class, VALUES::stringToMeasure);
        addMapperSerializer(Locale.class, VALUES::localeToString, String.class, VALUES::stringToLocale);
    }

    /**
     * @param <T> object model type
     * @param <D> DTO type
     * @return
     */
    private <T, D> GeoToolsFilterModule addMapperSerializer(
            Class<T> type, Function<T, D> serializerMapper, Class<D> dtoType, Function<D, T> deserializerMapper) {

        MapperSerializer<T, D> serializer = new MapperSerializer<>(type, serializerMapper);
        MapperDeserializer<D, T> deserializer = new MapperDeserializer<>(dtoType, deserializerMapper);
        return addSerializer(type, serializer, deserializer);
    }
}
