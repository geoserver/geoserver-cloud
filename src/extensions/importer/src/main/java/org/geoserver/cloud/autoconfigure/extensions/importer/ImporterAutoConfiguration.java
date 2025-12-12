/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.importer;

import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.cloud.autoconfigure.extensions.ConditionalOnGeoServerREST;
import org.geoserver.cloud.autoconfigure.extensions.ConditionalOnGeoServerWebUI;
import org.geoserver.configuration.extension.importer.ImporterCoreConfiguration;
import org.geoserver.configuration.extension.importer.ImporterRestConfiguration;
import org.geoserver.configuration.extension.importer.ImporterWebUIConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Auto-configuration for the GeoServer Importer extension.
 */
@AutoConfiguration
@ConditionalOnImporter
@EnableConfigurationProperties(ImporterConfigProperties.class)
@Import({
    ImporterAutoConfiguration.ImporterWebUIAutoConfiguration.class,
    ImporterAutoConfiguration.ImporterRestAutoConfiguration.class
})
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.extensions.importer")
public class ImporterAutoConfiguration {

    /**
     * Configuration for the Importer Web UI components.
     * @see ImporterCoreConfiguration
     * @see ImporterWebUIConfiguration
     */
    @Configuration
    @ConditionalOnImporter
    @ConditionalOnGeoServerWebUI
    @ConditionalOnClass(name = "org.geoserver.importer.web.ImporterConfigPage")
    @Import({ImporterCoreConfiguration.class, ImporterWebUIConfiguration.class})
    static class ImporterWebUIAutoConfiguration {
        @PostConstruct
        void log() {
            log.info("{} enabled", ImporterConfigProperties.PREFIX);
        }
    }

    /**
     * Configuration for the Importer REST API.
     * @see ImporterCoreConfiguration
     * @see ImporterRestConfiguration
     */
    @Configuration
    @ConditionalOnImporter
    @ConditionalOnGeoServerREST
    @ConditionalOnClass(name = "org.geoserver.importer.rest.ImportBaseController")
    @Import({ImporterCoreConfiguration.class, ImporterRestConfiguration.class})
    static class ImporterRestAutoConfiguration {
        @PostConstruct
        void log() {
            log.info("{} enabled", ImporterConfigProperties.PREFIX);
        }
    }
}
