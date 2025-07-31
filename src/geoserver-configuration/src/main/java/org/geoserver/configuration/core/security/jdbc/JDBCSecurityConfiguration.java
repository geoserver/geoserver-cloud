/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.configuration.core.security.jdbc;

import org.geoserver.cloud.config.factory.ImportFilteredResource;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for the GeoServer JDBC security extension.
 *
 * <p>This extension enables user authentication, user groups and roles to be stored
 * and managed in a database through JDBC.
 *
 * @since 2.27.0.0
 */
@Configuration
@ImportFilteredResource("jar:gs-sec-jdbc-.*!/applicationContext.xml")
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
public class JDBCSecurityConfiguration {}
