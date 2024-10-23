/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.catalog.backend.jdbcconfig;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Documented
@ConditionalOnJdbcConfigEnabled
@ConditionalOnClass(name = "org.geoserver.web.GeoServerHomePageContentProvider")
@ConditionalOnProperty(
        prefix = "geoserver.backend.jdbcconfig.web",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public @interface ConditionalOnJdbcConfigWebUIEnabled {}
