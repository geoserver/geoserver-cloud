/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.jackson.databind.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.geoserver.catalog.Info;
import org.geoserver.jackson.databind.config.dto.ConfigInfoDto;
import org.geoserver.jackson.databind.config.dto.mapper.GeoServerConfigMapper;
import org.mapstruct.factory.Mappers;

@RequiredArgsConstructor
public class ConfigInfoDeserializer<T extends Info, D extends ConfigInfoDto>
        extends JsonDeserializer<T> {

    private final Class<D> dtoType;

    public @Override T deserialize(JsonParser parser, DeserializationContext ctxt)
            throws IOException, JsonProcessingException {

        D dto = parser.readValueAs(dtoType);
        GeoServerConfigMapper mapper = Mappers.getMapper(GeoServerConfigMapper.class);
        T info = mapper.toInfo(dto);
        return info;
    }
}
