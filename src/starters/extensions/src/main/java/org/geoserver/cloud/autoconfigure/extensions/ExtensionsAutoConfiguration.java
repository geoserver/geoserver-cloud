/*
 * (c) 2025 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions;

import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 *
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ExtensionsConfigProperties.class)
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.extensions")
@Import(value = {AppSchemaConfiguration.class})
public class ExtensionsAutoConfiguration {
    public @PostConstruct void log() {
        log.info("Extensions configuration detected");
    }
}
