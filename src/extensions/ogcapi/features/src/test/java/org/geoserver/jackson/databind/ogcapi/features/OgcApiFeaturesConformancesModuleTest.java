/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jackson.databind.ogcapi.features;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.geoserver.ogcapi.v1.features.CQL2Conformance;
import org.geoserver.ogcapi.v1.features.ECQLConformance;
import org.geoserver.ogcapi.v1.features.FeatureConformance;
import org.geotools.jackson.databind.filter.dto.Literal;
import org.geotools.jackson.databind.util.ObjectMapperUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for the OgcApiFeaturesConformancesModule, particularly focused on
 * ensuring the serialization works correctly for all supported conformance
 * classes.
 */
class OgcApiFeaturesConformancesModuleTest {

    private ObjectMapper mapper;

    @BeforeEach
    void setup() {
        // create using ObjectMapperUtil.newObjectMapper() as done by pgconfig
        mapper = ObjectMapperUtil.newObjectMapper();
    }

    @Test
    void testSerializeDeserializeCQL2Conformance() throws JsonProcessingException {

        CQL2Conformance original = new CQL2Conformance();
        original.setText(true);
        original.setJSON(false);
        original.setBasic(true);

        String json = mapper.writeValueAsString(original);

        assertTrue(json.contains("\"text\":true"));
        assertTrue(json.contains("\"json\":false"));
        assertTrue(json.contains("\"basic\":true"));

        CQL2Conformance deserialized = mapper.readValue(json, CQL2Conformance.class);

        assertEquals(true, deserialized.isText());
        assertEquals(false, deserialized.isJSON());
        assertEquals(true, deserialized.isBasic());
    }

    @Test
    void testCQL2ConformanceWithNullAdvanced() throws JsonProcessingException {

        CQL2Conformance conf = new CQL2Conformance();
        conf.setText(true);

        String json = mapper.writeValueAsString(conf);

        CQL2Conformance deserialized = mapper.readValue(json, CQL2Conformance.class);

        assertEquals(true, deserialized.isText());
    }

    @Test
    void testCQL2ConformanceWithNonNullAdvanced() throws JsonProcessingException {

        CQL2Conformance conf = new CQL2Conformance();
        conf.setCql2Advanced(true);

        String json = mapper.writeValueAsString(conf);

        assertTrue(json.contains("\"advanced\":true"));

        CQL2Conformance deserialized = mapper.readValue(json, CQL2Conformance.class);

        assertEquals(true, deserialized.isAdvanced());
    }

    @Test
    void testCQL2ConformanceInLiteral() throws JsonProcessingException {
        CQL2Conformance conf = new CQL2Conformance();
        conf.setText(true);
        conf.setJSON(false);
        conf.setBasic(true);

        Literal literal = new Literal();
        literal.setValue(conf);

        String json = mapper.writeValueAsString(literal);

        Literal deserializedLiteral = mapper.readValue(json, Literal.class);
        assertNotNull(deserializedLiteral);

        Object value = deserializedLiteral.getValue();
        assertThat(value).isInstanceOf(CQL2Conformance.class);

        CQL2Conformance deserialized = (CQL2Conformance) value;
        assertEquals(true, deserialized.isText());
        assertEquals(false, deserialized.isJSON());
        assertEquals(true, deserialized.isBasic());
    }

    @Test
    void testSerializeDeserializeECQLConformance() throws JsonProcessingException {
        ECQLConformance original = new ECQLConformance();
        original.setECQL(true);

        String json = mapper.writeValueAsString(original);
        assertTrue(json.contains("\"ecql\":true"));

        ECQLConformance deserialized = mapper.readValue(json, ECQLConformance.class);
        assertEquals(true, deserialized.isECQL());
    }

    @Test
    void testECQLConformanceInLiteral() throws JsonProcessingException {
        ECQLConformance conf = new ECQLConformance();
        conf.setText(true);

        Literal literal = new Literal();
        literal.setValue(conf);

        String json = mapper.writeValueAsString(literal);

        Literal deserializedLiteral = mapper.readValue(json, Literal.class);
        assertNotNull(deserializedLiteral);

        Object value = deserializedLiteral.getValue();
        assertThat(value).isInstanceOf(ECQLConformance.class);

        ECQLConformance deserialized = (ECQLConformance) value;
        assertEquals(true, deserialized.isText());
    }

    @Test
    void testSerializeDeserializeFeatureConformance() throws JsonProcessingException {

        FeatureConformance original = new FeatureConformance();
        original.setCore(true);
        original.setGMLSF0(false);
        original.setQueryables(true);
        original.setFilter(true);

        String json = mapper.writeValueAsString(original);

        assertTrue(json.contains("\"core\":true"));
        assertTrue(json.contains("\"gmlSF0\":false"));
        assertTrue(json.contains("\"queryables\":true"));
        assertTrue(json.contains("\"filter\":true"));

        FeatureConformance deserialized = mapper.readValue(json, FeatureConformance.class);

        assertEquals(true, deserialized.isCore());
        assertEquals(false, deserialized.isGMLSFO());
        assertEquals(true, deserialized.isQueryables());
        assertEquals(true, deserialized.isFilter());
    }

    @Test
    void testFeatureConformanceInLiteral() throws JsonProcessingException {

        FeatureConformance original = new FeatureConformance();
        original.setCore(true);
        original.setGMLSF0(false);
        original.setQueryables(true);
        original.setFilter(true);

        Literal literal = new Literal();
        literal.setValue(original);

        String json = mapper.writeValueAsString(literal);

        Literal deserializedLiteral = mapper.readValue(json, Literal.class);
        assertNotNull(deserializedLiteral);

        Object value = deserializedLiteral.getValue();
        assertThat(value).isInstanceOf(FeatureConformance.class);

        FeatureConformance deserialized = (FeatureConformance) value;
        assertEquals(true, deserialized.isCore());
        assertEquals(false, deserialized.isGMLSFO());
        assertEquals(true, deserialized.isQueryables());
        assertEquals(true, deserialized.isFilter());
    }
}
