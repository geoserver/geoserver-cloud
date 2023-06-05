/*
 * (c) 2023 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.catalog.backend.pgsql;

import org.geoserver.cloud.config.catalog.backend.pgsql.PgsqlDataSourceConfiguration;
import org.geoserver.cloud.config.jndidatasource.JNDIDataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * @since 1.4
 */
@AutoConfiguration(after = JNDIDataSourceAutoConfiguration.class)
@ConditionalOnPgsqlBackendEnabled
@Import(PgsqlDataSourceConfiguration.class)
public class PgsqlDataSourceAutoConfiguration {}
