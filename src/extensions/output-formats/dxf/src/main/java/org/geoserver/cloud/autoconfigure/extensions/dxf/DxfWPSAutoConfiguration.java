/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.extensions.dxf;

import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.cloud.autoconfigure.extensions.ConditionalOnGeoServerWPS;
import org.geoserver.cloud.autoconfigure.extensions.ConditionalOnGeoServerWebUI;
import org.geoserver.configuration.extension.dxf.DxfWpsConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Auto-configuration for DXF WPS extension.
 *
 * @since 2.28.0, previously imported directly in web-ui and wps apps
 */
@AutoConfiguration
@Import({ //
    DxfWPSAutoConfiguration.WPS.class,
    DxfWPSAutoConfiguration.WEBUI.class
})
@EnableConfigurationProperties(DxfConfigProperties.class)
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.extensions.dxf")
public class DxfWPSAutoConfiguration {

    @Configuration
    @ConditionalOnDxfWPS
    @ConditionalOnGeoServerWPS
    @Import(DxfWpsConfiguration.class)
    public static class WPS {
        @PostConstruct
        void log() {
            log.info("DXF WPS extension enabled");
        }
    }

    @AutoConfiguration
    @ConditionalOnDxfWPS
    @ConditionalOnGeoServerWebUI
    @Import(DxfWpsConfiguration.class)
    public static class WEBUI {
        @PostConstruct
        void log() {
            log.info("DXF WPS extension enabled");
        }
    }
}
