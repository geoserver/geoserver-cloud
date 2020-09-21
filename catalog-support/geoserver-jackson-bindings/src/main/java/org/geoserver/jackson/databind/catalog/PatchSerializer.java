/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.jackson.databind.catalog;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.jackson.databind.catalog.dto.PatchDto;
import org.geoserver.jackson.databind.mapper.SharedMappers;
import org.mapstruct.factory.Mappers;

public class PatchSerializer extends StdSerializer<Patch> {
    private static final long serialVersionUID = 1400927583579278680L;

    static final SharedMappers mapper = Mappers.getMapper(SharedMappers.class);

    protected PatchSerializer() {
        super(Patch.class);
    }

    public @Override void serialize(Patch patch, JsonGenerator gen, SerializerProvider provider)
            throws IOException {

        PatchDto dto = mapper.patchToDto(patch);
        gen.writeObject(dto);
    }
}
