/*
 * (c) 2025 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.inspire.gwc;

import java.lang.annotation.*;
import org.geoserver.cloud.autoconfigure.extensions.ConditionalOnGeoServerGWC;
import org.geoserver.cloud.autoconfigure.extensions.inspire.ConditionalOnInspire;

/**
 * Conditional annotation that only matches when:
 * <ul>
 *   <li>A GeoServer instance is available in the application context
 *   <li>The INSPIRE extension is enabled via configuration property
 *   <li>GWC is available
 * </ul>
 *
 * @since 2.27.0.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Documented
@Inherited
@ConditionalOnInspire
// @ConditionalOnClass(GWC.class)
@ConditionalOnGeoServerGWC
public @interface ConditionalOnInspireGwc {}
