/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geotools.jackson.databind.filter.dto;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.UnaryOperator;
import lombok.NonNull;
import org.geotools.jackson.databind.filter.mapper.GeoToolsValueMappers;
import org.mapstruct.factory.Mappers;
import tools.jackson.core.Base64Variant;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonToken;
import tools.jackson.core.type.WritableTypeId;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.std.StdSerializer;
import tools.jackson.databind.type.TypeFactory;

/**
 *
 *
 * <pre>
 * <code>
 * {
 *  "type": canonical name of the value type (e.g. "java.lang.Integer", "int[]", etc.)
 *  "contentType": optional, content type if type is a collection type. Maybe Literal.class if the collection elements are not of an homogeneous type
 *  "value": encoded value
 * }
 * </code>
 * </pre>
 *
 * @since 1.0
 */
public class LiteralSerializer extends StdSerializer<Literal> {

    /** */
    static final String TYPE_KEY = "type";

    /** */
    static final String VALUE_KEY = "value";

    /** see {@link #writeCollection} */
    static final String COLLECTION_CONTENT_TYPE_KEY = "contentType";

    final GeoToolsValueMappers classNameMapper = Mappers.getMapper(GeoToolsValueMappers.class);

    public LiteralSerializer() {
        super(Literal.class);
    }

    protected GeoToolsValueMappers classNameMapper() {
        return classNameMapper;
    }

    @Override
    public void serializeWithType(
            Literal value, JsonGenerator g, SerializationContext provider, TypeSerializer typeSer) {

        WritableTypeId typeIdDef = typeSer.writeTypePrefix(g, provider, typeSer.typeId(value, JsonToken.VALUE_STRING));
        serialize(value, g, provider);
        typeSer.writeTypeSuffix(g, provider, typeIdDef);
    }

    @Override
    public void serialize(Literal literal, JsonGenerator gen, SerializationContext serializers) {

        final Object value = literal.getValue();

        gen.writeStartObject();
        writeValue(value, gen, serializers);
        gen.writeEndObject();
    }

    protected void writeValue(final Object value, JsonGenerator gen, SerializationContext provider) {
        if (value == null) {
            gen.writeNullProperty(VALUE_KEY);
        } else {
            writeNonNullValue(value, gen, provider);
        }
    }

    private void writeNonNullValue(final @NonNull Object value, JsonGenerator gen, SerializationContext provider) {
        Class<?> type = value.getClass();

        if (type.isArray()) {
            writeArray(value, gen, provider);
        } else if (Collection.class.isAssignableFrom(type)) {
            writeCollection((Collection<?>) value, gen, provider);
        } else if (Map.class.isAssignableFrom(type)) {
            writeMap((Map<?, ?>) value, gen);
        } else {
            serializeNonCollectionObject(value, gen, provider, type);
        }
    }

    private void serializeNonCollectionObject(
            final @NonNull Object value, JsonGenerator gen, SerializationContext provider, Class<?> type) {

        if (type.isAnonymousClass()) {
            Class<?> enclosingClass = type.getEnclosingClass();
            if (enclosingClass.isEnum()) {
                type = enclosingClass;
            } else {
                throw new IllegalArgumentException("Unable to encode anonymous class %s".formatted(type.getName()));
            }
        }

        ValueSerializer<Object> valueSerializer = findValueSerializer(provider, type);
        final Class<?> handledType = valueSerializer.handledType();
        String typeName = classNameMapper().classToCanonicalName(type.isEnum() ? type : handledType);
        gen.writeStringProperty(TYPE_KEY, typeName);
        gen.writeName(VALUE_KEY);
        valueSerializer.serialize(value, gen, provider);
    }

    protected ValueSerializer<Object> findValueSerializer(SerializationContext provider, final Class<?> type) {
        TypeFactory typeFactory = provider.getTypeFactory();
        JavaType javaType = typeFactory.constructType(type);
        return provider.findValueSerializer(javaType);
    }

    private void writeMap(Map<?, ?> value, JsonGenerator gen) {
        gen.writeStringProperty(TYPE_KEY, classNameMapper().classToCanonicalName(Map.class));

        gen.writeName(VALUE_KEY);
        gen.writeStartObject();
        for (Map.Entry<?, ?> e : value.entrySet()) {
            String k = e.getKey().toString();
            Literal v = Literal.valueOf(e.getValue());
            gen.writePOJOProperty(k, v);
        }
        gen.writeEndObject();
    }

    private void writeArray(Object array, JsonGenerator gen, SerializationContext provider) {
        // e.g. int[], java.lang.String[], etc.
        final String arrayTypeStr = classNameMapper().classToCanonicalName(array.getClass());
        gen.writeStringProperty(TYPE_KEY, arrayTypeStr);

        gen.writeName(VALUE_KEY);
        final int length = Array.getLength(array);
        if (byte[].class.equals(array.getClass())) {
            byte[] data = (byte[]) array;
            Base64Variant base64Variant = provider.getConfig().getBase64Variant();
            gen.writeBinary(base64Variant, data, 0, length);
        } else {
            gen.writeStartArray();
            for (int i = 0; i < length; i++) {
                Object v = Array.get(array, i);
                gen.writePOJO(v);
            }
            gen.writeEndArray();
        }
    }

    protected void writeCollection(Collection<?> collection, JsonGenerator gen, SerializationContext provider) {

        final Class<?> contentType = findContentType(collection, provider);

        final UnaryOperator<Object> valueMapper =
                Literal.class.equals(contentType) ? Literal::valueOf : UnaryOperator.identity();

        gen.writeStringProperty(TYPE_KEY, classNameMapper().classToCanonicalName(collectionType(collection)));

        if (null != contentType) {
            String singleContentTypeValue = classNameMapper.classToCanonicalName(contentType);
            gen.writeStringProperty(COLLECTION_CONTENT_TYPE_KEY, singleContentTypeValue);
        }

        gen.writeName(VALUE_KEY);
        gen.writeStartArray();
        for (Object v : collection) {
            v = valueMapper.apply(v);
            gen.writePOJO(v);
        }
        gen.writeEndArray();
    }

    protected Class<?> findContentType(Collection<?> collection, SerializationContext provider) {
        List<?> types = collection.stream()
                .filter(Objects::nonNull)
                .map(Object::getClass)
                .distinct()
                .toList();

        if (types.isEmpty()) {
            return null; // all null values or empty collection
        }
        if (types.size() == 1) {
            Class<?> type = (Class<?>) types.get(0);
            ValueSerializer<?> valueSerializer = findValueSerializer(provider, type);
            return valueSerializer.handledType();
        }

        return Literal.class;
    }

    protected Class<?> collectionType(Object value) {
        Class<?> collectionType;
        if (value instanceof List) {
            collectionType = List.class;
        } else if (value instanceof Set) {
            collectionType = Set.class;
        } else {
            throw new IllegalArgumentException();
        }
        return collectionType;
    }
}
