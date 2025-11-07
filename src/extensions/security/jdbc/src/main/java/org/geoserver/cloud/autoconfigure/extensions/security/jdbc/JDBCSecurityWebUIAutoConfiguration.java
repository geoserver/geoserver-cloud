/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.extensions.security.jdbc;

import org.geoserver.configuration.core.web.sec.WebSecJdbcConfiguration;
import org.geoserver.security.web.auth.AuthenticationFilterPanelInfo;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

/**
 * Auto-configuration for the GeoServer JDBC security web UI components.
 *
 * <p>
 * This configuration is only active when:
 * <ul>
 * <li>The JDBC security extension is enabled
 * <li>The GeoServer web UI classes are available on the classpath
 * </ul>
 *
 * <p>
 * It registers the JDBC security web UI components like panel info classes for
 * configuration through the GeoServer web admin interface.
 *
 * @since 2.27.0.0
 * @see WebSecJdbcConfiguration
 */
@AutoConfiguration
@ConditionalOnJDBC
@ConditionalOnClass(AuthenticationFilterPanelInfo.class)
@EnableConfigurationProperties(JDBCConfigProperties.class)
@Import(WebSecJdbcConfiguration.class)
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
public class JDBCSecurityWebUIAutoConfiguration {}
