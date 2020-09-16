package org.geotools.jackson.databind.filter.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.util.StdConverter;
import java.util.LinkedHashMap;
import org.geotools.jackson.databind.geojson.geometry.GeometryDeserializer;
import org.locationtech.jts.geom.GeometryFactory;

public class LiteralValueConverter extends StdConverter<Object, Object> {

    @Override
    public Object convert(Object value) {
        if (value instanceof LinkedHashMap) {
            // this is a HACK, type resolution should be handled automatically but I can't figure
            // out how
            @SuppressWarnings("unchecked")
            LinkedHashMap<String, Object> map = ((LinkedHashMap<String, Object>) value);
            String typeAttr = map.containsKey("type") ? String.valueOf(map.get("type")) : null;
            if (GeometryDeserializer.isGeometry(typeAttr)) {
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode node = objectMapper.valueToTree(map);
                GeometryDeserializer deserializer = new GeometryDeserializer(new GeometryFactory());
                return deserializer.readGeometry((ObjectNode) node);
            }
        }
        return value;
    }
}
