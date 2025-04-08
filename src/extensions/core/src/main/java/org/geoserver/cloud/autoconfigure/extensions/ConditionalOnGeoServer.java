/*
 * (c) 2025 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.geoserver.config.GeoServer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;

/**
 * Annotation that marks a component to be conditional on the application being
 * a GeoServer application with the GeoServer core classes available in the classpath.
 *
 * <p>
 * This annotation is used as a base for all GeoServer extension conditional
 * annotations. It verifies that the application has the GeoServer core classes
 * in its classpath, indicating that it's a GeoServer-based application.
 *
 * <p>
 * This provides a foundation for more specific conditional annotations like:
 * <ul>
 *   <li>{@link ConditionalOnGeoServerWebUI} - For Web UI specific components</li>
 *   <li>{@link ConditionalOnGeoServerWMS} - For WMS service specific components</li>
 *   <li>{@link ConditionalOnGeoServerWFS} - For WFS service specific components</li>
 *   <li>And other service-specific conditions</li>
 * </ul>
 *
 * <p>
 * Usage example:
 * <pre>{@code
 * @Configuration
 * @ConditionalOnGeoServer
 * public class MyGeoServerConfiguration {
 *     // Configuration only activated in GeoServer applications
 * }
 * }</pre>
 *
 * @since 2.27.0
 */
// original idea was conditional on bean but it's a can of worms with all the forcer spring initialization during
// startup
@ConditionalOnClass(GeoServer.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Documented
public @interface ConditionalOnGeoServer {}
