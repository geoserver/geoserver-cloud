/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.catalog;

import org.geoserver.cloud.config.jdbcconfig.JDBCConfigWebConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;

/** Auto configuration for the wicket ui components of the jdbcconfig extension */
@ConditionalOnClass(name = "org.geoserver.web.GeoServerHomePageContentProvider")
@ConditionalOnJdbcConfigEnabled
@ConditionalOnProperty(
        prefix = "geoserver.backend.jdbcconfig.web",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = false)
@Import({JDBCConfigWebConfiguration.class})
public class JDBCConfigWebAutoConfiguration {}
