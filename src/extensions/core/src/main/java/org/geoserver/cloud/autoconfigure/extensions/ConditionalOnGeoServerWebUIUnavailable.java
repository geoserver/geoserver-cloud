/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.extensions;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;

/**
 * Annotation that marks a component to be conditional on the application NOT being
 * the GeoServer Web UI application.
 *
 * <p>
 * This is the logical opposite of {@link ConditionalOnGeoServerWebUI} and is used to
 * selectively enable components that should only be active in GeoServer services
 * (like WMS, WFS, etc.) but not in the Web UI application.
 *
 * <p>
 * The condition checks for:
 * <ul>
 *   <li>The presence of GeoServer core classes ({@link ConditionalOnGeoServer})</li>
 *   <li>The absence of the GeoServer Web UI application class</li>
 * </ul>
 *
 * <p>
 * Usage example:
 * <pre>{@code
 * @Configuration
 * @ConditionalOnGeoServerWebUIUnavailable
 * public class ServiceOnlyConfiguration {
 *     // Configuration only activated in non-WebUI GeoServer services
 * }
 * }</pre>
 *
 * @see ConditionalOnGeoServer
 * @see ConditionalOnGeoServerWebUI
 * @since 2.27.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Documented
@ConditionalOnGeoServer
@ConditionalOnMissingClass("org.geoserver.web.GeoServerApplication")
public @interface ConditionalOnGeoServerWebUIUnavailable {}
