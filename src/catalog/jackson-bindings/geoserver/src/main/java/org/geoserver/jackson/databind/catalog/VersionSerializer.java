/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.jackson.databind.catalog;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.WritableTypeId;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import org.geoserver.jackson.databind.catalog.dto.VersionDto;
import org.geoserver.jackson.databind.mapper.SharedMappers;
import org.geotools.util.Version;
import org.mapstruct.factory.Mappers;

import java.io.IOException;

public class VersionSerializer extends StdSerializer<Version> {
    private static final long serialVersionUID = 1400927583579278680L;

    static final SharedMappers mapper = Mappers.getMapper(SharedMappers.class);

    protected VersionSerializer() {
        super(Version.class);
    }

    @Override
    public void serializeWithType(
            Version value,
            JsonGenerator gen,
            SerializerProvider serializers,
            TypeSerializer typeSer)
            throws IOException {

        WritableTypeId typeIdDef =
                typeSer.writeTypePrefix(gen, typeSer.typeId(value, JsonToken.VALUE_STRING));

        serialize(value, gen, null);

        typeSer.writeTypeSuffix(gen, typeIdDef);
    }

    @Override
    public void serialize(Version version, JsonGenerator gen, SerializerProvider provider)
            throws IOException {

        VersionDto dto = mapper.versionToDto(version);
        gen.writeObject(dto);
    }
}
