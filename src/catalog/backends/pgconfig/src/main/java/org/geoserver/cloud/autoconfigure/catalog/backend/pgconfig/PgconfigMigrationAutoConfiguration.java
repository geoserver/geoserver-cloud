/*
 * (c) 2023 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.catalog.backend.pgconfig;

import org.geoserver.cloud.config.catalog.backend.pgconfig.DatabaseMigrationConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * @since 1.4
 */
@AutoConfiguration(after = PgconfigDataSourceAutoConfiguration.class)
@ConditionalOnPgconfigBackendEnabled
@Import(DatabaseMigrationConfiguration.class)
public class PgconfigMigrationAutoConfiguration {}
