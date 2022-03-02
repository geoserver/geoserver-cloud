/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.gwc;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Conditionals:
 *
 * <ul>
 *   <li>{@literal gwc.enabled=true}: Core gwc integration is enabled
 *   <li>{@literal gwc.web-ui=true}: gwc web-ui is enabled
 * </ul>
 *
 * @since 1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Documented
@ConditionalOnGeoWebCacheEnabled
@ConditionalOnProperty(
        name = GeoWebCacheConfigurationProperties.WEBUI_ENABLED,
        havingValue = "true",
        matchIfMissing = false)
public @interface ConditionalOnWebUIEnabled {}
