/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.jackson.databind.catalog.dto;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import org.geoserver.jackson.databind.catalog.dto.VirtualTableDto.GeometryTypesDeserializer;
import org.geoserver.jackson.databind.catalog.dto.VirtualTableDto.GeometryTypesSerializer;
import org.geotools.jdbc.VirtualTable;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

/** DTO type for {@link VirtualTable} */
@Data
public class VirtualTableDto {

    private String name;
    private String sql;
    private boolean escapeSql;
    private List<String> primaryKeyColumns;

    @JsonSerialize(using = GeometryTypesSerializer.class)
    @JsonDeserialize(using = GeometryTypesDeserializer.class)
    private Map<String, Class<? extends Geometry>> geometryTypes;

    private Map<String, Integer> nativeSrids;
    private Map<String, Integer> dimensions;
    private Map<String, VirtualTableParameterDto> parameters;

    /**
     * Custom serializer to convert geometry class types to string representation
     */
    public static class GeometryTypesSerializer extends JsonSerializer<Map<String, Class<? extends Geometry>>> {
        @Override
        public void serialize(
                Map<String, Class<? extends Geometry>> value, JsonGenerator gen, SerializerProvider serializers)
                throws IOException {
            if (value == null) {
                gen.writeNull();
                return;
            }

            gen.writeStartObject();
            for (Map.Entry<String, Class<? extends Geometry>> entry : value.entrySet()) {
                gen.writeFieldName(entry.getKey());
                if (entry.getValue() != null) {
                    gen.writeString(entry.getValue().getSimpleName());
                } else {
                    gen.writeNull();
                }
            }
            gen.writeEndObject();
        }
    }

    /**
     * Custom deserializer to convert string back to geometry class types
     */
    public static class GeometryTypesDeserializer extends JsonDeserializer<Map<String, Class<? extends Geometry>>> {
        @Override
        public Map<String, Class<? extends Geometry>> deserialize(JsonParser p, DeserializationContext ctxt)
                throws IOException {
            Map<String, String> stringMap = p.readValueAs(new TypeReference<Map<String, String>>() {});
            if (stringMap == null) {
                return null;
            }

            Map<String, Class<? extends Geometry>> result = new HashMap<>();
            for (Map.Entry<String, String> entry : stringMap.entrySet()) {
                String className = entry.getValue();
                if (className != null) {
                    Class<? extends Geometry> geometryClass = getGeometryClass(className);
                    result.put(entry.getKey(), geometryClass);
                } else {
                    result.put(entry.getKey(), null);
                }
            }
            return result;
        }

        private Class<? extends Geometry> getGeometryClass(String className) {
            switch (className) {
                case "Point":
                    return Point.class;
                case "LineString":
                    return LineString.class;
                case "Polygon":
                    return Polygon.class;
                case "MultiPoint":
                    return MultiPoint.class;
                case "MultiLineString":
                    return MultiLineString.class;
                case "MultiPolygon":
                    return MultiPolygon.class;
                case "GeometryCollection":
                    return GeometryCollection.class;
                case "Geometry":
                default:
                    return Geometry.class;
            }
        }
    }
}
