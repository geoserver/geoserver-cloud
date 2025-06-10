/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.jackson.databind.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import lombok.SneakyThrows;
import org.geotools.jdbc.RegexpValidator;
import org.geotools.jdbc.VirtualTable;
import org.geotools.jdbc.VirtualTableParameter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

/**
 * Test to verify our VirtualTable serialization fixes work correctly
 */
public class VirtualTableSerializationTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new GeoServerCatalogModule());
    }

    @Test
    @SneakyThrows
    void testVirtualTableWithValidatorsAndGeometryTypes() {
        // Create a VirtualTable with validators and geometry types
        VirtualTable originalVt = new VirtualTable("test_vt", "SELECT * FROM test_table", true);

        // Set primary key columns
        originalVt.setPrimaryKeyColumns(Arrays.asList("id", "name"));

        // Add geometry metadata with different geometry types
        originalVt.addGeometryMetadatata("geom_point", Point.class, 4326, 2);
        originalVt.addGeometryMetadatata("geom_line", LineString.class, 4326, 2);
        originalVt.addGeometryMetadatata("geom_poly", Polygon.class, 3857, 2);

        // Add parameters with validators
        VirtualTableParameter param1 = new VirtualTableParameter("param1", "default1");
        param1.setValidator(new RegexpValidator("^[A-Za-z0-9]+$"));
        originalVt.addParameter(param1);

        VirtualTableParameter param2 = new VirtualTableParameter("param2", "default2");
        param2.setValidator(new RegexpValidator("\\d{4}-\\d{2}-\\d{2}"));
        originalVt.addParameter(param2);

        // Serialize to JSON
        String json = objectMapper.writeValueAsString(originalVt);
        System.out.println("Serialized VirtualTable: " + json);

        // Deserialize back
        VirtualTable deserializedVt = objectMapper.readValue(json, VirtualTable.class);

        // Verify basic properties
        assertEquals(originalVt.getName(), deserializedVt.getName());
        assertEquals(originalVt.getSql(), deserializedVt.getSql());
        assertEquals(originalVt.isEscapeSql(), deserializedVt.isEscapeSql());
        assertEquals(originalVt.getPrimaryKeyColumns(), deserializedVt.getPrimaryKeyColumns());

        // Verify geometry types
        assertEquals(originalVt.getGeometries(), deserializedVt.getGeometries());
        assertEquals(Point.class, deserializedVt.getGeometryType("geom_point"));
        assertEquals(LineString.class, deserializedVt.getGeometryType("geom_line"));
        assertEquals(Polygon.class, deserializedVt.getGeometryType("geom_poly"));

        // Verify SRIDs
        assertEquals(4326, deserializedVt.getNativeSrid("geom_point"));
        assertEquals(4326, deserializedVt.getNativeSrid("geom_line"));
        assertEquals(3857, deserializedVt.getNativeSrid("geom_poly"));

        // Verify parameters and validators
        assertEquals(originalVt.getParameterNames(), deserializedVt.getParameterNames());

        VirtualTableParameter deserializedParam1 = deserializedVt.getParameter("param1");
        assertNotNull(deserializedParam1);
        assertEquals("param1", deserializedParam1.getName());
        assertEquals("default1", deserializedParam1.getDefaultValue());
        assertNotNull(deserializedParam1.getValidator());
        assertTrue(deserializedParam1.getValidator() instanceof RegexpValidator);

        VirtualTableParameter deserializedParam2 = deserializedVt.getParameter("param2");
        assertNotNull(deserializedParam2);
        assertEquals("param2", deserializedParam2.getName());
        assertEquals("default2", deserializedParam2.getDefaultValue());
        assertNotNull(deserializedParam2.getValidator());
        assertTrue(deserializedParam2.getValidator() instanceof RegexpValidator);

        // Verify the validators work by checking their patterns
        RegexpValidator validator1 = (RegexpValidator) deserializedParam1.getValidator();
        RegexpValidator validator2 = (RegexpValidator) deserializedParam2.getValidator();

        assertEquals("^[A-Za-z0-9]+$", validator1.getPattern().pattern());
        assertEquals("\\d{4}-\\d{2}-\\d{2}", validator2.getPattern().pattern());

        // Test that the validators actually work (validate() throws exception on invalid input)
        try {
            validator1.validate("test123");
            // If we get here, validation passed
        } catch (RuntimeException e) {
            throw new AssertionError("Valid input 'test123' should not fail validation", e);
        }

        try {
            validator2.validate("2023-12-25");
            // If we get here, validation passed
        } catch (RuntimeException e) {
            throw new AssertionError("Valid input '2023-12-25' should not fail validation", e);
        }
    }

    @Test
    @SneakyThrows
    void testVirtualTableWithNullValues() {
        // Test with null geometry types and null validators
        VirtualTable originalVt = new VirtualTable("test_null", "SELECT * FROM test", false);

        // Add a parameter without validator (null validator)
        VirtualTableParameter param = new VirtualTableParameter("no_validator", "default");
        // param.setValidator(null); // This should be null by default
        originalVt.addParameter(param);

        // Serialize and deserialize
        String json = objectMapper.writeValueAsString(originalVt);
        VirtualTable deserializedVt = objectMapper.readValue(json, VirtualTable.class);

        // Verify
        assertEquals(originalVt.getName(), deserializedVt.getName());
        assertEquals(originalVt.getSql(), deserializedVt.getSql());
        assertEquals(originalVt.isEscapeSql(), deserializedVt.isEscapeSql());

        VirtualTableParameter deserializedParam = deserializedVt.getParameter("no_validator");
        assertNotNull(deserializedParam);
        assertEquals("no_validator", deserializedParam.getName());
        assertEquals("default", deserializedParam.getDefaultValue());
        // The validator should be null or not set
    }
}
