/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.Conditional;

/**
 * Annotation that marks a component to be conditional on a named GeoServer service being enabled.
 *
 * <p>The named service is resolved in three steps, evaluated in order:
 *
 * <ul>
 *   <li>The explicit {@code geoserver.service.<value>.enabled} property, if present, decides the outcome regardless of
 *       any other setting.
 *   <li>Otherwise, the {@code geoserver.services} comma-separated list of enabled service names is consulted. The
 *       service is enabled if the list contains its name, or the literal {@code all}, case-insensitively.
 *   <li>Otherwise, the condition does not match.
 * </ul>
 *
 * <p>The fallback to "does not match" preserves the {@code matchIfMissing = false} semantics of the
 * {@code @ConditionalOnProperty}-based conditions this annotation replaces: a service that neither sets its own flag
 * nor appears in the services list stays disabled.
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * @Configuration
 * @ConditionalOnGeoServerService("wms")
 * public class WmsSpecificConfiguration {
 *     // Configuration only activated when the wms service is enabled
 * }
 * }</pre>
 *
 * @see GeoServerServiceCondition
 * @since 3.1
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Documented
@Conditional(GeoServerServiceCondition.class)
public @interface ConditionalOnGeoServerService {

    /** The service name, e.g. {@code wms}, as used in {@code geoserver.service.<name>.enabled}. */
    String value();
}
