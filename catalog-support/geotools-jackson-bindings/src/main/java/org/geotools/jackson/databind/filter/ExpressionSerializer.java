package org.geotools.jackson.databind.filter;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import org.geotools.jackson.databind.filter.mapper.ExpressionMapper;
import org.mapstruct.factory.Mappers;
import org.opengis.filter.expression.Expression;

public class ExpressionSerializer extends StdSerializer<Expression> {
    private static final long serialVersionUID = -4772839273787523779L;

    protected ExpressionSerializer() {
        super(Expression.class);
    }

    public @Override void serialize(
            Expression expression, JsonGenerator gen, SerializerProvider provider)
            throws IOException {

        ExpressionMapper mapper = Mappers.getMapper(ExpressionMapper.class);
        org.geotools.jackson.databind.filter.dto.Expression pojo = mapper.map(expression);
        gen.writeObject(pojo);
    }
}
