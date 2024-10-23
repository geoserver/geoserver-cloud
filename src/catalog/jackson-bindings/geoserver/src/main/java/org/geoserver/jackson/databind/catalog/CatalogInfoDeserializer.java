/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.jackson.databind.catalog;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.jackson.databind.catalog.dto.CatalogInfoDto;
import org.geoserver.jackson.databind.catalog.mapper.CatalogInfoMapper;
import org.mapstruct.factory.Mappers;

public class CatalogInfoDeserializer<I extends CatalogInfo> extends JsonDeserializer<I> {

    private static final CatalogInfoMapper mapper = Mappers.getMapper(CatalogInfoMapper.class);

    @Override
    public I deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {

        CatalogInfoDto dto = parser.readValueAs(CatalogInfoDto.class);
        return mapper.map(dto);
    }
}
