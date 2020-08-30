/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.jdbcconfig;

import org.geoserver.cloud.jdbcconfig.JDBCStoreConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(JDBCStoreConfiguration.class)
public class JDBCStoreAutoConfiguration {}
