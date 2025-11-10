/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.extensions.cssstyling;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.catalog.StyleHandler;
import org.geoserver.cloud.config.factory.ImportFilteredResource;
import org.geoserver.community.css.web.CssHandler;
import org.geoserver.platform.ModuleStatus;
import org.geoserver.platform.ModuleStatusImpl;
import org.geotools.styling.css.CssParser;
import org.geotools.util.Version;
import org.geotools.util.factory.GeoTools;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Auto-configuration for CSS Styling extension that provides a style handler for CSS
 * stylesheets.
 *
 * <p>
 * This auto-configuration class enables the CSS styling extension in GeoServer Cloud,
 * allowing users to define map styles using CSS syntax instead of SLD. It will be activated
 * when the following conditions are met:
 * <ul>
 *   <li>The CSS handler classes are on the classpath</li>
 *   <li>The geoserver.extension.css-styling.enabled property is true (the default)</li>
 * </ul>
 *
 * <p>
 * The configuration consists of two inner classes:
 * <ul>
 *   <li>Enabled - Imports the CSS extension when it's enabled</li>
 *   <li>Disabled - Provides a disabled module status when extension is explicitly disabled</li>
 * </ul>
 *
 * @since 2.27.0
 */
@AutoConfiguration
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.extensions.cssstyling")
@EnableConfigurationProperties(CssStylingConfigProperties.class)
@Import(value = {CssStylingAutoConfiguration.Enabled.class, CssStylingAutoConfiguration.Disabled.class})
public class CssStylingAutoConfiguration {

    /**
     * Configuration class that activates CSS styling extension when enabled.
     */
    @Configuration
    @ConditionalOnCssStyling
    @ImportFilteredResource("jar:gs-css-.*!/applicationContext.xml")
    static class Enabled {
        @PostConstruct
        void log() {
            log.info("CSS Styling extension enabled");
        }
    }

    /**
     * Configuration class that activates when the CSS styling extension is explicitly disabled.
     *
     * <p>
     * {@link CssHandler} is both a {@link StyleHandler} and a {@link ModuleStatus}. This config
     * engages when CSS styling is disabled and provides a {@link ModuleStatus} with
     * {@link ModuleStatus#isEnabled() == false}.
     */
    @Configuration
    @ConditionalOnProperty(
            name = "geoserver.extension.css-styling.enabled",
            havingValue = "false",
            matchIfMissing = false)
    static class Disabled {

        /**
         * Creates a ModuleStatus bean indicating that the CSS styling module is disabled.
         *
         * @return A ModuleStatus instance marked as available but disabled
         */
        @Bean
        ModuleStatus cssDisabledModuleStatus() {
            ModuleStatusImpl mod = new ModuleStatusImpl();
            mod.setAvailable(true);
            mod.setEnabled(false);
            mod.setMessage(
                    "CSS Styling module disabled through config property geoserver.extension.css-styling.enabled=false");
            mod.setComponent("GeoServer CSS Styling");
            mod.setModule("gs-css");
            mod.setName("CSS");
            Version v = GeoTools.getVersion(CssParser.class);
            mod.setVersion(v == null ? null : v.toString());
            return mod;
        }
    }
}
