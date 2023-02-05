/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.cog.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.WritableTypeId;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.function.Function;

@Slf4j
class MapperSerializer<I, DTO> extends StdSerializer<I> {

    private static final long serialVersionUID = 1L;

    private Function<I, DTO> mapper;

    private Class<I> type;

    public MapperSerializer(Class<I> type, java.util.function.Function<I, DTO> serializerMapper) {
        super(type);
        this.type = type;
        this.mapper = serializerMapper;
    }

    public @Override void serializeWithType(
            I value, JsonGenerator gen, SerializerProvider serializers, TypeSerializer typeSer)
            throws IOException {

        WritableTypeId typeIdDef =
                typeSer.writeTypePrefix(gen, typeSer.typeId(value, type, JsonToken.VALUE_STRING));

        serialize(value, gen, null);

        typeSer.writeTypeSuffix(gen, typeIdDef);
    }

    public @Override void serialize(I value, JsonGenerator gen, SerializerProvider provider)
            throws IOException {

        DTO dto;
        try {
            dto = mapper.apply(value);
        } catch (RuntimeException e) {
            log.error("Error mapping {}", value.getClass().getCanonicalName(), e);
            throw e;
        }
        try {
            gen.writeObject(dto);
        } catch (RuntimeException e) {
            log.error(
                    "Error writing value mapped from {} to {}",
                    value.getClass().getCanonicalName(),
                    dto == null ? "null" : dto.getClass().getCanonicalName(),
                    e);
            throw e;
        }
    }
}
