/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.jackson.databind.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import org.geoserver.catalog.Info;
import org.geoserver.jackson.databind.config.dto.ConfigInfoDto;
import org.geoserver.jackson.databind.config.dto.mapper.GeoServerConfigMapper;
import org.mapstruct.factory.Mappers;

public class ConfigInfoSerializer<T extends Info> extends StdSerializer<T> {
    private static final long serialVersionUID = -4772839273787523779L;

    protected ConfigInfoSerializer(Class<T> infoType) {
        super(infoType);
    }

    public @Override void serialize(T info, JsonGenerator gen, SerializerProvider provider)
            throws IOException {

        GeoServerConfigMapper mapper = Mappers.getMapper(GeoServerConfigMapper.class);
        ConfigInfoDto dto = mapper.toDto(info);
        gen.writeObject(dto);
    }
}
