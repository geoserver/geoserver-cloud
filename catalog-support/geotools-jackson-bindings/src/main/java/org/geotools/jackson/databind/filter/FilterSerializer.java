package org.geotools.jackson.databind.filter;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import org.geotools.jackson.databind.filter.mapper.FilterMapper;
import org.mapstruct.factory.Mappers;
import org.opengis.filter.Filter;

public class FilterSerializer extends StdSerializer<Filter> {
    private static final long serialVersionUID = -4772839273787523779L;

    protected FilterSerializer() {
        super(Filter.class);
    }

    public @Override void serialize(Filter filter, JsonGenerator gen, SerializerProvider provider)
            throws IOException {

        FilterMapper mapper = Mappers.getMapper(FilterMapper.class);
        org.geotools.jackson.databind.filter.dto.Filter pojo = mapper.map(filter);
        gen.writeObject(pojo);
    }
}
