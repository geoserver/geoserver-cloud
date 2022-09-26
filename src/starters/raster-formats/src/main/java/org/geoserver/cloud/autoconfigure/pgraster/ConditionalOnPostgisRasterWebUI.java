/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.pgraster;

import org.geoserver.web.GeoServerApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Conditional to enable the WebUI extension for Postgis Raster coverage format.
 *
 * <p>Conditions:
 *
 * <ul>
 *   <li>{@link GeoServerApplication} is in the classpath
 * </ul>
 *
 * @since 1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Documented
@ConditionalOnClass(GeoServerApplication.class)
public @interface ConditionalOnPostgisRasterWebUI {}
