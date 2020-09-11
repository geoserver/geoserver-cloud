/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.catalog;

import org.geoserver.cloud.config.jdbcconfig.JDBCConfigBackendConfigurer;
import org.geoserver.cloud.config.jdbcconfig.JDBCDataSourceConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration(proxyBeanMethods = false)
@ConditionalOnJdbcConfigEnabled
@Import({JDBCConfigBackendConfigurer.class, JDBCDataSourceConfiguration.class})
public class JDBCConfigAutoConfiguration extends AbstractBackendAutoConfiguration {}
