/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.extensions.flatgeobuf;

import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.cloud.autoconfigure.extensions.ConditionalOnGeoServerWFS;
import org.geoserver.cloud.autoconfigure.extensions.ConditionalOnGeoServerWebUI;
import org.geoserver.cloud.config.factory.ImportFilteredResource;
import org.geoserver.platform.ModuleStatus;
import org.geoserver.platform.ModuleStatusImpl;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Auto-configuration for FlatGeobuf extension across multiple GeoServer services.
 *
 * <p>
 * This auto-configuration enables the FlatGeobuf extension in GeoServer Cloud,
 * allowing FlatGeobuf to be used as a WFS output format and integrated with the WebUI.
 * It serves as an example of a module that's required by multiple services.
 *
 * <p>
 * The configuration consists of the following components:
 * <ul>
 * <li>The main configuration class that registers module status indicators</li>
 * <li>A nested {@code FlatGeobufOutputFormatConfiguration} class that imports the WFS
 *     output format functionality</li>
 * <li>A nested {@code WebUIConfiguration} class that enables FlatGeobuf in the
 *     WebUI for WFS admin pages and layer preview</li>
 * </ul>
 *
 * <p>
 * The nested configurations are activated when the following conditions are met:
 * <ul>
 * <li>The required FlatGeobufOutputFormat class is on the classpath
 *     ({@code ConditionalOnFlatGeobuf})</li>
 * <li>The geoserver.extension.flatgeobuf.enabled property is true (the default)</li>
 * <li>The respective service is available (WFS, WebUI)</li>
 * </ul>
 *
 * <p>
 * Multi-service integration:
 * <ul>
 * <li>WFS - FlatGeobuf is offered as an output format for GetFeature requests</li>
 * <li>WebUI - Format is available in the Layer Preview page and WFS admin UI</li>
 * </ul>
 *
 * <p>
 * The FlatGeobuf extension provides a compact binary format for vector data
 * with random access capabilities, making it efficient for large datasets.
 *
 * @since 2.27.0
 */
@AutoConfiguration
@Import({
    FlatGeobufAutoConfiguration.FlatGeobufOutputFormatConfiguration.class,
    FlatGeobufAutoConfiguration.WebUIConfiguration.class
})
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
@EnableConfigurationProperties(FlatGeobufConfigProperties.class)
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.extensions.flatgeobuf")
public class FlatGeobufAutoConfiguration {

    /**
     * Provides a ModuleStatus for the FlatGeobuf extension.
     */
    @Bean("flatGeobufExtension")
    ModuleStatus flatGeobufExtension(FlatGeobufConfigProperties config) {
        ModuleStatusImpl status = new ModuleStatusImpl("flatgeobuf", "FlatGeobuf WFS Output Format");
        status.setAvailable(true);
        status.setEnabled(config.isEnabled());
        return status;
    }

    /**
     * Configuration class that enables FlatGeobuf as a WFS output format.
     *
     * <p>
     * This configuration is only activated when both the FlatGeobuf extension is enabled
     * (via {@code ConditionalOnFlatGeobuf}) and the WFS service is available
     * (via {@code ConditionalOnGeoServerWFS}).
     *
     * <p>
     * When active, it imports the FlatGeobuf output format beans defined in the
     * extension's applicationContext.xml, allowing WFS GetFeature requests to return
     * data in FlatGeobuf format.
     */
    @Configuration
    @ConditionalOnFlatGeobuf
    @ConditionalOnGeoServerWFS
    @ImportFilteredResource("jar:gs-flatgeobuf-.*!/applicationContext.xml#name=.*")
    public static class FlatGeobufOutputFormatConfiguration {
        @PostConstruct
        void log() {
            log.info("FlatGeobuf WFS output format enabled");
        }
    }

    /**
     * Configuration class that enables FlatGeobuf in the WebUI service.
     *
     * <p>
     * This configuration is activated when both the FlatGeobuf extension is enabled
     * and the WebUI service is available. It enables:
     * <ul>
     * <li>FlatGeobuf format option in the Layer Preview page</li>
     * <li>FlatGeobuf format in the WFS service admin page</li>
     * <li>UI components for configuring and managing FlatGeobuf outputs</li>
     * </ul>
     *
     * <p>
     * This demonstrates how a single extension can be required by multiple services
     * to provide a complete user experience.
     */
    @Configuration
    @ConditionalOnFlatGeobuf
    @ConditionalOnGeoServerWebUI
    @ImportFilteredResource("jar:gs-flatgeobuf-.*!/applicationContext.xml#name=.*")
    public static class WebUIConfiguration {
        @PostConstruct
        void log() {
            log.info("FlatGeobuf WebUI output format extension enabled");
        }
    }
}
