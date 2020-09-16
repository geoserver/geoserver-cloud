/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geotools.jackson.databind.filter;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.geotools.jackson.databind.geojson.GeoToolsGeoJsonModule;
import org.locationtech.jts.geom.Geometry;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.Expression;

/**
 * Jackson {@link com.fasterxml.jackson.databind.Module} to handle GeoTools {@link Filter} and
 * {@link Expression} bindings.
 *
 * <p>Depends on {@link GeoToolsGeoJsonModule} to being able of encoding and decoding JTS {@link
 * Geometry} literals.
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
 * objectMapper.registerModule(new GeoToolsFilterModule());
 * </code>
 * </pre>
 */
public class GeoToolsFilterModule extends SimpleModule {
    private static final long serialVersionUID = 4898575169880138758L;

    public GeoToolsFilterModule() {
        super(GeoToolsFilterModule.class.getSimpleName(), new Version(1, 0, 0, null, null, null));

        addSerializer(new ExpressionSerializer());
        addSerializer(new FilterSerializer());
        addDeserializer(
                org.opengis.filter.expression.Expression.class, new ExpressionDeserializer());
        addDeserializer(org.opengis.filter.Filter.class, new FilterDeserializer());
    }
}
