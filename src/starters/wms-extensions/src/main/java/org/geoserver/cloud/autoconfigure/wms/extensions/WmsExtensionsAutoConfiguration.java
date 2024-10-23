/*
 * (c) 2021 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.wms.extensions;

import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @since 1.0
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(WmsExtensionsConfigProperties.class)
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.wms.extension")
@Import(value = {CssStylingConfiguration.class, MapBoxStylingConfiguration.class, VectorTilesConfiguration.class})
public class WmsExtensionsAutoConfiguration {
    public @PostConstruct void log() {
        log.info("WMS extensions configuration detected");
    }
}
