/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jackson.databind.ogcapi.features;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.io.IOException;
import java.lang.reflect.Field;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.ogcapi.v1.features.CQL2Conformance;
import org.geoserver.ogcapi.v1.features.ECQLConformance;
import org.geoserver.ogcapi.v1.features.FeatureConformance;

/**
 * Jackson module for serializing and deserializing OGC API Features Conformance classes.
 * <p>
 * This module is registered through the SPI mechanism via META-INF/services/com.fasterxml.jackson.databind.Module
 * to ensure it's picked up by the {@code PgconfigObjectMapper} utility class.
 * <p>
 * NOTE: This module particularly addresses a bug in {@link CQL2Conformance#isAdvanced()} which can throw
 * {@code NullPointerException} when the 'advanced' field is null. This workaround should be removed once
 * the issue is fixed in the upstream GeoServer codebase.
 */
@Slf4j
public class OgcApiFeaturesConformancesModule extends SimpleModule {

    private static final long serialVersionUID = 1L;

    public OgcApiFeaturesConformancesModule() {
        super(OgcApiFeaturesConformancesModule.class.getSimpleName());
        log.debug("Registering Jackson serializers for OGC API Features Conformance classes");

        // Register serializer and deserializer for CQL2Conformance
        addSerializer(CQL2Conformance.class, new CQL2ConformanceSerializer());
        addDeserializer(CQL2Conformance.class, new CQL2ConformanceDeserializer());

        // Register serializer and deserializer for ECQLConformance
        addSerializer(ECQLConformance.class, new ECQLConformanceSerializer());
        addDeserializer(ECQLConformance.class, new ECQLConformanceDeserializer());

        // Register serializer and deserializer for FeatureConformance
        addSerializer(FeatureConformance.class, new FeatureConformanceSerializer());
        addDeserializer(FeatureConformance.class, new FeatureConformanceDeserializer());

        // Register the classes for subtype recognition
        registerSubtypes(CQL2Conformance.class);
        registerSubtypes(ECQLConformance.class);
        registerSubtypes(FeatureConformance.class);
    }

    /**
     * Custom serializer for CQL2Conformance that safely handles the problematic isAdvanced() method.
     */
    static class CQL2ConformanceSerializer extends JsonSerializer<CQL2Conformance> {
        @Override
        public Class<CQL2Conformance> handledType() {
            return CQL2Conformance.class;
        }

        @Override
        public void serialize(CQL2Conformance value, JsonGenerator gen, SerializerProvider serializers)
                throws IOException {

            if (value == null) {
                gen.writeNull();
                return;
            }

            gen.writeStartObject();

            // Write standard fields
            writeNullSafe(gen, "text", value.isText());
            writeNullSafe(gen, "json", value.isJSON());

            // Use reflection for the problematic 'advanced' field
            writeAdvancedField(gen, value);

            // Write other fields using standard getters
            writeNullSafe(gen, "arithmetic", value.isArithmetic());
            writeNullSafe(gen, "basic", value.isBasic());
            writeNullSafe(gen, "basicSpatial", value.isBasicSpatial());
            writeNullSafe(gen, "functions", value.isFunctions());
            writeNullSafe(gen, "propertyProperty", value.isPropertyProperty());
            writeNullSafe(gen, "spatial", value.isSpatial());

            gen.writeEndObject();
        }

        private void writeNullSafe(JsonGenerator gen, String fieldName, Boolean value) throws IOException {
            if (value != null) {
                gen.writeBooleanField(fieldName, value);
            } else {
                gen.writeNullField(fieldName);
            }
        }

        @SuppressWarnings("java:S3011") // /setAccessible required because method and variable typed don't match
        private void writeAdvancedField(JsonGenerator gen, CQL2Conformance value) throws IOException {
            try {
                Field field = CQL2Conformance.class.getDeclaredField("advanced");
                field.setAccessible(true);
                Boolean advanced = (Boolean) field.get(value);

                if (advanced != null) {
                    gen.writeBooleanField("advanced", advanced);
                } else {
                    gen.writeNullField("advanced");
                }
            } catch (Exception e) {
                // If anything goes wrong, write null
                gen.writeNullField("advanced");
            }
        }
    }

    /**
     * Custom deserializer for CQL2Conformance that handles all possible fields.
     */
    static class CQL2ConformanceDeserializer extends JsonDeserializer<CQL2Conformance> {
        @Override
        public Class<CQL2Conformance> handledType() {
            return CQL2Conformance.class;
        }

        @Override
        public CQL2Conformance deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {

            if (p.currentToken() == JsonToken.VALUE_NULL) {
                return null;
            }

            CQL2Conformance conf = new CQL2Conformance();

            // Skip over the class wrapper if present
            if (p.currentToken() != JsonToken.START_OBJECT) {
                p.nextToken();
            }

            while (p.nextToken() != JsonToken.END_OBJECT) {
                String fieldName = p.currentName();
                p.nextToken();

                // Skip null values
                if (p.currentToken() == JsonToken.VALUE_NULL) {
                    continue;
                }

                // Get the value as boolean and set the appropriate field
                Boolean value = p.getBooleanValue();
                setField(conf, fieldName, value);
            }

            return conf;
        }

        private void setField(CQL2Conformance conf, String fieldName, Boolean value) {
            if (value == null) {
                return;
            }
            switch (fieldName) {
                case "text" -> conf.setText(value);
                case "json" -> conf.setJSON(value);
                case "advanced" -> conf.setCql2Advanced(value);
                case "arithmetic" -> conf.setArtihmetic(value);
                case "basic" -> conf.setBasic(value);
                case "basicSpatial" -> conf.setBasicSpatial(value);
                case "functions" -> conf.setFunctions(value);
                case "propertyProperty" -> conf.setPropertyProperty(value);
                case "spatial" -> conf.setSpatial(value);
                default -> throw new IllegalArgumentException("Unknown field in CQL2Conformance: " + fieldName);
            }
        }
    }

