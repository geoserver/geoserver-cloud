/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geotools.jackson.databind.filter;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import org.geotools.jackson.databind.filter.mapper.ExpressionMapper;
import org.mapstruct.factory.Mappers;
import org.opengis.filter.expression.Expression;

public class ExpressionDeserializer extends JsonDeserializer<Expression> {

    public @Override Expression deserialize(JsonParser parser, DeserializationContext ctxt)
            throws IOException, JsonProcessingException {

        org.geotools.jackson.databind.filter.dto.Expression pojo;
        pojo = parser.readValueAs(org.geotools.jackson.databind.filter.dto.Expression.class);

        ExpressionMapper mapper = Mappers.getMapper(ExpressionMapper.class);
        Expression expression = mapper.map(pojo);
        return expression;
    }
}
