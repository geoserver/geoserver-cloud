/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.catalog.backend.pgconfig;

import org.geoserver.cloud.config.catalog.backend.pgconfig.PgconfigBackendConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

/** @since 1.4 */
@AutoConfiguration(after = PgconfigMigrationAutoConfiguration.class)
@ConditionalOnPgconfigBackendEnabled
@Import(PgconfigBackendConfiguration.class)
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
public class PgconfigBackendAutoConfiguration {}
