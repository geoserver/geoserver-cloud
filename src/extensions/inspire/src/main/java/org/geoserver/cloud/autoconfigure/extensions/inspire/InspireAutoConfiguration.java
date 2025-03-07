/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.inspire;

import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.platform.ModuleStatus;
import org.geoserver.platform.ModuleStatusImpl;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for the GeoServer INSPIRE extension.
 *
 * <p>This extension enables some INSPIRE metadata features. It allows
 * GeoServer to configure INSPIRE metadata that can be served by
 * in GetCapabilities.
 *
 * <p>The extension is disabled by default and can be enabled with:
 *
 * <pre>{@code
 * geoserver:
 *   extension:
 *     inspire:
 *       enabled: true
 * }</pre>
 *
 * @since 2.27.0.0
 */
@AutoConfiguration
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
@EnableConfigurationProperties(InspireConfigProperties.class)
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.extensions.inspire")
public class InspireAutoConfiguration {

    public @PostConstruct void log() {
        log.info("INSPIRE configuration detected");
    }

    /**
     * Creates the INSPIRE module status bean.
     *
     * <p>Equivalent to the following XML configuration:
     *
     * <pre>
     * {@code
     * <bean id="inspireExtension" class=
     * "org.geoserver.platform.ModuleStatusImpl">
     *  <property name="module" value="gs-inspire" />
     *  <property name="name" value="INSPIRE Extension"/>
     *  <property name="component" value="INSPIRE extension"/>
     *  <property name="available" value="true"/>
     *  <property name="enabled" value="true"/>
     * </bean>
     * }</pre>
     *
     * @param config the INSPIRE configuration properties
     * @return the INSPIRE module status bean
     */
    @Bean
    ModuleStatus inspireExtension(InspireConfigProperties config) {
        ModuleStatusImpl mod = new ModuleStatusImpl("gs-inspire", "INSPIRE Extension", "INSPIRE extension");
        mod.setAvailable(true);
        mod.setEnabled(config.isEnabled());
        if (config.isEnabled()) {
            mod.setMessage(
                    "INSPIRE extension enabled through config property geoserver.extension.inspire.enabled=true");
        } else {
            mod.setMessage(
                    "INSPIRE extension disabled through config property geoserver.extension.inspire.enabled=false");
        }

        return mod;
    }
}
