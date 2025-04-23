/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geotools.jackson.databind.filter.dto;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.io.IOException;
import java.util.Collection;
import java.util.function.UnaryOperator;
import org.geotools.jackson.databind.util.ObjectMapperUtil;
import org.junit.jupiter.api.BeforeAll;

public class ChangedFieldOrderExpressionSerializationTest extends ExpressionSerializationTest {

    @BeforeAll
    static void setUpMapper() {
        objectMapper = ObjectMapperUtil.newObjectMapper();

        // use the custom serializer from below to ensure that
        // "value" attribute appears before "contentType" attribute
        SimpleModule serializerOverrideModule = new SimpleModule();
        serializerOverrideModule.addSerializer(Literal.class, new ChangedAttributeOrderLiteralSerializer());

        // our custom serializer for this test class will be prioritized as we add our module as latest
        objectMapper.registerModule(serializerOverrideModule);
    }

    /**
     * This is a patched version of the LiteralSerializer that overwrites the writeCollection method
     * to ensure that the "value" field appears before the "contentType" field, which
     * is a real scenario in data originating from postgres JSONB columns.
     *
     */
    @SuppressWarnings("serial")
    private static class ChangedAttributeOrderLiteralSerializer extends LiteralSerializer {
        @Override
        protected void writeCollection(Collection<?> collection, JsonGenerator gen, SerializerProvider provider)
                throws IOException {

            final Class<?> contentType = findContentType(collection, provider);

            final UnaryOperator<Object> valueMapper =
                    Literal.class.equals(contentType) ? Literal::valueOf : UnaryOperator.identity();

            gen.writeStringField(TYPE_KEY, classNameMapper().classToCanonicalName(collectionType(collection)));

            gen.writeFieldName(VALUE_KEY);
            gen.writeStartArray();
            for (Object v : collection) {
                v = valueMapper.apply(v);
                gen.writeObject(v);
            }
            gen.writeEndArray();

            if (null != contentType) {
                String singleContentTypeValue = classNameMapper.classToCanonicalName(contentType);
                gen.writeStringField(COLLECTION_CONTENT_TYPE_KEY, singleContentTypeValue);
            }
        }
    }
}
