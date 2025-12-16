/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.extensions.mapboxstyling;

import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.community.mbstyle.MBStyleHandler;
import org.geoserver.configuration.extension.mbstyle.MapboxStyleConfiguration;
import org.geoserver.platform.ModuleStatus;
import org.geoserver.platform.ModuleStatusImpl;
import org.geotools.util.Version;
import org.geotools.util.factory.GeoTools;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Auto-configuration for MapBox Styling extension that provides a style handler
 * for MapBox stylesheets.
 *
 * @since 2.27.0
 * @see MapboxStyleConfiguration
 */
@AutoConfiguration
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.extensions.mapboxstyling")
@EnableConfigurationProperties(MapBoxStylingConfigProperties.class)
@Import(value = {MapBoxStylingAutoConfiguration.Enabled.class, MapBoxStylingAutoConfiguration.Disabled.class})
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
public class MapBoxStylingAutoConfiguration {

    @Configuration
    @ConditionalOnMapBoxStyling
    @Import(MapboxStyleConfiguration.class)
    static class Enabled {
        @PostConstruct
        void log() {
            log.info("MapBox Styling extension enabled");
        }
    }

    @Configuration
    @ConditionalOnProperty(
            name = "geoserver.extension.mapbox-styling.enabled",
            havingValue = "false",
            matchIfMissing = false)
    @ConditionalOnClass(MBStyleHandler.class)
    static class Disabled {

        @SuppressWarnings("java:S6830")
        @Bean(name = "MBStyleExtension")
        ModuleStatus mbStyleDisabledModuleStatus() {
            ModuleStatusImpl mod = new ModuleStatusImpl();
            mod.setAvailable(true);
            mod.setEnabled(false);
            mod.setMessage(
                    "MapBox Styling module disabled through config property geoserver.extension.mapbox-styling.enabled=false");
            mod.setComponent("MBStyle plugin");
            mod.setModule("gs-mbstyle");
            mod.setName("MBStyle Extension");
            Version v = GeoTools.getVersion(MBStyleHandler.class);
            mod.setVersion(v == null ? null : v.toString());
            return mod;
        }
    }
}
