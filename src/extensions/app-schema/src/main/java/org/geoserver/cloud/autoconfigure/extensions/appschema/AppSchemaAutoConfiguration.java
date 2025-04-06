/*
 * (c) 2025 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.appschema;

import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.platform.ModuleStatus;
import org.geoserver.platform.ModuleStatusImpl;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * Auto-configuration for the GeoServer App-Schema extension.
 *
 * <p>This extension enables complex feature mapping using XML-based application schemas. It allows
 * GeoServer to serve complex features by mapping simple features to complex schemas.
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
 */
@AutoConfiguration
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
@EnableConfigurationProperties(AppSchemaConfigProperties.class)
@Import(AppSchemaConfiguration.class)
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.extensions.appschema")
public class AppSchemaAutoConfiguration {

    public @PostConstruct void log() {
        log.info("App-schema configuration detected");
    }

    /**
     * Creates the App-Schema module status bean.
     *
     * <p>Equivalent to the following XML configuration:
     *
     * <pre>
     * {@code
     * <bean id="appSchemaExtension" class=
     * "org.geoserver.platform.ModuleStatusImpl">
     *  <property name="module" value="gs-app-schema-core" />
     *  <property name="name" value="App Schema Core Extension"/>
     *  <property name="component" value="App Schema Core extension"/>
     *  <property name="available" value="true"/>
     *  <property name="enabled" value="true"/>
     * </bean>
     * }</pre>
     *
     * @param config the App-Schema configuration properties
     * @return the App-Schema module status bean
     */
    @Bean
    ModuleStatus appSchemaExtension(AppSchemaConfigProperties config) {
        ModuleStatusImpl mod =
                new ModuleStatusImpl("gs-app-schema-core", "App Schema Core Extension", "App Schema Core extension");
        mod.setAvailable(true);
        mod.setEnabled(config.isEnabled());
        if (config.isEnabled()) {
            mod.setMessage(
                    "App schema extension enabled through config property geoserver.extension.appschema.enabled=true");
        } else {
            mod.setMessage(
                    "App schema extension disabled through config property geoserver.extension.appschema.enabled=false");
        }

        return mod;
    }
}
