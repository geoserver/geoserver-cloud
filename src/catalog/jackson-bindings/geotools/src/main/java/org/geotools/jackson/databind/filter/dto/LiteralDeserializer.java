/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geotools.jackson.databind.filter.dto;

import static java.util.Objects.requireNonNull;
import static org.geotools.jackson.databind.filter.dto.LiteralSerializer.COLLECTION_CONTENT_TYPE_KEY;
import static org.geotools.jackson.databind.filter.dto.LiteralSerializer.TYPE_KEY;
import static org.geotools.jackson.databind.filter.dto.LiteralSerializer.VALUE_KEY;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.*;
import org.geotools.jackson.databind.filter.mapper.GeoToolsValueMappers;
import org.mapstruct.factory.Mappers;

/**
 *
 *
 * <pre>
 * <code>
 * {
 *  "type": canonical name of the value type (e.g. "java.lang.Integer", "int[]", etc.)
 *  "contentType": optional, content type if type is a collection type
 *  "value": encoded value
 * }
 * </code>
 * </pre>
 *
 * @since 1.0
 */
public class LiteralDeserializer extends JsonDeserializer<Literal> {

    private static GeoToolsValueMappers classNameMapper = Mappers.getMapper(GeoToolsValueMappers.class);

    @Override
    public Literal deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
        expect(parser.currentToken(), JsonToken.START_OBJECT);
        final Class<?> type = readType(parser);
        if (null == type) {
            return Literal.valueOf(null);
        }
        final Class<?> contentType;
        final Object value;

        String fieldName = parser.nextFieldName();
        requireNonNull(fieldName, "expected contentType or value attribute, got null");
        if (COLLECTION_CONTENT_TYPE_KEY.equals(fieldName)) {
            String contentTypeVal = parser.nextTextValue();
            requireNonNull(contentTypeVal, "expected value for contentType, got null");
            contentType = classNameMapper.canonicalNameToClass(contentTypeVal);
            fieldName = parser.nextFieldName();
        } else {
            contentType = null;
        }

