/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.jackson.databind.catalog.dto;

import static org.locationtech.jts.geom.Geometry.TYPENAME_GEOMETRYCOLLECTION;
import static org.locationtech.jts.geom.Geometry.TYPENAME_LINESTRING;
import static org.locationtech.jts.geom.Geometry.TYPENAME_MULTILINESTRING;
import static org.locationtech.jts.geom.Geometry.TYPENAME_MULTIPOINT;
import static org.locationtech.jts.geom.Geometry.TYPENAME_MULTIPOLYGON;
import static org.locationtech.jts.geom.Geometry.TYPENAME_POINT;
import static org.locationtech.jts.geom.Geometry.TYPENAME_POLYGON;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.NonNull;
import org.geotools.jdbc.VirtualTable;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;

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
    public static class GeometryTypesSerializer extends ValueSerializer<Map<String, Class<? extends Geometry>>> {
        @Override
        public void serialize(
                Map<String, Class<? extends Geometry>> value, JsonGenerator gen, SerializationContext serializers) {
            if (value == null) {
                gen.writeNull();
                return;
            }

            gen.writeStartObject();
            for (Map.Entry<String, Class<? extends Geometry>> entry : value.entrySet()) {
                String attName = entry.getKey();
                gen.writeName(attName);
                Class<? extends Geometry> geomType = entry.getValue();
                if (geomType == null) {
                    gen.writeNull();
                } else {
                    gen.writeString(geomType.getSimpleName());
                }
            }
            gen.writeEndObject();
        }
    }

    /**
     * Custom deserializer to convert string back to geometry class types
     */
    public static class GeometryTypesDeserializer extends ValueDeserializer<Map<String, Class<? extends Geometry>>> {
        @Override
        @SuppressWarnings("java:S1168") // if stringMap is null we do want to return null instead of empty
        public Map<String, Class<? extends Geometry>> deserialize(JsonParser p, DeserializationContext ctxt) {
            Map<String, String> stringMap = p.readValueAs(new TypeReference<Map<String, String>>() {});
            if (stringMap == null) {
                return null;
            }

            Map<String, Class<? extends Geometry>> result = new HashMap<>();
            for (Map.Entry<String, String> entry : stringMap.entrySet()) {
                String attName = entry.getKey();
                String className = entry.getValue();
                Class<? extends Geometry> geometryClass = null;
                if (className != null) {
                    geometryClass = getGeometryClass(className);
                }
                result.put(attName, geometryClass);
            }
            return result;
        }

        private Class<? extends Geometry> getGeometryClass(@NonNull String className) {
            return switch (className) {
                case TYPENAME_POINT -> Point.class;
                case TYPENAME_LINESTRING -> LineString.class;
                case TYPENAME_POLYGON -> Polygon.class;
                case TYPENAME_MULTIPOINT -> MultiPoint.class;
                case TYPENAME_MULTILINESTRING -> MultiLineString.class;
                case TYPENAME_MULTIPOLYGON -> MultiPolygon.class;
                case TYPENAME_GEOMETRYCOLLECTION -> GeometryCollection.class;
                default -> Geometry.class;
            };
        }
    }
}
