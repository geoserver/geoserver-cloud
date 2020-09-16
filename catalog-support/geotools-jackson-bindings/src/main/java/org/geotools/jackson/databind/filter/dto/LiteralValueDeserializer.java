package org.geotools.jackson.databind.filter.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;

public class LiteralValueDeserializer extends JsonDeserializer<Object> {

    public @Override Object deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException, JsonProcessingException {

        ObjectNode node = p.readValueAsTree();
        String className = node.findValue("class").asText();
        if ("null".equals(className)) {
            return null;
        }
        Class<?> type;
        try {
            type = ctxt.findClass(className);
        } catch (ClassNotFoundException e) {
            throw ValueInstantiationException.from(p, "Can't find class " + className, e);
        }
        Object value = ctxt.readValue(p, type);
        return value;
    }
}
