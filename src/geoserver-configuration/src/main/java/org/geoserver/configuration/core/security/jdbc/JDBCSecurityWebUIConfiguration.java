/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.configuration.core.security.jdbc;

import org.geoserver.spring.config.annotations.TranspileXmlConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Configuration for the GeoServer JDBC security web UI components.
 *
 * <p>
 * This configuration registers the JDBC security web UI components like panel
 * info classes for configuration through the GeoServer web admin interface.
 *
 * @since 2.27.0.0
 */
@Configuration
@TranspileXmlConfig(locations = "jar:gs-web-sec-jdbc-.*!/applicationContext.xml")
@Import({JDBCSecurityConfiguration.class, JDBCSecurityWebUIConfiguration_Generated.class})
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
public class JDBCSecurityWebUIConfiguration {}
