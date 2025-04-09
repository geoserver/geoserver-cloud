/*
 * (c) 2025 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.jackson.databind.catalog;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import org.geotools.jackson.databind.filter.dto.Literal;

/**
 * Custom deserializer for Store connection parameters.
 *
 * <p>
 * This deserializer handles Literal objects in the map, extracting their values.
 * </p>
 */
public class ConnectionParametersDeserializer extends JsonDeserializer<ConnectionParameters> {

    @Override
    public ConnectionParameters deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        ConnectionParameters result = new ConnectionParameters();

        if (p.currentToken() != JsonToken.START_OBJECT) {
            throw new IOException("Expected START_OBJECT token, got " + p.currentToken());
        }

        // Read all fields in the object
        while (p.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = p.currentName();
            p.nextToken(); // Move to the value

            // Read the value
            Object value = readValue(p, ctxt);

            // If it's a Literal, unwrap it
            if (value instanceof Literal literal) {
                result.put(fieldName, literal.getValue());
            } else {
                result.put(fieldName, value);
            }
        }

        return result;
    }

    private Object readValue(JsonParser p, DeserializationContext ctxt) throws IOException {
        // For object values, try to parse as Literal
        if (p.currentToken() == JsonToken.START_OBJECT) {
            try {
                // Try to deserialize as a Literal
                return ctxt.readValue(p, Literal.class);
            } catch (Exception e) {
                // If Literal deserialization fails, fall back to basic deserialization
                // for simple types like Map
                return ctxt.readValue(p, Object.class);
            }
        }

        // For arrays, strings, numbers, booleans, null - deserialize as is
        return ctxt.readValue(p, Object.class);
    }
}
