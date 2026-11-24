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

/**
 * Annotation that marks a component to be conditional on the application being a GeoServer WPS (Web Processing Service)
 * application.
 *
 * <p>This annotation is used to selectively enable components that should only be active in GeoServer WPS applications.
 * It verifies that:
 *
 * <ul>
 *   <li>The application has GeoServer core classes ({@link ConditionalOnGeoServer})
 *   <li>The WPS service class is available in the classpath
 *   <li>The {@code wps} service is enabled, resolved through {@link ConditionalOnGeoServerService}
 * </ul>
 *
 * <p>This conditional uses a property-based approach rather than bean detection to avoid potential issues with bean
 * initialization order during auto-configuration processing. Each GeoServer service module is responsible for setting
 * its corresponding {@code geoserver.service.[service-name].enabled} property in its bootstrap configuration.
 *
 * <p>An earlier revision also required the {@code wpsResourceManager} bean to already exist, as a proxy for "running in
 * the wps application". The service-flag check above now expresses that directly, and the bean requirement was dropped:
 * a component that declares this annotation can itself be the one that creates {@code wpsResourceManager}, and a bean
 * can never exist before the configuration that defines it has run.
 *
 * <p>Usage example:
 *
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
@ConditionalOnClass(name = "org.geoserver.wps.DefaultWebProcessingService")
@ConditionalOnGeoServerService("wps")
public @interface ConditionalOnGeoServerWPS {}
