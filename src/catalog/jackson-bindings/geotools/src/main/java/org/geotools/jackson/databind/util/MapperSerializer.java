/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geotools.jackson.databind.util;

import java.io.Serial;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonToken;
import tools.jackson.core.type.WritableTypeId;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.std.StdSerializer;

/**
 * Generic {@link ValueSerializer} that applies a function from the original object type to the
 * encoded object type before {@link JsonGenerator#writePOJO(Object) writing} it
 *
 * @param <I> object model type
 * @param <D> DTO type
 */
@Slf4j
public class MapperSerializer<I, D> extends StdSerializer<I> {

    @Serial
    private static final long serialVersionUID = 1L;

    private final transient Function<I, D> mapper;

    private Class<I> type;

    public MapperSerializer(Class<I> type, java.util.function.Function<I, D> serializerMapper) {
        super(type);
        this.type = type;
        this.mapper = serializerMapper;
    }

    @Override
    public void serializeWithType(
            I value, JsonGenerator gen, SerializationContext serializers, TypeSerializer typeSer) {

        WritableTypeId typeIdDef =
                typeSer.writeTypePrefix(gen, serializers, typeSer.typeId(value, type, JsonToken.VALUE_STRING));

        serialize(value, gen, serializers);

        typeSer.writeTypeSuffix(gen, serializers, typeIdDef);
    }

    @Override
    public void serialize(I value, JsonGenerator gen, SerializationContext provider) {

        D dto;
        try {
            dto = mapper.apply(value);
        } catch (RuntimeException e) {
            log.error("Error mapping {}", value.getClass().getCanonicalName(), e);
            throw e;
        }
        try {
            gen.writePOJO(dto);
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
