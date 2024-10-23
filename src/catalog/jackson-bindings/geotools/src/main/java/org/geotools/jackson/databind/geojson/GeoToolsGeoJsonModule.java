/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geotools.jackson.databind.geojson;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import lombok.extern.slf4j.Slf4j;
import org.geotools.jackson.databind.geojson.geometry.GeometryDeserializer;
import org.geotools.jackson.databind.geojson.geometry.GeometrySerializer;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

/**
 * Jackson {@link com.fasterxml.jackson.databind.Module} to handle GeoJSON bindings for JTS {@link
 * Geometry geometries}.
 *
 * <p>When running a spring-boot application, being on the classpath should be enough to get this
 * module auto-registered to all {@link ObjectMapper}s, by means of being registered under {@code
 * META-INF/services/com.fasterxml.jackson.databind.Module}.
 *
 * <p>To register the module for a specific {@link ObjectMapper}, either:
 *
 * <pre>
 * <code>
 * ObjectMapper objectMapper = ...
 * objectMapper.findAndRegisterModules();
 * </code>
 * </pre>
 *
 * Or:
 *
 * <pre>
 * <code>
 * ObjectMapper objectMapper = ...
 * objectMapper.registerModule(new GeoToolsGeoJsonModule());
 * </code>
 * </pre>
 */
@Slf4j(topic = "org.geotools.jackson.databind.geojson")
public class GeoToolsGeoJsonModule extends SimpleModule {
    private static final long serialVersionUID = 4898575169880138758L;

    public GeoToolsGeoJsonModule() {
        super(GeoToolsGeoJsonModule.class.getSimpleName(), new Version(1, 0, 0, null, null, null));

        log.debug("registering de/serializers for all JTS Geometry types");

        addSerializer(new GeometrySerializer());

        addDeserializer(Geometry.class, new GeometryDeserializer<>());
        addDeserializer(Point.class, new GeometryDeserializer<>());
        addDeserializer(MultiPoint.class, new GeometryDeserializer<>());
        addDeserializer(LineString.class, new GeometryDeserializer<>());
        addDeserializer(MultiLineString.class, new GeometryDeserializer<>());
        addDeserializer(Polygon.class, new GeometryDeserializer<>());
        addDeserializer(MultiPolygon.class, new GeometryDeserializer<>());
        addDeserializer(GeometryCollection.class, new GeometryDeserializer<>());
    }
}
