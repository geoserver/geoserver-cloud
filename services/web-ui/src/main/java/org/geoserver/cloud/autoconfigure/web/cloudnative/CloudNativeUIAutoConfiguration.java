/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.web.cloudnative;

import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.cloud.web.service.WebUiCloudServicesConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/** @since 1.0 */
@Configuration(proxyBeanMethods = false)
@Import(WebUiCloudServicesConfiguration.class)
@Slf4j
public class CloudNativeUIAutoConfiguration {

    public @PostConstruct void log() {
        log.info("cloud native webui components enabled");
    }
}