        expectFieldName(fieldName, VALUE_KEY);
        if (type.isArray()) {
            value = readArray(type, parser, ctxt);
        } else if (Collection.class.isAssignableFrom(type)) {
            value = readCollection(type, contentType, parser, ctxt);
        } else if (Map.class.isAssignableFrom(type)) {
            value = readMap(parser, ctxt);
        } else {
            parser.nextToken();
            value = ctxt.readValue(parser, type);
        }
        JsonToken token = parser.nextToken();
        expect(token, JsonToken.END_OBJECT);
        return Literal.valueOf(value);
    }

    private Object readMap(JsonParser parser, DeserializationContext ctxt) throws IOException {
        JsonToken nextToken = parser.nextToken();
        expect(nextToken, JsonToken.START_OBJECT);

        Map<String, Object> parsed = new LinkedHashMap<>();
        String key;
        while (null != (key = parser.nextFieldName())) {
            expect(parser.nextToken(), JsonToken.START_OBJECT);
            Literal valueLiteral = ctxt.readValue(parser, Literal.class);
            Object value = valueLiteral.getValue();
            parsed.put(key, value);
        }

        // nextFieldName() already advanced to the next token
        nextToken = parser.currentToken();
        expect(nextToken, JsonToken.END_OBJECT);
        return parsed;
    }

    private Collection<Object> readCollection(
            Class<?> type, Class<?> contentType, JsonParser parser, DeserializationContext ctxt) throws IOException {

        JsonToken nextToken = parser.nextToken();
        expect(nextToken, JsonToken.START_ARRAY);
        Collection<Object> value = readList(contentType, parser, ctxt);
        if (Set.class.isAssignableFrom(type)) {
            value = new LinkedHashSet<>(value);
        }
        return value;
    }

    private Object readArray(Class<?> arrayType, JsonParser parser, DeserializationContext ctxt) throws IOException {
        JsonToken nextToken = parser.nextToken();
        Object value;
        if (byte[].class.equals(arrayType)) {
            // special case, byte[] is encoded as a base64 string. VALUE_EMBEDDED_OBJECT is the
            // token when encoding as YAML
            expect(nextToken, JsonToken.VALUE_STRING, JsonToken.VALUE_EMBEDDED_OBJECT);
            value = parser.getBinaryValue();
        } else {
            expect(nextToken, JsonToken.START_ARRAY);
            Class<?> componentType = arrayType.getComponentType();
            List<Object> list = readList(componentType, parser, ctxt);
            Object array = Array.newInstance(componentType, list.size());
            for (int i = 0; i < list.size(); i++) {
                Object v = list.get(i);
                Array.set(array, i, v);
            }
            value = array;
        }
        return value;
    }

    private List<Object> readList(Class<?> contentType, JsonParser parser, DeserializationContext ctxt)
            throws IOException {

        JsonToken nextToken = parser.currentToken();
        expect(nextToken, JsonToken.START_ARRAY);
        List<Object> value = new ArrayList<>();
        if (null == contentType) {
            // store the values to parse the collection later
            ArrayNode valuesArrayNode = parser.readValueAs(ArrayNode.class);

            if (valuesArrayNode.isEmpty()) {
                return value;
            }

            if (isOnlyNulls(valuesArrayNode)) {
                return convertArrayNodeToArrayList(contentType, ctxt, valuesArrayNode);
            }

            // we only end up here if the "value" field appeared
            // before the "contentType" field during the previous parsing

            // but we have to know the contentType to be
            // able to parse a list of that type
            // (order issue is probably related to JSONB storage in postgres)
            nextToken = parser.nextToken();
            if (nextToken == JsonToken.FIELD_NAME) {
                String fieldName = parser.currentName();
                expectFieldName(fieldName, COLLECTION_CONTENT_TYPE_KEY);

                String contentTypeVal = parser.nextTextValue();
                requireNonNull(contentTypeVal, "expected value for contentType, got null");

                contentType = classNameMapper.canonicalNameToClass(contentTypeVal);
            }

            value = convertArrayNodeToArrayList(contentType, ctxt, valuesArrayNode);
        } else {
            while ((nextToken = parser.nextToken()) != JsonToken.END_ARRAY) {
                Object item;
                if (JsonToken.VALUE_NULL == nextToken) {
                    item = null;
                } else {
                    item = ctxt.readValue(parser, contentType);
                    if (item instanceof Literal literal) {
                        item = literal.getValue();
                    }
                }
                value.add(item);
            }
        }
        return value;
    }

    private Class<?> readType(JsonParser parser) throws IOException {
        final String typeFieldName = parser.nextFieldName();
        if (VALUE_KEY.equals(typeFieldName)) {
            // value can only be the first fieldname if it's null
            JsonToken nextToken = parser.nextToken();
            if (JsonToken.VALUE_NULL != nextToken) {
                throw new IllegalArgumentException(
                        "First field is value, expected VALUE_NULL, or first field shall be 'type'");
            }
            nextToken = parser.nextToken();
            if (JsonToken.END_OBJECT != nextToken) {
                throw new IllegalArgumentException("Expected END_OBJECT, got %s".formatted(nextToken));
            }
            return null;
        }

        expectFieldName(typeFieldName, TYPE_KEY);
        final String typeString = parser.nextTextValue();
        requireNonNull(typeString, "type is null");
        return classNameMapper.canonicalNameToClass(typeString);
    }

    /**
     * @param value actual value
     * @param expected expected value
     */
    private void expectFieldName(String value, String expected) {
        if (!expected.equals(value))
            throw new IllegalStateException("Expected field name '%s', got '%s'".formatted(expected, value));
    }

    private void expect(JsonToken current, JsonToken... expectedOneOf) {
        for (JsonToken expected : expectedOneOf) {
            if (current == expected) return;
        }
        throw new IllegalStateException("Expected one of %s got %s".formatted(Arrays.toString(expectedOneOf), current));
    }

    private static List<Object> convertArrayNodeToArrayList(
            Class<?> contentTypeClass, DeserializationContext ctxt, ArrayNode valuesArrayNode) throws IOException {
        List<Object> value;
        ArrayList<Object> valuesList = new ArrayList<>();
        for (JsonNode node : valuesArrayNode) {
            if (node instanceof NullNode) {
                valuesList.add(null);
            } else if (contentTypeClass != null) {
                if (Literal.class.isAssignableFrom(contentTypeClass)) {
                    // some special care in case of Literal.class
                    final JsonNode literalNode = node.get("Literal");
                    final JsonNode valueNode = literalNode.get("value");
                    final JsonNode typeNode = literalNode.get("type");

                    if (typeNode == null && valueNode.isNull()) {
                        // "type" does not exist as a field,
                        // but "value" does with a null value
                        valuesList.add(null);
                    } else if (typeNode != null) {
                        String typeVal = typeNode.asText();
                        Class<?> type = classNameMapper.canonicalNameToClass(typeVal);
                        Object val = ctxt.readTreeAsValue(valueNode, type);
                        valuesList.add(val);
                    }
                } else {
                    // finally parse a collection with the now known "arbitrary" content type
                    Object item = ctxt.readTreeAsValue(node, contentTypeClass);
                    valuesList.add(item);
                }
            }
        }
        value = valuesList;
        return value;
    }

    private static boolean isOnlyNulls(ArrayNode arrayNode) {
        if (arrayNode == null || arrayNode.isEmpty()) {
            return false;
        }

        for (JsonNode node : arrayNode) {
            if (!(node instanceof NullNode)) {
                return false;
            }
        }
        return true;
    }
}
