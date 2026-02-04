/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jackson.databind.catalog;

import com.fasterxml.jackson.core.JsonGenerator;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.Map;
import org.geotools.jackson.databind.filter.dto.Literal;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

/**
 * Custom serializer for Store connection parameters.
 *
 * <p>
 * This serializer handles complex types like ReferencedEnvelope by wrapping them in a Literal.
 * </p>
 */
public class ConnectionParametersSerializer extends ValueSerializer<ConnectionParameters> {

    @Override
    public Class<ConnectionParameters> handledType() {
        return ConnectionParameters.class;
    }

    @Override
    public void serialize(ConnectionParameters value, JsonGenerator gen, SerializationContext serializers)
            throws IOException {

        if (value == null) {
            gen.writeNull();
            return;
        }

        gen.writeStartObject();
        for (Map.Entry<String, Object> entry : value.entrySet()) {
            String key = entry.getKey();
            Object val = entry.getValue();

            if (val == null) {
                gen.writeNullProperty(key);
            } else if (shouldConvertToString(val)) {
                // Values like URI, URL, File, Path should be serialized as strings
                gen.writeStringProperty(key, val.toString());
            } else if (shouldWrapAsLiteral(val)) {
                // Complex type, wrap in Literal
                Literal literal = Literal.valueOf(val);
                gen.writeObjectProperty(key, literal);
            } else {
                // Primitive type
                gen.writeObjectProperty(key, val);
            }
        }
        gen.writeEndObject();
    }

    /**
     * Determine if a value should be converted to a String.
     *
     * <p>
     * Common types like URI, URL, File, and Path should be serialized as strings
     * since DataAccessFactory.Param.lookUp() will convert them back to the proper type.
     * </p>
     */
    private boolean shouldConvertToString(Object value) {
        return value instanceof URI || value instanceof URL || value instanceof File || value instanceof Path;
    }

    /**
     * Determine if a value should be wrapped in a Literal.
     */
    private boolean shouldWrapAsLiteral(Object value) {
        if (value == null) {
            return false;
        }

        // Basic types don't need to be wrapped
        if (value instanceof String || value instanceof Number || value instanceof Boolean) {
            return false;
        }

        // Types that are converted to strings don't need to be wrapped
        return !shouldConvertToString(value);
    }
}
