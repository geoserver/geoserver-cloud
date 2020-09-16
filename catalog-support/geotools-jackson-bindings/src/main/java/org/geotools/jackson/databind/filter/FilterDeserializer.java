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
import org.geotools.jackson.databind.filter.mapper.FilterMapper;
import org.mapstruct.factory.Mappers;
import org.opengis.filter.Filter;

public class FilterDeserializer extends JsonDeserializer<Filter> {

    public @Override Filter deserialize(JsonParser parser, DeserializationContext ctxt)
            throws IOException, JsonProcessingException {

        org.geotools.jackson.databind.filter.dto.Filter pojo;
        pojo = parser.readValueAs(org.geotools.jackson.databind.filter.dto.Filter.class);

        FilterMapper mapper = Mappers.getMapper(FilterMapper.class);
        org.opengis.filter.Filter filter = mapper.map(pojo);
        return filter;
    }
}
