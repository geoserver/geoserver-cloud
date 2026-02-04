/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geotools.jackson.databind.geojson.geometry;

import java.io.Serial;
import java.util.concurrent.atomic.AtomicReference;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.CoordinateSequenceFilter;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonToken;
import tools.jackson.core.type.WritableTypeId;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.std.StdSerializer;

public class GeometrySerializer extends StdSerializer<Geometry> {
    @Serial
    private static final long serialVersionUID = 1L;

    public GeometrySerializer() {
        super(Geometry.class);
    }

    @Override
    public void serializeWithType(
            Geometry value, JsonGenerator gen, SerializationContext serializers, TypeSerializer typeSer) {

        WritableTypeId typeIdDef =
                typeSer.writeTypePrefix(gen, serializers, typeSer.typeId(value, JsonToken.START_OBJECT));

        serializeContent(value, gen, null);

        typeSer.writeTypeSuffix(gen, serializers, typeIdDef);
    }

    @Override
    public void serialize(Geometry value, JsonGenerator gen, SerializationContext serializers) {

        serialize(value, gen);
    }

    public void serialize(Geometry geometry, JsonGenerator generator) {
        serialize(geometry, generator, (String) null);
    }

    public void serialize(Geometry geometry, JsonGenerator generator, String customNameProperty) {
        if (geometry == null) {
            generator.writeNull();
            return;
        }
        generator.writeStartObject();
        serializeContent(geometry, generator, customNameProperty);
        generator.writeEndObject();
    }

    private void serializeContent(Geometry geometry, JsonGenerator generator, String customNameProperty) {
        generator.writeStringProperty("type", geometry.getGeometryType());
        writeDimensions(geometry, generator);
        if (customNameProperty != null) {
            generator.writeStringProperty("name", customNameProperty);
        }
        if (geometry instanceof GeometryCollection
                && !(geometry instanceof MultiPoint
                        || geometry instanceof MultiLineString
                        || geometry instanceof MultiPolygon)) {

            generator.writeName("geometries");
            generator.writeStartArray();
            for (int i = 0; i < geometry.getNumGeometries(); i++) {
                serialize(geometry.getGeometryN(i), generator, customNameProperty);
            }
            generator.writeEndArray();
            return;
        }
        generator.writeName("coordinates");
        writeGeometry(geometry, generator);
    }

    /**
     * Custom extension adding {@code dimensions (int)} and {@code measures (boolean)} properties if
     * the geometry has Z and/or M ordinates.
     */
    private void writeDimensions(Geometry geometry, JsonGenerator generator) {
        CoordinateSequence sampleSequence = findSampleSequence(geometry);
        if (sampleSequence != null && sampleSequence.getDimension() > 2) {
            int outputDimension = sampleSequence.getDimension();
            boolean hasM = sampleSequence.hasM();
            generator.writeNumberProperty("dimensions", outputDimension);
            generator.writeBooleanProperty("hasM", hasM);
        }
    }

    private CoordinateSequence findSampleSequence(Geometry g) {
        if (g == null || g.isEmpty()) {
            return null;
        }
        if (g instanceof GeometryCollection col) {
            return findSampleSequence(col.getGeometryN(0));
        }
        if (g instanceof Point point) {
            return point.getCoordinateSequence();
        }
        if (g instanceof LineString line) {
            return line.getCoordinateSequence();
        }
        if (g instanceof Polygon poly) {
            return findSampleSequence(poly.getExteriorRing());
        }
        return null;
    }

    private void writeGeometry(Geometry geometry, JsonGenerator generator) {
        if (geometry instanceof GeometryCollection col) {
            writeMultiGeom(col, generator);
        } else {
            writeSimpleGeom(geometry, generator);
        }
    }

    private void writeMultiGeom(GeometryCollection multi, JsonGenerator generator) {
        generator.writeStartArray();
        for (int i = 0; i < multi.getNumGeometries(); i++) {
            writeGeometry(multi.getGeometryN(i), generator);
        }
        generator.writeEndArray();
    }

    private void writeSimpleGeom(Geometry geometry, JsonGenerator generator) {
        if (geometry.isEmpty()) {
            generator.writeStartArray();
            generator.writeEndArray();
            return;
        }
        if (geometry instanceof Point p) {
            writeCoordinate(p.getCoordinateSequence(), 0, generator);
        } else if (geometry instanceof Polygon poly) {
            generator.writeStartArray();
            writeCoordinateSequence(poly.getExteriorRing(), generator);
            for (int r = 0; r < poly.getNumInteriorRing(); r++) {
                writeCoordinateSequence(poly.getInteriorRingN(r), generator);
            }
            generator.writeEndArray();
        } else {
            writeCoordinateSequence(geometry, generator);
        }
    }

    private void writeCoordinateSequence(Geometry simpleGeom, JsonGenerator generator) {
        final AtomicReference<CoordinateSequence> seqRef = new AtomicReference<>();
        simpleGeom.apply(new CoordinateSequenceFilter() {
            @Override
            public void filter(CoordinateSequence seq, int i) {
                seqRef.set(seq);
            }

            @Override
            public boolean isGeometryChanged() {
                return false;
            }

            @Override
            public boolean isDone() {
                return true;
            }
        });

        CoordinateSequence seq = seqRef.get();
        int size = seq.size();
        generator.writeStartArray();
        for (int i = 0; i < size; i++) {
            writeCoordinate(seq, i, generator);
        }
        generator.writeEndArray();
    }

    private void writeCoordinate(CoordinateSequence seq, int index, JsonGenerator generator) {
        int dimension = seq.getDimension();
        generator.writeStartArray();
        for (int i = 0; i < dimension; i++) {
            generator.writeNumber(seq.getOrdinate(index, i));
        }
        generator.writeEndArray();
    }
}
