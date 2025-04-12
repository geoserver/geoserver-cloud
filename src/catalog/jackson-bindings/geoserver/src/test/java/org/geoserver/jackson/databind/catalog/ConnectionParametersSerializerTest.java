/*
 * (c) 2025 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.jackson.databind.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.jackson.databind.filter.GeoToolsFilterModule;
import org.geotools.jackson.databind.filter.dto.Literal;
import org.geotools.jackson.databind.geojson.GeoToolsGeoJsonModule;
import org.geotools.referencing.CRS;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ConnectionParameters} serialization and deserialization.
 */
class ConnectionParametersSerializerTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new GeoServerCatalogModule());
        objectMapper.registerModule(new GeoToolsGeoJsonModule());
        objectMapper.registerModule(new GeoToolsFilterModule());
    }

    @Test
    void testSimpleTypesNotWrapped() throws IOException {
        ConnectionParameters params = new ConnectionParameters();
        params.put("string", "test");
        params.put("integer", 123);
        params.put("double", 123.456);
        params.put("boolean", true);
        params.put("null", null);

        // Serialize
        String json = objectMapper.writeValueAsString(params);

        // Deserialize
        ConnectionParameters result = objectMapper.readValue(json, ConnectionParameters.class);

        // Verify
        assertEquals(5, result.size());
        assertEquals("test", result.get("string"));
        assertEquals(123, result.get("integer"));
        assertEquals(123.456, result.get("double"));
        assertEquals(true, result.get("boolean"));
        assertEquals(null, result.get("null"));
    }

    @Test
    void testReferencedEnvelopeWrappedInLiteral() throws Exception {
        // Create a ReferencedEnvelope
        ReferencedEnvelope envelope = new ReferencedEnvelope(-180, 180, -90, 90, CRS.decode("EPSG:4326", true));

        // Create test params
        ConnectionParameters params = new ConnectionParameters();
        params.put("bbox", envelope);
        params.put("name", "testEnvelope");

        // Serialize
        String json = objectMapper.writeValueAsString(params);

        // Verify JSON contains Literal structure
        assertTrue(json.contains("Literal"));

        // Deserialize
        ConnectionParameters result = objectMapper.readValue(json, ConnectionParameters.class);

        // Verify
        assertEquals(2, result.size());
        assertEquals("testEnvelope", result.get("name"));

        Object bbox = result.get("bbox");
        assertNotNull(bbox);
        assertInstanceOf(ReferencedEnvelope.class, bbox);

        ReferencedEnvelope resultEnvelope = (ReferencedEnvelope) bbox;
        assertEquals(envelope.getMinX(), resultEnvelope.getMinX(), 1e-8);
        assertEquals(envelope.getMaxX(), resultEnvelope.getMaxX(), 1e-8);
        assertEquals(envelope.getMinY(), resultEnvelope.getMinY(), 1e-8);
        assertEquals(envelope.getMaxY(), resultEnvelope.getMaxY(), 1e-8);
        assertTrue(CRS.equalsIgnoreMetadata(
                envelope.getCoordinateReferenceSystem(), resultEnvelope.getCoordinateReferenceSystem()));
    }

    @Test
    void testMixedSimpleAndComplexTypes() throws Exception {
        // Create a ReferencedEnvelope
        ReferencedEnvelope envelope = new ReferencedEnvelope(-180, 180, -90, 90, CRS.decode("EPSG:4326", true));

        // Create test params with mix of simple and complex types
        ConnectionParameters params = new ConnectionParameters();
        params.put("bbox", envelope);
        params.put("name", "testMixed");
        params.put("count", 42);
        params.put("enabled", true);

        // Serialize
        String json = objectMapper.writeValueAsString(params);

        // Deserialize
        ConnectionParameters result = objectMapper.readValue(json, ConnectionParameters.class);

        // Verify
        assertEquals(4, result.size());
        assertEquals("testMixed", result.get("name"));
        assertEquals(42, result.get("count"));
        assertEquals(true, result.get("enabled"));

        Object bbox = result.get("bbox");
        assertNotNull(bbox);
        assertInstanceOf(ReferencedEnvelope.class, bbox);

        ReferencedEnvelope resultEnvelope = (ReferencedEnvelope) bbox;
        assertEquals(envelope.getMinX(), resultEnvelope.getMinX(), 1e-8);
        assertEquals(envelope.getMaxX(), resultEnvelope.getMaxX(), 1e-8);
        assertEquals(envelope.getMinY(), resultEnvelope.getMinY(), 1e-8);
        assertEquals(envelope.getMaxY(), resultEnvelope.getMaxY(), 1e-8);
    }

    @Test
    void testAlreadyWrappedLiteral() throws Exception {
        // Create a ReferencedEnvelope already wrapped in a Literal
        ReferencedEnvelope envelope = new ReferencedEnvelope(-180, 180, -90, 90, CRS.decode("EPSG:4326", true));
        Literal literal = Literal.valueOf(envelope);

        // Create test params
        ConnectionParameters params = new ConnectionParameters();
        params.put("bbox", literal);
        params.put("name", "testLiteral");

        // Serialize
        String json = objectMapper.writeValueAsString(params);

        // Deserialize
        ConnectionParameters result = objectMapper.readValue(json, ConnectionParameters.class);

        // Verify
        assertEquals(2, result.size());
        assertEquals("testLiteral", result.get("name"));

        Object bbox = result.get("bbox");
        assertNotNull(bbox);
        assertInstanceOf(ReferencedEnvelope.class, bbox);

        ReferencedEnvelope resultEnvelope = (ReferencedEnvelope) bbox;
        assertEquals(envelope.getMinX(), resultEnvelope.getMinX(), 1e-8);
        assertEquals(envelope.getMaxX(), resultEnvelope.getMaxX(), 1e-8);
        assertEquals(envelope.getMinY(), resultEnvelope.getMinY(), 1e-8);
        assertEquals(envelope.getMaxY(), resultEnvelope.getMaxY(), 1e-8);
    }

    @Test
    void testMapConversion() {
        // Create a ReferencedEnvelope
        ReferencedEnvelope envelope = null;
        try {
            envelope = new ReferencedEnvelope(-180, 180, -90, 90, CRS.decode("EPSG:4326", true));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Test the conversion methods
        ConnectionParameters params = new ConnectionParameters();
        params.put("bbox", envelope);
        params.put("name", "testConversion");
        params.put("count", 42);

        // Convert to serializable map
        var serializableMap = params.toSerializableMap();

        // Convert back
        ConnectionParameters roundTrip = ConnectionParameters.fromSerializableMap(serializableMap);

        // Verify
        assertEquals(3, roundTrip.size());
        assertEquals("testConversion", roundTrip.get("name"));
        assertEquals(42, roundTrip.get("count"));

        Object bbox = roundTrip.get("bbox");
        assertNotNull(bbox);
        assertInstanceOf(ReferencedEnvelope.class, bbox);
    }

    @Test
    void testPathUriUrlFileAsStrings() throws Exception {
        // Create test parameters with various path-related types
        ConnectionParameters params = new ConnectionParameters();

        // Add different path types
        File file = new File("/tmp/testfile.txt");
        URI uri = new URI("http://example.com/data");
        URL url = new URL("http://example.com/data.json");
        Path path = Paths.get("/tmp/testpath");

        params.put("file", file);
        params.put("uri", uri);
        params.put("url", url);
        params.put("path", path);

        // Also add a primitive type for comparison
        params.put("name", "testPaths");

        // Serialize
        String json = objectMapper.writeValueAsString(params);

        // Verify values are serialized as strings, not wrapped in Literal
        assertTrue(json.contains("\"file\":\"/tmp/testfile.txt\""));
        assertTrue(json.contains("\"uri\":\"http://example.com/data\""));
        assertTrue(json.contains("\"url\":\"http://example.com/data.json\""));
        assertTrue(json.contains("\"path\":\"/tmp/testpath\""));
        assertTrue(json.contains("\"name\":\"testPaths\""));

        // Verify literals aren't used for these types
        assertFalse(json.contains("\"file\":{\"@type\":\"Literal\""));

        // Deserialize
        ConnectionParameters result = objectMapper.readValue(json, ConnectionParameters.class);

        // Verify
        assertEquals(5, result.size());
        assertEquals("testPaths", result.get("name"));

        // Note: After deserialization, all path-related types are strings
        assertEquals("/tmp/testfile.txt", result.get("file"));
        assertEquals("http://example.com/data", result.get("uri"));
        assertEquals("http://example.com/data.json", result.get("url"));
        assertEquals("/tmp/testpath", result.get("path"));
    }
}
