/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geotools.jackson.databind.geojson.geometry;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.NumericNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.CoordinateSequenceFactory;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.impl.PackedCoordinateSequenceFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;

public class GeometryDeserializer<T extends Geometry> extends JsonDeserializer<T> {

    private static final String COORDINATES_PROPERTY = "coordinates";

    private static final GeometryFactory DEFAULT_GF =
            new GeometryFactory(new PackedCoordinateSequenceFactory());

    private GeometryFactory geometryFactory;

    public GeometryDeserializer() {
        this(DEFAULT_GF);
    }

    public GeometryDeserializer(GeometryFactory geometryFactory) {
        this.geometryFactory = geometryFactory;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {

        return (T) readGeometry(p.readValueAsTree());
    }

    public Geometry readGeometry(ObjectNode geometryNode) {
        final int dimensions = getDimensions(geometryNode);
        final boolean hasM = resolveHasM(geometryNode);
        return readGeometry(geometryNode, dimensions, hasM);
    }

    private Geometry readGeometry(ObjectNode geometryNode, int dimensions, boolean hasM) {
        final String type = geometryNode.findValue("type").asText();
        switch (type) {
            case Geometry.TYPENAME_POINT:
                return readPoint(geometryNode, dimensions, hasM);
            case Geometry.TYPENAME_MULTIPOINT:
                return readMultiPoint(geometryNode, dimensions, hasM);
            case Geometry.TYPENAME_LINESTRING:
                return readLineString(geometryNode, dimensions, hasM);
            case Geometry.TYPENAME_MULTILINESTRING:
                return readMultiLineString(geometryNode, dimensions, hasM);
            case Geometry.TYPENAME_POLYGON:
                return readPolygon(geometryNode, dimensions, hasM);
            case Geometry.TYPENAME_MULTIPOLYGON:
                return readMultiPolygon(geometryNode, dimensions, hasM);
            case Geometry.TYPENAME_GEOMETRYCOLLECTION:
                return readGeometryCollection(geometryNode, dimensions, hasM);
            default:
                throw new IllegalArgumentException(
                        "Unknown geometry node: %s".formatted(geometryNode));
        }
    }

    private int getDimensions(ObjectNode geometryNode) {
        JsonNode dimensionsProperty = geometryNode.findValue("dimensions");
        if (dimensionsProperty instanceof NumericNode numericNode) {
            return numericNode.asInt();
        }
        return 2;
    }

    private boolean resolveHasM(ObjectNode geometryNode) {
        JsonNode hasMProperty = geometryNode.findValue("hasM");
        if (hasMProperty instanceof BooleanNode booleanNode) {
            return booleanNode.asBoolean();
        }
        return false;
    }

    private MultiLineString readMultiLineString(
            ObjectNode geometryNode, int dimensions, boolean hasM) {
        ArrayNode coordinates = (ArrayNode) geometryNode.findValue(COORDINATES_PROPERTY);
        if (coordinates.isEmpty()) {
            return geometryFactory.createMultiLineString();
        }

        LineString[] lineStrings =
                IntStream.range(0, coordinates.size())
                        .mapToObj(i -> (ArrayNode) coordinates.get(i))
                        .map(geomN -> readCoordinateSequence(geomN, dimensions, hasM))
                        .map(geometryFactory::createLineString)
                        .toArray(LineString[]::new);

        return geometryFactory.createMultiLineString(lineStrings);
    }

    private MultiPolygon readMultiPolygon(ObjectNode geometryNode, int dimensions, boolean hasM) {
        ArrayNode coordinates = (ArrayNode) geometryNode.findValue(COORDINATES_PROPERTY);
        if (coordinates.isEmpty()) {
            return geometryFactory.createMultiPolygon();
        }
        Polygon[] polygons =
                IntStream.range(0, coordinates.size())
                        .mapToObj(i -> (ArrayNode) coordinates.get(i))
                        .map(array -> readPolygon(array, dimensions, hasM))
                        .toArray(Polygon[]::new);
        return geometryFactory.createMultiPolygon(polygons);
    }

    private GeometryCollection readGeometryCollection(
            ObjectNode geometryNode, int dimensions, boolean hasM) {
        ArrayNode geometries = (ArrayNode) geometryNode.findValue("geometries");
        if (geometries.isEmpty()) {
            return geometryFactory.createGeometryCollection();
        }
        Geometry[] subGeometries = new Geometry[geometries.size()];
        for (int i = 0; i < geometries.size(); i++) {
            ObjectNode geomNode = (ObjectNode) geometries.get(i);
            subGeometries[i] = readGeometry(geomNode, dimensions, hasM);
        }
        return geometryFactory.createGeometryCollection(subGeometries);
    }

    private Polygon readPolygon(ObjectNode geometryNode, int dimensions, boolean hasM) {
        ArrayNode coordinates = (ArrayNode) geometryNode.findValue(COORDINATES_PROPERTY);
        return readPolygon(coordinates, dimensions, hasM);
    }

    private Polygon readPolygon(ArrayNode coordinates, int dimensions, boolean hasM) {
        if (null == coordinates || coordinates.isEmpty()) {
            return geometryFactory.createPolygon();
        }
        LinearRing shell = readLinearRing((ArrayNode) coordinates.get(0), dimensions, hasM);
        final LinearRing[] holes;
        if (coordinates.size() > 1) {
            holes = new LinearRing[coordinates.size() - 1];
            IntStream.range(1, coordinates.size())
                    .forEach(
                            i ->
                                    holes[i - 1] =
                                            readLinearRing(
                                                    (ArrayNode) coordinates.get(i),
                                                    dimensions,
                                                    hasM));
        } else {
            holes = null;
        }
        return geometryFactory.createPolygon(shell, holes);
    }

    private LinearRing readLinearRing(ArrayNode coordinates, int dimensions, boolean hasM) {
        if (coordinates.isEmpty()) {
            return geometryFactory.createLinearRing();
        }
        CoordinateSequence coords = readCoordinateSequence(coordinates, dimensions, hasM);
        return geometryFactory.createLinearRing(coords);
    }

    private LineString readLineString(ObjectNode geometryNode, int dimensions, boolean hasM) {
        ArrayNode coordinates = (ArrayNode) geometryNode.findValue(COORDINATES_PROPERTY);
        if (coordinates.isEmpty()) {
            return geometryFactory.createLineString();
        }
        CoordinateSequence coords = readCoordinateSequence(coordinates, dimensions, hasM);
        return geometryFactory.createLineString(coords);
    }

    private MultiPoint readMultiPoint(ObjectNode geometryNode, int dimensions, boolean hasM) {
        ArrayNode coordinates = (ArrayNode) geometryNode.findValue(COORDINATES_PROPERTY);
        if (null == coordinates || coordinates.isEmpty()) {
            return geometryFactory.createMultiPoint();
        }
        CoordinateSequence coords = readCoordinateSequence(coordinates, dimensions, hasM);
        return geometryFactory.createMultiPoint(coords);
    }

    private CoordinateSequence readCoordinateSequence(
            ArrayNode coordinates, int dimension, boolean hasM) {
        final int size = coordinates.size();
        final int measures = hasM ? 1 : 0;
        CoordinateSequenceFactory sequenceFactory = geometryFactory.getCoordinateSequenceFactory();
        CoordinateSequence sequence = sequenceFactory.create(size, dimension, measures);
        for (int coord = 0; coord < size; coord++) {
            ArrayNode coordNode = (ArrayNode) coordinates.get(coord);
            for (int d = 0; d < dimension; d++) {
                sequence.setOrdinate(coord, d, coordNode.get(d).asDouble());
            }
        }
        return sequence;
    }

    private Point readPoint(ObjectNode geometryNode, int dimensions, boolean hasM) {
        ArrayNode coordinateArray = (ArrayNode) geometryNode.findValue(COORDINATES_PROPERTY);
        if (null == coordinateArray || coordinateArray.isEmpty()) {
            return geometryFactory.createPoint();
        }
        CoordinateSequence coordinate =
                geometryFactory.getCoordinateSequenceFactory().create(1, dimensions, hasM ? 1 : 0);
        for (int d = 0; d < dimensions; d++)
            coordinate.setOrdinate(0, d, coordinateArray.get(d).asDouble());
        return geometryFactory.createPoint(coordinate);
    }

    public static boolean isGeometry(JsonNode value) {
        if (!(value instanceof ObjectNode)) {
            return false;
        }
        JsonNode typeNode = value.get("type");
        return typeNode instanceof TextNode textNode && isGeometry(textNode.asText());
    }

    private static final Set<String> geomTypes =
            new HashSet<>(
                    Arrays.asList( //
                            "Point",
                            "MultiPoint",
                            "LineString",
                            "MultiLineString",
                            "Polygon",
                            "MultiPolygon",
                            "GeometryCollection"));

    public static boolean isGeometry(String type) {
        return geomTypes.contains(type);
    }
}
