/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.extensions.appschema;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.configuration.extension.appschema.AppSchemaConfiguration;
import org.geoserver.platform.ModuleStatus;
import org.geoserver.platform.ModuleStatusImpl;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * Auto-configuration for the GeoServer App-Schema extension.
 *
 * <p>This extension enables complex feature mapping using XML-based application schemas. It allows GeoServer to serve
 * complex features by mapping simple features to complex schemas.
 *
 * <p>The extension is disabled by default and can be enabled with:
 *
 * <pre>{@code
 * geoserver:
 *   extension:
 *     appschema:
 *       enabled: true
 * }</pre>
 *
 * @since 2.27.0.0
 * @see AppSchemaConfiguration
 */
@AutoConfiguration
@Import({AppSchemaAutoConfiguration.Enabled.class, AppSchemaAutoConfiguration.Disabled.class})
@EnableConfigurationProperties(AppSchemaConfigProperties.class)
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.extensions.appschema")
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
public class AppSchemaAutoConfiguration {

    public @PostConstruct void log() {
        log.info("App-schema configuration detected");
    }

    @AutoConfiguration
    @ConditionalOnAppSchema
    @Import(AppSchemaConfiguration.class)
    static class Enabled {}

    @AutoConfiguration(after = AppSchemaAutoConfiguration.Enabled.class)
    @ConditionalOnProperty(
            prefix = AppSchemaConfigProperties.PREFIX,
            name = "enabled",
            havingValue = "false",
            matchIfMissing = true)
    static class Disabled {
        @Bean
        ModuleStatus appSchemaExtension() {
            ModuleStatusImpl mod = new ModuleStatusImpl(
                    "gs-app-schema-core", "App Schema Core Extension", "App Schema Core extension");
            mod.setCategory(ModuleStatus.Category.EXTENSION);
            mod.setAvailable(true);
            mod.setEnabled(false);
            mod.setMessage(
                    "App schema extension disabled through config property geoserver.extension.appschema.enabled=false");
            return mod;
        }
    }
}
