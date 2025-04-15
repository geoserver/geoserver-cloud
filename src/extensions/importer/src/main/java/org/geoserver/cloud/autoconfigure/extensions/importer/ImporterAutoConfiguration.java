/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.importer;

import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.cloud.autoconfigure.extensions.ConditionalOnGeoServerREST;
import org.geoserver.cloud.autoconfigure.extensions.ConditionalOnGeoServerWebUI;
import org.geoserver.cloud.config.factory.ImportFilteredResource;
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
    ImporterAutoConfiguration.ImporterWebUIConfiguration.class,
    ImporterAutoConfiguration.ImporterRestConfiguration.class
})
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.extensions.importer")
public class ImporterAutoConfiguration {

    /**
     * Configuration for the Importer Web UI components.
     */
    @Configuration
    @ConditionalOnImporter
    @ConditionalOnGeoServerWebUI
    @ConditionalOnClass(name = "org.geoserver.importer.web.ImporterConfigPage")
    @ImportFilteredResource({
        "jar:gs-importer-core-.*!/applicationContext.xml",
        "jar:gs-importer-web-.*!/applicationContext.xml"
    })
    static class ImporterWebUIConfiguration {
        @PostConstruct
        void log() {
            log.info("{} enabled", ImporterConfigProperties.PREFIX);
        }
    }

    /**
     * Configuration for the Importer REST API.
     */
    @Configuration
    @ConditionalOnImporter
    @ConditionalOnGeoServerREST
    @ConditionalOnClass(name = "org.geoserver.importer.rest.ImportBaseController")
    @ImportFilteredResource({
        "jar:gs-importer-core-.*!/applicationContext.xml",
        "jar:gs-importer-rest-.*!/applicationContext.xml"
    })
    static class ImporterRestConfiguration {
        @PostConstruct
        void log() {
            log.info("{} enabled", ImporterConfigProperties.PREFIX);
        }
    }
}
