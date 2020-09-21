/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.jackson.databind.catalog;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.jackson.databind.catalog.dto.PatchDto;

public class PatchDeserializer extends JsonDeserializer<Patch> {

    public @Override Patch deserialize(JsonParser parser, DeserializationContext ctxt)
            throws IOException, JsonProcessingException {

        PatchDto dto = parser.readValueAs(PatchDto.class);
        Patch patch = PatchSerializer.mapper.dtoToPatch(dto);
        return patch;
    }
}
