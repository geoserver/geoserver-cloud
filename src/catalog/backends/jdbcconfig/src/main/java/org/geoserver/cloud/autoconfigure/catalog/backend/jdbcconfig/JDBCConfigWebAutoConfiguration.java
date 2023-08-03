/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.catalog.backend.jdbcconfig;

import org.geoserver.cloud.config.catalog.backend.jdbcconfig.JDBCConfigWebConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

/** Auto configuration for the wicket ui components of the jdbcconfig extension */
@AutoConfiguration
@ConditionalOnJdbcConfigWebUIEnabled
@Import({JDBCConfigWebConfiguration.class})
public class JDBCConfigWebAutoConfiguration {}
