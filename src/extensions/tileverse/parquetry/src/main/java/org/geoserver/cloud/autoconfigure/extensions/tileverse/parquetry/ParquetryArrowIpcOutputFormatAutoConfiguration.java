/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.tileverse.parquetry;

import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.cloud.autoconfigure.extensions.ConditionalOnGeoServerWFS;
import org.geoserver.cloud.autoconfigure.extensions.ConditionalOnGeoServerWebUI;
import org.geoserver.cloud.config.factory.ImportFilteredResource;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Auto-configuration for the Parquetry plugin's {@code arrow-ipc} WFS GetFeature output format.
 *
 * <p>Imports the output format and its module status from the plugin's {@code applicationContext.xml}, in the
 * services that serve or list WFS output formats:
 *
 * <ul>
 *   <li>WFS, where GetFeature answers requests for the format
 *   <li>WebUI, where Layer Preview and the WFS service page list it
 * </ul>
 *
 * <p>Active when {@link ConditionalOnParquetryArrowIpcOutputFormat} holds: the Parquetry plugin is on and
 * {@code geoserver.extension.tileverse.parquetry.output-formats.arrow-ipc.enabled} is {@code true} (the default).
 *
 * @since 2.28.7
 */
@AutoConfiguration
@ConditionalOnParquetryArrowIpcOutputFormat
@Import({
    ParquetryArrowIpcOutputFormatAutoConfiguration.WfsConfiguration.class,
    ParquetryArrowIpcOutputFormatAutoConfiguration.WebUIConfiguration.class
})
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.extensions.tileverse.parquetry")
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
public class ParquetryArrowIpcOutputFormatAutoConfiguration {

    static final String OUTPUT_FORMAT_BEANS =
            "jar:tileverse-geoserver-parquetry-.*!/applicationContext.xml#name=^(arrowIpcOutputFormat|arrowIpcWfsOutputFormatModuleStatus)$";

    /** Registers the output format in the WFS service. */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnGeoServerWFS
    @ImportFilteredResource(OUTPUT_FORMAT_BEANS)
    static class WfsConfiguration {

        @PostConstruct
        void log() {
            log.info("Parquetry arrow-ipc WFS output format enabled");
        }
    }

    /** Registers the output format in the WebUI service, for Layer Preview and the WFS service page. */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnGeoServerWebUI
    @ImportFilteredResource(OUTPUT_FORMAT_BEANS)
    static class WebUIConfiguration {

        @PostConstruct
        void log() {
            log.info("Parquetry arrow-ipc WFS output format enabled in the WebUI");
        }
    }
}
