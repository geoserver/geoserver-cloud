/*
 * (c) 2025 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions;

import java.lang.annotation.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;

/**
 * Annotation that marks a component to be conditional on the application being
 * a GeoServer GWC application.
 *
 * <p>
 * Usage example:
 * <pre>{@code
 * @Configuration
 * @ConditionalOnGeoServerGWC
 * public class GwcSpecificConfiguration {
 *     // Configuration only activated in GWC service
 * }
 * }</pre>
 *
 * @see ConditionalOnGeoServer
 * @see ConditionalOnGeoServerWFS
 * @see ConditionalOnGeoServerWCS
 * @see ConditionalOnGeoServerWPS
 * @since 2.27.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Documented
@ConditionalOnGeoServer
@ConditionalOnClass(org.geoserver.gwc.GWC.class)
// @ConditionalOnBean(name = "gwcServiceTarget")
public @interface ConditionalOnGeoServerGWC {}
