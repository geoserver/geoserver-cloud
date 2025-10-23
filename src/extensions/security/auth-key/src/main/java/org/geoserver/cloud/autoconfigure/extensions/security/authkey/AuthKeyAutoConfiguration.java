/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.extensions.security.authkey;

import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.configuration.extension.authkey.AuthKeyConfiguration;
import org.geoserver.configuration.extension.authkey.AuthKeyWebUIConfiguration;
import org.geoserver.platform.ModuleStatus;
import org.geoserver.platform.ModuleStatusImpl;
import org.geoserver.security.web.auth.AuthenticationFilterPanel;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Auto-configuration for the GeoServer AuthKey extension.
 *
 * <p>This extension enables authentication via URL parameter or HTTP header tokens in GeoServer.
 *
 * <p>The extension is enabled by default and can be disabled with:
 *
 * <pre>{@code
 * geoserver:
 *   extension:
 *     security:
 *       auth-key:
 *         enabled: false
 * }</pre>
 *
 * <p>The externalized configuration in config/geoserver.yml provides backward compatibility
 * with the older property through property placeholders:
 *
 * <pre>{@code
 * geoserver:
 *   security:
 *     authkey: true|false
 * }</pre>
 *
 * @since 2.27.0
 * @see AuthKeyConfiguration
 * @see AuthKeyWebUIConfiguration
 */
@AutoConfiguration
@EnableConfigurationProperties(AuthKeyConfigProperties.class)
@Import({AuthKeyAutoConfiguration.Enabled.class, AuthKeyAutoConfiguration.WebUI.class})
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.extensions.security.authkey")
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
public class AuthKeyAutoConfiguration {

    /**
     * Creates the AuthKey module status bean.
     *
     * @param config the AuthKey configuration properties
     * @return the AuthKey module status bean
     */
    @Bean(name = "authKeyExtension")
    ModuleStatus authKeyExtension(AuthKeyConfigProperties config) {
        ModuleStatusImpl module = new ModuleStatusImpl();
        module.setName("Authkey Extension");
        module.setModule("gs-authkey");
        module.setComponent("Authkey extension");
        module.setAvailable(true);
        module.setEnabled(config.isEnabled());
        if (!config.isEnabled()) {
            module.setMessage("Authkey Extension disabled: " + AuthKeyConfigProperties.PREFIX + ".enabled=false");
        }
        return module;
    }

    /**
     * Configuration for the core AuthKey components.
     */
    @ConditionalOnAuthKey
    @Import(AuthKeyConfiguration.class)
    static @Configuration class Enabled {
        public @PostConstruct void log() {
            log.info("AuthKey extension enabled");
        }
    }

    /**
     * Configuration for the AuthKey Web UI components.
     */
    @ConditionalOnAuthKey
    @ConditionalOnClass(AuthenticationFilterPanel.class)
    @Import(AuthKeyWebUIConfiguration.class)
    static @Configuration class WebUI {
        public @PostConstruct void log() {
            log.info("AuthKey WebUI extension enabled");
        }
    }
}
