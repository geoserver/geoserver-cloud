/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.extensions.security.jdbc;

import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.configuration.core.security.jdbc.JDBCSecurityConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

/**
 * Auto-configuration for the GeoServer JDBC security extension.
 *
 * <p>
 * This extension enables user authentication, user groups and roles to be
 * stored and managed in a database through JDBC.
 *
 * <p>
 * The extension is enabled by default and can be disabled with:
 *
 * <pre>{@code
 * geoserver:
 *   extension:
 *     security:
 *       jdbc:
 *         enabled: false
 * }</pre>
 *
 * <p>
 * The externalized configuration in config/geoserver.yml provides backward
 * compatibility with the older property through property placeholders:
 *
 * <pre>{@code
 * geoserver:
 *   security:
 *     jdbc: true|false
 * }</pre>
 *
 * @since 2.27.0.0
 * @see JDBCSecurityConfiguration
 */
@AutoConfiguration
@ConditionalOnJDBC
@EnableConfigurationProperties(JDBCConfigProperties.class)
@Import(JDBCSecurityConfiguration.class)
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.extensions.security.jdbc")
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
public class JDBCSecurityAutoConfiguration {

    /**
     * Log that the JDBC security configuration is detected.
     */
    public @PostConstruct void log() {
        log.info("JDBC security configuration detected");
    }
}
