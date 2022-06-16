/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.catalog.backend.jdbcconfig;

import org.geoserver.web.GeoServerHomePageContentProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Documented
@ConditionalOnJdbcConfigEnabled
@ConditionalOnProperty(
        prefix = "geoserver.backend.jdbcconfig.web",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
@ConditionalOnClass(GeoServerHomePageContentProvider.class)
public @interface ConditionalOnJdbcConfigWebUIEnabled {}