    /**
     * Custom serializer for ECQLConformance.
     */
    static class ECQLConformanceSerializer extends JsonSerializer<ECQLConformance> {
        @Override
        public Class<ECQLConformance> handledType() {
            return ECQLConformance.class;
        }

        @Override
        public void serialize(ECQLConformance value, JsonGenerator gen, SerializerProvider serializers)
                throws IOException {

            if (value == null) {
                gen.writeNull();
                return;
            }

            gen.writeStartObject();

            // Write fields directly using getters
            writeNullSafe(gen, "text", value.isText());

            gen.writeEndObject();
        }

        private void writeNullSafe(JsonGenerator gen, String fieldName, Boolean value) throws IOException {
            if (value != null) {
                gen.writeBooleanField(fieldName, value);
            } else {
                gen.writeNullField(fieldName);
            }
        }
    }

    /**
     * Custom deserializer for ECQLConformance.
     */
    static class ECQLConformanceDeserializer extends JsonDeserializer<ECQLConformance> {
        @Override
        public Class<ECQLConformance> handledType() {
            return ECQLConformance.class;
        }

        @Override
        public ECQLConformance deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            if (p.currentToken() == JsonToken.VALUE_NULL) {
                return null;
            }

            ECQLConformance conf = new ECQLConformance();

            // Skip over the class wrapper if present
            if (p.currentToken() != JsonToken.START_OBJECT) {
                p.nextToken();
            }

            while (p.nextToken() != JsonToken.END_OBJECT) {
                String fieldName = p.currentName();
                p.nextToken();

                // Skip null values
                if (p.currentToken() == JsonToken.VALUE_NULL) {
                    continue;
                }

                // Set the appropriate field
                Boolean value = p.getBooleanValue();
                switch (fieldName) {
                    // "ecql" constant preserved for pre 2.28.0 compatibility
                    case "ecql", "text" -> conf.setText(value);
                    default -> throw new IllegalArgumentException("Unknown field in ECQLConformance: " + fieldName);
                }
            }

            return conf;
        }
    }

    /**
     * Custom serializer for FeatureConformance.
     */
    static class FeatureConformanceSerializer extends JsonSerializer<FeatureConformance> {
        @Override
        public Class<FeatureConformance> handledType() {
            return FeatureConformance.class;
        }

        @Override
        public void serialize(FeatureConformance value, JsonGenerator gen, SerializerProvider serializers)
                throws IOException {

            if (value == null) {
                gen.writeNull();
                return;
            }

            gen.writeStartObject();

            // Write all fields directly using getters
            writeNullSafe(gen, "core", value.isCore());
            writeNullSafe(gen, "gml321", value.isGML321());
            writeNullSafe(gen, "gmlSF0", value.isGMLSFO());
            writeNullSafe(gen, "gmlSF2", value.isGMLSF2());
            writeNullSafe(gen, "featuresFilter", value.isFeaturesFilter());
            writeNullSafe(gen, "crsByReference", value.isCRSByReference());
            writeNullSafe(gen, "filter", value.isFilter());
            writeNullSafe(gen, "search", value.isSearch());
            writeNullSafe(gen, "queryables", value.isQueryables());
            writeNullSafe(gen, "ids", value.isIDs());
            writeNullSafe(gen, "sortBy", value.isSortBy());

            gen.writeEndObject();
        }

        private void writeNullSafe(JsonGenerator gen, String fieldName, Boolean value) throws IOException {
            if (value != null) {
                gen.writeBooleanField(fieldName, value);
            } else {
                gen.writeNullField(fieldName);
            }
        }
    }

    /**
     * Custom deserializer for FeatureConformance.
     */
    static class FeatureConformanceDeserializer extends JsonDeserializer<FeatureConformance> {
        @Override
        public Class<FeatureConformance> handledType() {
            return FeatureConformance.class;
        }

        @Override
        public FeatureConformance deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            if (p.currentToken() == JsonToken.VALUE_NULL) {
                return null;
            }

            FeatureConformance conf = new FeatureConformance();

            // Skip over the class wrapper if present
            if (p.currentToken() != JsonToken.START_OBJECT) {
                p.nextToken();
            }

            while (p.nextToken() != JsonToken.END_OBJECT) {
                String fieldName = p.currentName();
                p.nextToken();

                // Skip null values
                if (p.currentToken() == JsonToken.VALUE_NULL) {
                    continue;
                }

                // Get the value as boolean
                Boolean value = p.getBooleanValue();

                // Set field using reflection
                setField(conf, fieldName, value);
            }

            return conf;
        }

        private void setField(FeatureConformance conf, String fieldName, Boolean value) {
            // Set fields directly using setters
            switch (fieldName) {
                case "core" -> conf.setCore(value);
                case "gml321" -> conf.setGML321(value);
                case "gmlSF0" -> conf.setGMLSF0(value);
                case "gmlSF2" -> conf.setGMLSF2(value);
                case "featuresFilter" -> conf.setFeaturesFilter(value);
                case "crsByReference" -> conf.setCRSByReference(value);
                case "filter" -> conf.setFilter(value);
                case "search" -> conf.setSearch(value);
                case "queryables" -> conf.setQueryables(value);
                case "ids" -> conf.setIDs(value);
                case "sortBy" -> conf.setSortBy(value);
                default -> throw new IllegalArgumentException("Unknown field in FeatureConformance: " + fieldName);
            }
        }
    }
}
