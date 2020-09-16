package org.geotools.jackson.databind.filter.dto;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;

public class LiteralValueSerializer extends StdSerializer<Object> {
    private static final long serialVersionUID = 1L;

    public LiteralValueSerializer() {
        super(Object.class);
    }

    public @Override void serialize(Object value, JsonGenerator gen, SerializerProvider serializers)
            throws IOException {

        gen.writeStartObject();
        gen.writeStringField("type", value == null ? "null" : value.getClass().getCanonicalName());
        gen.writeObjectField("value", value);
        gen.writeEndObject();
    }
}
