/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.jackson.databind.catalog;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import org.geoserver.jackson.databind.catalog.dto.VersionDto;
import org.geotools.util.Version;

import java.io.IOException;

public class VersionDeserializer extends JsonDeserializer<Version> {

    @Override
    public Version deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {

        VersionDto dto = parser.readValueAs(VersionDto.class);
        return VersionSerializer.mapper.dtoToVersion(dto);
    }
}
