/*
 * (c) 2025 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.security.acl;

import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Placeholder auto-configuration for the GeoServer ACL extension.
 *
 * <p>The actual auto-configuration is provided by the GeoServer ACL plugin itself through
 * its own auto-configuration classes. This class simply provides a consistent configuration
 * property interface for the GeoServer Cloud ecosystem and logs when the extension is detected.
 *
 * <p>The extension is disabled by default and can be enabled with:
 *
 * <pre>{@code
 * geoserver:
 *   extension:
 *     security:
 *       acl:
 *         enabled: true
 * }</pre>
 *
 * <p>The externalized configuration in config/geoserver.yml provides backward compatibility
 * with the older property through property placeholders:
 *
 * <pre>{@code
 * geoserver:
 *   security:
 *     acl:
 *       enabled: true|false
 * }</pre>
 *
 * @since 2.27.0.0
 */
@AutoConfiguration
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
@EnableConfigurationProperties(AclConfigProperties.class)
@ConditionalOnAcl
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.extensions.security.acl")
public class AclAutoConfiguration {

    /**
     * Log that the GeoServer ACL extension is enabled.
     */
    public @PostConstruct void log() {
        log.info("GeoServer ACL extension installed");
    }
}
