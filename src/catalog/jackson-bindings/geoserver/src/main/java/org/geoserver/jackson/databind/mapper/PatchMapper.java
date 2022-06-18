/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.jackson.databind.mapper;

import lombok.extern.slf4j.Slf4j;

import org.geoserver.catalog.Info;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.catalog.plugin.PropertyDiff;
import org.geoserver.jackson.databind.catalog.ProxyUtils;
import org.geoserver.jackson.databind.catalog.dto.InfoReference;
import org.geoserver.jackson.databind.catalog.dto.PatchDto;
import org.geotools.jackson.databind.filter.dto.Expression;
import org.geotools.jackson.databind.filter.dto.Expression.Literal;
import org.geotools.jackson.databind.filter.mapper.ExpressionMapper;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

@Mapper
@Slf4j(topic = "org.geoserver.jackson.databind.mapper")
public abstract class PatchMapper {

    private static SharedMappers SHARED = Mappers.getMapper(SharedMappers.class);

    private static ExpressionMapper expressionMapper = Mappers.getMapper(ExpressionMapper.class);

    public Patch dtoToPatch(PatchDto dto) {
        if (dto != null) {
            Patch patch = new Patch();
            dto.getPatches().entrySet().stream().map(this::dtoToProperty).forEach(patch::add);
            return patch;
        }
        return null;
    }

    public PatchDto patchToDto(Patch patch) {
        if (patch == null) return null;

        PatchDto dto = new PatchDto();
        for (Patch.Property propChange : patch.getPatches()) {
            String name = propChange.getName();
            Object value = valueToDto(propChange.getValue());
            if (value instanceof InfoReference && log.isTraceEnabled()) {
                log.trace(
                        "Replaced patch property {} of type {} by {}",
                        name,
                        ProxyUtils.referenceTypeOf(propChange.getValue()).orElse(null),
                        value);
            }
            Literal literal = new org.geotools.jackson.databind.filter.dto.Expression.Literal();
            literal.setValue(value);
            dto.getPatches().put(name, literal);
        }
        return dto;
    }

    private Patch.Property dtoToProperty(Map.Entry<String, Expression.Literal> entry) {
        final String name = entry.getKey();
        final Expression.Literal dto = entry.getValue();
        org.opengis.filter.expression.Literal literal = expressionMapper.map(dto);
        Object valueDto = literal.getValue();
        Object propertyValye = dtoToValue(valueDto);
        return new Patch.Property(name, propertyValye);
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
    private Object valueToDto(final Object value) {
        Object dto = value;
        if (ProxyUtils.encodeByReference(value)) {
            dto = SHARED.infoToReference((Info) value);
        } else if (value instanceof Collection) {
            Collection<?> c = (Collection<?>) value;
            dto = copyOf(c, this::valueToDto);
        } else if (value instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<Object, Object> fromMap = (Map<Object, Object>) value;
            dto = copyOf(fromMap, this::valueToDto);
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
