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
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * Annotation that marks a component to be conditional on the application being
 * a GeoServer WPS (Web Processing Service) application.
 *
 * <p>
 * This annotation is used to selectively enable components that should only be active
 * in GeoServer WPS applications. It verifies that:
 * <ul>
 *   <li>The application has GeoServer core classes ({@link ConditionalOnGeoServer})</li>
 *   <li>The WPS service class is available in the classpath</li>
 *   <li>The {@code geoserver.service.wps.enabled} property is set to {@code true}</li>
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
 * @ConditionalOnGeoServerWPS
 * public class WpsSpecificConfiguration {
 *     // Configuration only activated in WPS service
 * }
 * }</pre>
 *
 * @see ConditionalOnGeoServer
 * @see ConditionalOnGeoServerWMS
 * @see ConditionalOnGeoServerWFS
 * @see ConditionalOnGeoServerWCS
 * @see ConditionalOnGeoServerREST
 * @see ConditionalOnGeoServerWebUI
 * @see ConditionalOnGeoServerGWC
 * @since 2.27.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Documented
@ConditionalOnGeoServer
@ConditionalOnClass(org.geoserver.wps.DefaultWebProcessingService.class)
@ConditionalOnBean(name = "wpsResourceManager")
@ConditionalOnProperty(name = "geoserver.service.wps.enabled", havingValue = "true", matchIfMissing = false)
public @interface ConditionalOnGeoServerWPS {}
