/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.jackson.databind.catalog;

import org.geoserver.catalog.CatalogInfo;
import org.geoserver.jackson.databind.catalog.dto.CatalogInfoDto;
import org.geoserver.jackson.databind.catalog.mapper.CatalogInfoMapper;
import org.mapstruct.factory.Mappers;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

public class CatalogInfoDeserializer<I extends CatalogInfo> extends ValueDeserializer<I> {

    private static final CatalogInfoMapper mapper = Mappers.getMapper(CatalogInfoMapper.class);

    @Override
    public I deserialize(JsonParser parser, DeserializationContext ctxt) {

        CatalogInfoDto dto = parser.readValueAs(CatalogInfoDto.class);
        return mapper.map(dto);
    }
}
