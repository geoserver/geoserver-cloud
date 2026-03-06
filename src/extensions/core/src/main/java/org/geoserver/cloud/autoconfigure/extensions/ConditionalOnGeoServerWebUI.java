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
 * Annotation that marks a component to be conditional on the application being the GeoServer Web UI application
 * (containing {@code org.geoserver.web.GeoServerApplication}).
 *
 * <p>This annotation is used to selectively enable components that should only be active in the GeoServer Web UI
 * application, such as UI panels, page components, and related configurations.
 *
 * <p>The condition checks for:
 *
 * <ul>
 *   <li>The presence of GeoServer core classes ({@link ConditionalOnGeoServer})
 *   <li>The presence of the GeoServer Web UI application class
 *   <li>The {@code geoserver.service.webui.enabled} property is set to {@code true}
 * </ul>
 *
 * <p>This conditional uses a property-based approach rather than relying solely on class presence to avoid potential
 * issues with bean initialization order during auto-configuration processing. Each GeoServer service module is
 * responsible for setting its corresponding {@code geoserver.service.[service-name].enabled} property in its bootstrap
 * configuration.
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * @Configuration
 * @ConditionalOnGeoServerWebUI
 * public class WebUISpecificConfiguration {
 *     // Configuration only activated in the Web UI application
 * }
 * }</pre>
 *
 * @see ConditionalOnGeoServer
 * @see ConditionalOnGeoServerWebUIUnavailable
 * @since 2.27.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Documented
@ConditionalOnGeoServer
// use string class name for optional dependencies
@ConditionalOnClass(name = "org.geoserver.web.GeoServerApplication")
@ConditionalOnProperty(name = "geoserver.service.webui.enabled", havingValue = "true", matchIfMissing = false)
public @interface ConditionalOnGeoServerWebUI {}
