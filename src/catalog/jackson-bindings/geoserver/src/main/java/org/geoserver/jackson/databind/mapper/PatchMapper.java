/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.jackson.databind.mapper;

import lombok.NonNull;

import org.geoserver.catalog.Info;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.catalog.plugin.PropertyDiff;
import org.geoserver.jackson.databind.catalog.ProxyUtils;
import org.geoserver.jackson.databind.catalog.dto.InfoReference;
import org.geoserver.jackson.databind.catalog.dto.PatchDto;
import org.geoserver.jackson.databind.catalog.mapper.ValueMappers;
import org.geoserver.jackson.databind.config.dto.mapper.GeoServerConfigMapper;
import org.geoserver.jackson.databind.config.dto.mapper.ObjectFacotries;
import org.geoserver.jackson.databind.config.dto.mapper.WPSMapper;
import org.geotools.jackson.databind.filter.dto.Literal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

@Mapper(
        uses = {
            ObjectFacotries.class,
            WPSMapper.class,
            ValueMappers.class,
            SharedMappers.class,
            GeoServerConfigMapper.class
        })
public abstract class PatchMapper {

    private static SharedMappers SHARED = Mappers.getMapper(SharedMappers.class);

    @Mapping(target = "propertyNames", ignore = true)
    public abstract Patch dtoToPatch(PatchDto dto);

    public abstract PatchDto patchToDto(Patch patch);

    protected @NonNull Literal literalValueToDto(final Object value) {
        Object proxified = valueToDto(value);
        return Literal.valueOf(proxified);
    }

    protected Object literalDtoToValueObject(Literal l) {
        Object value = l == null ? null : l.getValue();
        return dtoToValue(value);
    }

    private Object dtoToValue(final Object valueDto) {
        Object value = valueDto;
        if (valueDto instanceof InfoReference) {
            value = SHARED.referenceToInfo((InfoReference) valueDto);
        } else if (valueDto instanceof Collection) {
            Collection<?> c = (Collection<?>) valueDto;
            value = copyOf(c, this::dtoToValue);
        } else if (value instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<Object, Object> fromMap = (Map<Object, Object>) valueDto;
            value = copyOf(fromMap, this::dtoToValue);
        }
        return value;
    }

    /**
     * If value is an identified {@link Info} (catalog or config object), returns an {@link
     * InfoReference} instead, to be resolved at the receiving end
     */
    @SuppressWarnings("unchecked")
    private <V, R> R valueToDto(final V value) {
        R dto = (R) value;

        if (value instanceof Info) {
            Info info = (Info) dto;
            if (ProxyUtils.encodeByReference(info)) {
                dto = (R) SHARED.infoToReference(info);
            }
        } else if (value instanceof Collection) {
            Collection<?> c = (Collection<?>) value;
            dto = (R) copyOf(c, this::valueToDto);
        } else if (value instanceof Map) {
            Map<Object, Object> fromMap = (Map<Object, Object>) value;
            dto = (R) copyOf(fromMap, this::valueToDto);
        }
        return dto;
    }

    private Object copyOf(Map<Object, Object> fromMap, Function<Object, Object> valueMapper) {
        // create a Map of a type compatible with the original collection
        return PropertyDiff.PropertyDiffBuilder.copyOf(fromMap, valueMapper);
    }

    private <V, R> Collection<R> copyOf(Collection<? extends V> c, Function<V, R> valueMapper) {
        // create a collection of a type compatible with the original collection
        return PropertyDiff.PropertyDiffBuilder.copyOf(c, valueMapper);
    }
}
