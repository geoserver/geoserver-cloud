/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.extensions.security.ldap;

import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.configuration.core.security.ldap.LDAPSecurityConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

/**
 * Auto-configuration for the GeoServer LDAP security extension.
 *
 * <p>This extension enables user authentication, user groups and roles to be stored
 * and managed in an LDAP directory.
 *
 * <p>The extension is enabled by default and can be disabled with:
 *
 * <pre>{@code
 * geoserver:
 *   extension:
 *     security:
 *       ldap:
 *         enabled: false
 * }</pre>
 *
 * <p>The externalized configuration in config/geoserver.yml provides backward compatibility
 * with the older property through property placeholders:
 *
 * <pre>{@code
 * geoserver:
 *   security:
 *     ldap: true|false
 * }</pre>
 *
 * @since 2.27.0.0
 * @see LDAPSecurityConfiguration
 */
@AutoConfiguration
@ConditionalOnLDAP
@EnableConfigurationProperties(LDAPConfigProperties.class)
@Import(LDAPSecurityConfiguration.class)
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.extensions.security.ldap")
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
public class LDAPSecurityAutoConfiguration {

    /**
     * Log that the LDAP security configuration is detected.
     */
    public @PostConstruct void log() {
        log.info("LDAP security configuration detected");
    }
}
