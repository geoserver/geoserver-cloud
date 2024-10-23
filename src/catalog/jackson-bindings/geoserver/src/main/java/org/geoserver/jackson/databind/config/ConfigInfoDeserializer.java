/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.jackson.databind.config;

import lombok.NonNull;
import org.geoserver.catalog.Info;
import org.geoserver.jackson.databind.catalog.dto.InfoDto;
import org.geoserver.jackson.databind.config.dto.mapper.GeoServerConfigMapper;
import org.geotools.jackson.databind.util.MapperDeserializer;
import org.mapstruct.factory.Mappers;

public class ConfigInfoDeserializer<T extends Info, D extends InfoDto> extends MapperDeserializer<D, T> {

    private static final GeoServerConfigMapper mapper = Mappers.getMapper(GeoServerConfigMapper.class);

    public ConfigInfoDeserializer(@NonNull Class<D> from) {
        super(from, mapper::toInfo);
    }
}
