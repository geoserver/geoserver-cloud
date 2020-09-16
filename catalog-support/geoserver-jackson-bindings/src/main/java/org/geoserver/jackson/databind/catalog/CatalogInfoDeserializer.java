package org.geoserver.jackson.databind.catalog;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.jackson.databind.catalog.dto.CatalogInfoDto;
import org.geoserver.jackson.databind.catalog.mapper.CatalogInfoMapper;
import org.mapstruct.factory.Mappers;

public class CatalogInfoDeserializer extends JsonDeserializer<CatalogInfo> {

    public @Override CatalogInfo deserialize(JsonParser parser, DeserializationContext ctxt)
            throws IOException, JsonProcessingException {

        CatalogInfoDto dto = parser.readValueAs(CatalogInfoDto.class);
        CatalogInfoMapper mapper = Mappers.getMapper(CatalogInfoMapper.class);
        CatalogInfo info = mapper.map(dto);
        return info;
    }
}
