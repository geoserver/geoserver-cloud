/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geotools.jackson.databind.geojson;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.CoordinateSequenceFactory;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.impl.PackedCoordinateSequenceFactory;
import org.locationtech.jts.io.Ordinate;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.WKTWriter;

import java.util.EnumSet;

/**
 * Test suite for {@link GeoToolsGeoJsonModule}, assuming it's registered to an {@link ObjectMapper}
 */
public class GeoToolsGeoJsonModuleTest {

    private ObjectMapper objectMapper;

    public @Before void before() {
        objectMapper = new ObjectMapper();
        objectMapper.setDefaultPropertyInclusion(Include.NON_EMPTY);
        objectMapper.findAndRegisterModules();
    }

    public @Test void testEmptyGeometries() throws JsonProcessingException {
        roundtripTest("POINT EMPTY");
        roundtripTest("LINESTRING EMPTY");
        roundtripTest("POLYGON EMPTY");
        roundtripTest("MULTIPOINT EMPTY");
        roundtripTest("MULTILINESTRING EMPTY");
        roundtripTest("MULTIPOLYGON EMPTY");
        roundtripTest("GEOMETRYCOLLECTION EMPTY");
    }

    public @Test void testPoint() throws JsonProcessingException {
        roundtripTest("POINT(0 1)");
        roundtripTest("POINT Z(0 1 2)");
        roundtripTest("POINT M(0 1 3)");
        roundtripTest("POINT ZM(0 1 2 3)");
    }

    public @Test void testMultiPoint() throws JsonProcessingException {
        roundtripTest("MULTIPOINT(0 1, -1 -2)");
        roundtripTest("MULTIPOINT Z(0 1 2, -1 -2 -3)");
        roundtripTest("MULTIPOINT M(0 1 3, -1 -2 -4)");
        roundtripTest("MULTIPOINT ZM(0 1 2 3, -1 -2 -3 -4)");
    }

    public @Test void testLineString() throws JsonProcessingException {
        roundtripTest("LINESTRING(0 1, 4 5)");
        roundtripTest("LINESTRING Z(0 1 2, 4 5 6)");
        roundtripTest("LINESTRING M(0 1 3, 4 5 7)");
        roundtripTest("LINESTRING ZM(0 1 2 3, 4 5 6 7)");
    }

    public @Test void testMultiLineString() throws JsonProcessingException {
        roundtripTest("MULTILINESTRING((0 1, 4 5), (-1 -2, -5 -6))");
        roundtripTest("MULTILINESTRING Z((0 1 2, 4 5 6), (-1 -2 -3, -5 -6 -7))");
        roundtripTest("MULTILINESTRING M((0 1 3, 4 5 7), (-1 -2 -4, -5 -6 -8))");
        roundtripTest("MULTILINESTRING ZM((0 1 2 3, 4 5 6 7), (-1 -2 -3 -4, -5 -6 -7 -8))");
    }

    public @Test void testPolygon() throws JsonProcessingException {
        roundtripTest("POLYGON   ((0 0, 10 10, 20 0, 0 0),(1 1, 9 9, 19 1, 1 1))");
        roundtripTest("POLYGON  Z((0 0 0, 10 10 1, 20 0 2, 0 0 0),(1 1 1, 9 9 2, 19 1 3, 1 1 1))");
        roundtripTest("POLYGON  M((0 0 0, 10 10 1, 20 0 2, 0 0 0),(1 1 1, 9 9 2, 19 1 3, 1 1 1))");
        roundtripTest(
                "POLYGON ZM((0 0 0 0, 10 10 1 1, 20 0 2 2, 0 0 0 0),(1 1 1 1, 9 9 2 2, 19 1 3 3, 1 1 1 1))");
    }

    public @Test void testMultiPolygon() throws JsonProcessingException {
        roundtripTest("MULTIPOLYGON   (((0 0, 10 10, 20 0, 0 0)), ((1 1, 9 9, 19 1, 1 1)))");
        roundtripTest(
                "MULTIPOLYGON  Z(((0 0 0, 10 10 1, 20 0 2, 0 0 0)), ((1 1 1, 9 9 2, 19 1 3, 1 1 1)))");
        roundtripTest(
                "MULTIPOLYGON  M(((0 0 0, 10 10 1, 20 0 2, 0 0 0)), ((1 1 1, 9 9 2, 19 1 3, 1 1 1)))");
        roundtripTest(
                "MULTIPOLYGON ZM(((0 0 0 0, 10 10 1 1, 20 0 2 2, 0 0 0 0)), ((1 1 1 1, 9 9 2 2, 19 1 3 3, 1 1 1 1)))");
    }

