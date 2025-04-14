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
 * a GeoServer WMS (Web Map Service) application.
 *
 * <p>
 * This annotation is used to selectively enable components that should only be active
 * in GeoServer WMS applications. It verifies that:
 * <ul>
 *   <li>The application has GeoServer core classes ({@link ConditionalOnGeoServer})</li>
 *   <li>The WMS service class is available in the classpath</li>
 *   <li>The {@code geoserver.service.wms.enabled} property is set to {@code true}</li>
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
 * @ConditionalOnGeoServerWMS
 * public class WmsSpecificConfiguration {
 *     // Configuration only activated in WMS service
 * }
 * }</pre>
 *
 * @see ConditionalOnGeoServer
 * @see ConditionalOnGeoServerWFS
 * @see ConditionalOnGeoServerWCS
 * @see ConditionalOnGeoServerWPS
 * @see ConditionalOnGeoServerREST
 * @see ConditionalOnGeoServerWebUI
 * @see ConditionalOnGeoServerGWC
 * @since 2.27.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Documented
@ConditionalOnGeoServer
@ConditionalOnClass(org.geoserver.wms.DefaultWebMapService.class)
@ConditionalOnProperty(name = "geoserver.service.wms.enabled", havingValue = "true", matchIfMissing = false)
public @interface ConditionalOnGeoServerWMS {}
