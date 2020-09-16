package org.geoserver.jackson.databind.catalog;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.jackson.databind.catalog.dto.CatalogInfoDto;
import org.geoserver.jackson.databind.catalog.mapper.CatalogInfoMapper;
import org.mapstruct.factory.Mappers;

public class CatalogInfoSerializer extends StdSerializer<CatalogInfo> {
    private static final long serialVersionUID = -4772839273787523779L;

    protected CatalogInfoSerializer() {
        super(CatalogInfo.class);
    }

    public @Override void serialize(
            CatalogInfo info, JsonGenerator gen, SerializerProvider provider) throws IOException {

        CatalogInfoMapper mapper = Mappers.getMapper(CatalogInfoMapper.class);
        CatalogInfoDto dto = mapper.map(info);
        gen.writeObject(dto);
    }
}