    public @Test void testGeometryCollection() throws JsonProcessingException {
        roundtripTest(
                "GEOMETRYCOLLECTION(POINT EMPTY,"
                        + "POLYGON ((0 0, 10 10, 20 0, 0 0),(1 1, 9 9, 19 1, 1 1)),"
                        + "LINESTRING(0 1, 4 5))");
        roundtripTest(
                "GEOMETRYCOLLECTION("
                        + "POLYGON Z((0 0 0, 10 10 1, 20 0 2, 0 0 0),(1 1 1, 9 9 2, 19 1 3, 1 1 1)),"
                        + "LINESTRING Z(0 1 2, 4 5 6))");
        roundtripTest(
                "GEOMETRYCOLLECTION("
                        + "POLYGON  M((0 0 0, 10 10 1, 20 0 2, 0 0 0),(1 1 1, 9 9 2, 19 1 3, 1 1 1)),"
                        + "LINESTRING M(0 1 3, 4 5 7))");
        roundtripTest(
                "GEOMETRYCOLLECTION("
                        + "POLYGON ZM((0 0 0 0, 10 10 1 1, 20 0 2 2, 0 0 0 0),(1 1 1 1, 9 9 2 2, 19 1 3 3, 1 1 1 1)),"
                        + "LINESTRING ZM(0 1 2 3, 4 5 6 7))");
    }

    private Geometry roundtripTest(String wkt) throws JsonProcessingException {
        return roundtripTest(geom(wkt));
    }

    private Geometry roundtripTest(Geometry orig) throws JsonProcessingException {
        String preWkt = toWKT(orig);
        String serialized = objectMapper.writeValueAsString(orig);
        System.err.println(serialized);
        Geometry deserialized = objectMapper.readValue(serialized, Geometry.class);
        String postWkt = toWKT(deserialized);
        System.err.printf(" orig: %s%n read: %s%n%n", preWkt, postWkt);
        assertActuallyEqualsExact(orig, deserialized);
        return deserialized;
    }

    /**
     * There's no way I could find in JTS to check for actual full geometry equality including all
     * dimensions, despite {@link Geometry#equalsExact(Geometry)}
     */
    private void assertActuallyEqualsExact(Geometry g1, Geometry g2) {
        assertTrue(g1.equalsExact(g2));
        Coordinate[] cs1 = g1.getCoordinates();
        Coordinate[] cs2 = g2.getCoordinates();
        assertEquals(cs1.length, cs2.length);
        for (int i = 0; i < cs1.length; i++) {
            Coordinate c1 = cs1[i];
            Coordinate c2 = cs2[i];
            assertTrue(String.format("expected %s, got %s", c1, c2), c1.equals3D(c2));
            assertEquals(c1.getM(), c2.getM(), 1e-9d);
        }
    }

    private String toWKT(Geometry orig) {
        if (orig == null) return null;
        EnumSet<Ordinate> outputOrdinates = Ordinate.createXY();
        CoordinateSequence seq = findCoordSeq(orig);
        int outputDimension = seq == null ? 2 : seq.getDimension();
        boolean hasZ = seq == null ? false : seq.hasZ();
        boolean hasM = seq == null ? false : seq.hasM();
        if (hasZ) {
            outputOrdinates.add(Ordinate.Z);
        }
        if (hasM) {
            outputOrdinates.add(Ordinate.M);
        }
        WKTWriter writer = new WKTWriter(outputDimension);
        writer.setOutputOrdinates(outputOrdinates);
        return writer.write(orig);
    }

    private CoordinateSequence findCoordSeq(Geometry g) {
        if (g == null || g.isEmpty()) return null;
        if (g instanceof GeometryCollection) {
            return findCoordSeq(g.getGeometryN(0));
        }
        if (g instanceof Point) return ((Point) g).getCoordinateSequence();
        if (g instanceof LineString) return ((LineString) g).getCoordinateSequence();
        if (g instanceof Polygon) return findCoordSeq(((Polygon) g).getExteriorRing());
        return null;
    }

    private Geometry geom(String wkt) {
        try {
            CoordinateSequenceFactory csf = new PackedCoordinateSequenceFactory();
            GeometryFactory gf = new GeometryFactory(csf);
            WKTReader reader = new WKTReader(gf);
            // do not create 3-dim coords if only 2 are requested
            reader.setIsOldJtsCoordinateSyntaxAllowed(false);
            reader.setIsOldJtsMultiPointSyntaxAllowed(true);
            return reader.read(wkt);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }
}
