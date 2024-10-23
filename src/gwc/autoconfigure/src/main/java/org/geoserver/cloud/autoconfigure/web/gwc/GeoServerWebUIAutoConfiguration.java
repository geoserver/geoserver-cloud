/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.web.gwc;

import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.cloud.autoconfigure.gwc.ConditionalOnGeoServerWebUIEnabled;
import org.geoserver.cloud.autoconfigure.gwc.GoServerWebUIConfigurationProperties;
import org.geoserver.cloud.config.factory.FilteringXmlBeanDefinitionReader;
import org.geoserver.config.GeoServer;
import org.geoserver.gwc.GWC;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

/**
 * Auto configuration to enabled GWC Wicket Web UI components.
 *
 * <p>Conditionals:
 *
 * <ul>
 *   <li>The {@literal gs-web-gwc} jar is in the classpath
 *   <li>{@literal gwc.enabled=true}: Core gwc integration is enabled
 *   <li>{@literal geoserver.web-ui.gwc.=true}: gwc web-ui integration is enabled
 * </ul>
 *
 * @since 1.0
 */
@Configuration
@ConditionalOnGeoServerWebUIEnabled
@EnableConfigurationProperties(GoServerWebUIConfigurationProperties.class)
@ImportResource( //
        reader = FilteringXmlBeanDefinitionReader.class,
        locations = {
            "jar:gs-web-gwc-.*!/applicationContext.xml#name=^(?!"
                    + GeoServerWebUIAutoConfiguration.EXCLUDED_BEANS
                    + ").*$"
        })
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.web.gwc")
public class GeoServerWebUIAutoConfiguration {

    /*
     * Disable disk-quota by brute force for now. We need to resolve how and where to store the
     * configuration and database.
     */
    static final String EXCLUDED_BEANS = "diskQuotaMenuPage|wmtsServiceDescriptor";

    public @PostConstruct void log() {
        log.info("{} enabled", GoServerWebUIConfigurationProperties.GWC_WEBUI_ENABLED_PROPERTY);
    }

    @Bean
    CloudGWCServiceDescriptionProvider cloudGWCServiceDescriptionProvider(
            GWC gwc, GeoServer gs, GoServerWebUIConfigurationProperties staticConfig) {
        return new CloudGWCServiceDescriptionProvider(gwc, gs, staticConfig);
    }
}
