/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.extensions.dxf;

import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.cloud.autoconfigure.extensions.ConditionalOnGeoServerWFS;
import org.geoserver.cloud.autoconfigure.extensions.ConditionalOnGeoServerWebUI;
import org.geoserver.cloud.config.factory.ImportFilteredResource;
import org.geoserver.platform.ModuleStatus;
import org.geoserver.platform.ModuleStatusImpl;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Auto-configuration for DXF extension across multiple GeoServer services.
 *
 * <p>
 * This auto-configuration enables the DXF extension in GeoServer Cloud,
 * allowing DXF to be used as a WFS output format and integrated with the WebUI.
 *
 * <p>
 * The configuration consists of the following components:
 * <ul>
 * <li>The main configuration class that registers module status indicators</li>
 * <li>A nested {@code DxfOutputFormatConfiguration} class that imports the WFS
 *     output format functionality</li>
 * <li>A nested {@code WebUIConfiguration} class that enables DXF in the
 *     WebUI for WFS admin pages and layer preview</li>
 * </ul>
 *
 * <p>
 * The nested configurations are activated when the following conditions are met:
 * <ul>
 * <li>The required DXFOutputFormat class is on the classpath
 *     ({@code ConditionalOnDxf})</li>
 * <li>The geoserver.extension.dxf.enabled property is true (the default)</li>
 * <li>The respective service is available (WFS, WebUI)</li>
 * </ul>
 *
 * @since 2.27.0
 */
@AutoConfiguration
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
@Import({DxfAutoConfiguration.DxfOutputFormatConfiguration.class, DxfAutoConfiguration.WebUIConfiguration.class})
@EnableConfigurationProperties(DxfConfigProperties.class)
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.extensions.dxf")
public class DxfAutoConfiguration {

    /**
     * Provides a ModuleStatus for the DXF extension.
     */
    @SuppressWarnings("java:S6830")
    @Bean("DxfExtension")
    @ConditionalOnMissingBean
    ModuleStatus dxfExtension(DxfConfigProperties config) {
        ModuleStatusImpl status = new ModuleStatusImpl("gs-dxf", "DXF Extension");
        status.setComponent("DXF extension");
        status.setAvailable(true);
        status.setEnabled(config.isEnabled());
        return status;
    }

    /**
     * Configuration class that enables DXF as a WFS output format.
     *
     * <p>
     * This configuration is only activated when both the DXF extension is enabled
     * (via {@code ConditionalOnDxf}) and the WFS service is available
     * (via {@code ConditionalOnGeoServerWFS}).
     *
     * <p>
     * When active, it imports the DXF output format beans defined in the
     * extension's applicationContext.xml, allowing WFS GetFeature requests to return
     * data in DXF format.
     */
    @Configuration
    @ConditionalOnDxf
    @ConditionalOnGeoServerWFS
    @ImportFilteredResource("jar:gs-dxf-core-.*!/applicationContext.xml#name=.*")
    public static class DxfOutputFormatConfiguration {
        @PostConstruct
        void log() {
            log.info("DXF WFS output format extension enabled");
        }
    }

    /**
     * Configuration class that enables DXF in the WebUI service.
     *
     * <p>
     * This configuration is activated when both the DXF extension is enabled
     * and the WebUI service is available. It enables:
     * <ul>
     * <li>DXF format option in the Layer Preview page</li>
     * <li>DXF format in the WFS service admin page</li>
     * <li>UI components for configuring and managing DXF outputs</li>
     * </ul>
     *
     * <p>
     * This demonstrates how a single extension can be required by multiple services
     * to provide a complete user experience.
     */
    @Configuration
    @ConditionalOnDxf
    @ConditionalOnGeoServerWebUI
    @ImportFilteredResource("jar:gs-dxf-core-.*!/applicationContext.xml#name=.*")
    public static class WebUIConfiguration {
        @PostConstruct
        void log() {
            log.info("DXF WebUI output format extension enabled");
        }
    }
}
