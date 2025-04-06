/*
 * (c) 2025 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.cssstyling;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.geoserver.cloud.autoconfigure.extensions.ConditionalOnGeoServerWMS;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * Composite annotation that combines conditions required for CSS styling support.
 *
 * <p>
 * This conditional activates when:
 * <ul>
 *   <li>The GeoServer WMS module is available in the application context</li>
 *   <li>The geoserver.extension.css-styling.enabled property is true (the default)</li>
 * </ul>
 *
 * <p>
 * This annotation can be used on configuration classes or bean methods to make them
 * conditional on CSS styling being enabled.
 *
 * @see ConditionalOnGeoServerWMS
 * @see ConditionalOnProperty
 * @since 2.27.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Documented
@Inherited
@ConditionalOnGeoServerWMS
@ConditionalOnProperty(
        name = "geoserver.extension.css-styling.enabled",
        havingValue = "true",
        matchIfMissing = CssStylingConfigProperties.DEFAULT)
public @interface ConditionalOnCssStyling {}
