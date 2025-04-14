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
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * Annotation that marks a component to be conditional on the application being
 * a GeoServer REST Configuration service application.
 *
 * <p>
 * This annotation is used to selectively enable components that should only be active
 * in GeoServer REST Configuration applications. In GeoServer Cloud, this generally refers
 * to the REST microservice that handles configuration operations through GeoServer's REST API.
 *
 * <p>
 * The condition verifies that:
 * <ul>
 *   <li>The application has GeoServer core classes ({@link ConditionalOnGeoServer})</li>
 *   <li>The REST Configuration service class is available in the classpath</li>
 *   <li>The {@code geoserver.service.restconfig.enabled} property is set to {@code true}</li>
 * </ul>
 *
 * <p>
 * This conditional uses a property-based approach rather than bean detection to avoid
 * potential issues with bean initialization order during auto-configuration processing.
 * Each GeoServer service module is responsible for setting its corresponding
 * {@code geoserver.service.[service-name].enabled} property in its bootstrap configuration.
 *
 * <p>
 * Usage example:
 * <pre>{@code
 * @Configuration
 * @ConditionalOnGeoServerREST
 * public class RestSpecificConfiguration {
 *     // Configuration only activated in REST Configuration service
 * }
 * }</pre>
 *
 * @see ConditionalOnGeoServer
 * @see ConditionalOnGeoServerWMS
 * @see ConditionalOnGeoServerWFS
 * @see ConditionalOnGeoServerWCS
 * @see ConditionalOnGeoServerWPS
 * @see ConditionalOnGeoServerWebUI
 * @see ConditionalOnGeoServerGWC
 * @since 2.27.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Documented
@ConditionalOnGeoServer
@ConditionalOnClass(org.geoserver.rest.security.RestConfigXStreamPersister.class)
@ConditionalOnProperty(name = "geoserver.service.restconfig.enabled", havingValue = "true", matchIfMissing = false)
public @interface ConditionalOnGeoServerREST {}
