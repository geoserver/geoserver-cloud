/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.jackson.databind.mapper;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import lombok.Generated;
import lombok.NonNull;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.catalog.plugin.PropertyDiff;
import org.geoserver.catalog.plugin.resolving.ProxyUtils;
import org.geoserver.jackson.databind.catalog.dto.PatchDto;
import org.geoserver.jackson.databind.catalog.dto.ResolvingProxyDto;
import org.geoserver.jackson.databind.catalog.mapper.GeoServerValueObjectsMapper;
import org.geoserver.jackson.databind.config.mapper.GeoServerConfigMapper;
import org.geoserver.jackson.databind.config.mapper.ObjectFactories;
import org.geoserver.jackson.databind.config.mapper.WPSMapper;
import org.geotools.jackson.databind.filter.dto.LiteralDto;
import org.mapstruct.AnnotateWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        uses = {
            ObjectFactories.class,
            WPSMapper.class,
            GeoServerValueObjectsMapper.class,
            ResolvingProxyMapper.class,
            GeoServerConfigMapper.class
        })
@AnnotateWith(value = Generated.class)
public abstract class PatchMapper {

    private static final ResolvingProxyMapper REFS = Mappers.getMapper(ResolvingProxyMapper.class);

    @Mapping(target = "propertyNames", ignore = true)
    public abstract Patch dtoToPatch(PatchDto dto);

    public abstract PatchDto patchToDto(Patch patch);

    protected @NonNull LiteralDto literalValueToDto(final Object value) {
        Object proxified = valueToDto(value);
        return LiteralDto.valueOf(proxified);
    }

    protected Object literalDtoToValueObject(LiteralDto l) {
        Object value = l == null ? null : l.getValue();
        return dtoToValue(value);
    }

    private Object dtoToValue(final Object valueDto) {
        Object value = valueDto;
        if (valueDto instanceof ResolvingProxyDto infoRef) {
            value = REFS.referenceToInfo(infoRef);
        } else if (valueDto instanceof Collection<?> c) {
            value = copyOf(c, this::dtoToValue);
        } else if (value instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<Object, Object> fromMap = (Map<Object, Object>) valueDto;
            value = copyOf(fromMap, this::dtoToValue);
        }
        return value;
    }

    /**
     * If value is an identified {@link Info} (catalog or config object), returns an {@link ResolvingProxyDto} instead,
     * to be resolved at the receiving end
     */
    @SuppressWarnings("unchecked")
    private <V, R> R valueToDto(final V value) {
        R dto = (R) value;

        if (value instanceof Info info) {
            if (ProxyUtils.encodeByReference(info)) {
                dto = (R) REFS.infoToReference(info);
            }
            // note if info is a value object Info (i.e. can't have id) and still is a ModificationProxy, a Jackson
            // serializer/deserializer must be registered against its interface (e.g. see GeoServerConfigModule
            // registering de/serializer for UserDetailsDisplaySettings
        } else if (value instanceof Collection<?> c) {
            dto = (R) copyOf(c, this::valueToDto);
        } else if (value instanceof Map) {
            Map<Object, Object> fromMap = (Map<Object, Object>) value;
            dto = (R) copyOf(fromMap, this::valueToDto);
        }
        return dto;
    }

    private Map<Object, Object> copyOf(Map<Object, Object> fromMap, UnaryOperator<Object> valueMapper) {
        // create a Map of a type compatible with the original collection
        return PropertyDiff.PropertyDiffBuilder.copyOf(fromMap, valueMapper);
    }

    private <V, R> Collection<R> copyOf(Collection<? extends V> c, Function<V, R> valueMapper) {
        // create a collection of a type compatible with the original collection
        return PropertyDiff.PropertyDiffBuilder.copyOf(c, valueMapper);
    }
}
