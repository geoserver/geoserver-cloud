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
import java.io.IOException;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.jackson.databind.catalog.dto.CatalogInfoDto;
import org.geoserver.jackson.databind.catalog.mapper.CatalogInfoMapper;
import org.mapstruct.factory.Mappers;

public class CatalogInfoSerializer<I extends CatalogInfo> extends StdSerializer<I> {
    private static final long serialVersionUID = -4772839273787523779L;

    private static final CatalogInfoMapper mapper = Mappers.getMapper(CatalogInfoMapper.class);

    private Class<I> infoType;

    protected CatalogInfoSerializer(Class<I> infoType) {
        super(infoType);
        this.infoType = infoType;
    }

    public @Override void serialize(
            CatalogInfo info, JsonGenerator gen, SerializerProvider provider) throws IOException {

        CatalogInfoDto dto = mapper.map(info);
        gen.writeObject(dto);
    }

    public @Override void serializeWithType(
            I value, JsonGenerator gen, SerializerProvider serializers, TypeSerializer typeSer)
            throws IOException {

        WritableTypeId typeIdDef =
                typeSer.writeTypePrefix(
                        gen, typeSer.typeId(value, infoType, JsonToken.VALUE_STRING));

        serialize(value, gen, null);

        typeSer.writeTypeSuffix(gen, typeIdDef);
    }
}
