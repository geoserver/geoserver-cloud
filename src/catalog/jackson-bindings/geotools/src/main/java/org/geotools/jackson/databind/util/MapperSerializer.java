/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geotools.jackson.databind.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.WritableTypeId;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;

/**
 * Generic {@link JsonSerializer} that applies a function from the original object type to the
 * encoded object type before {@link JsonGenerator#writeObject(Object) writing} it
 *
 * @param <I> object model type
 * @param <D> DTO type
 */
@Slf4j
public class MapperSerializer<I, D> extends StdSerializer<I> {

    private static final long serialVersionUID = 1L;

    private final transient Function<I, D> mapper;

    private Class<I> type;

    public MapperSerializer(Class<I> type, java.util.function.Function<I, D> serializerMapper) {
        super(type);
        this.type = type;
        this.mapper = serializerMapper;
    }

    @Override
    public void serializeWithType(I value, JsonGenerator gen, SerializerProvider serializers, TypeSerializer typeSer)
            throws IOException {

        WritableTypeId typeIdDef = typeSer.writeTypePrefix(gen, typeSer.typeId(value, type, JsonToken.VALUE_STRING));

        serialize(value, gen, null);

        typeSer.writeTypeSuffix(gen, typeIdDef);
    }

    @Override
    public void serialize(I value, JsonGenerator gen, SerializerProvider provider) throws IOException {

        D dto;
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
