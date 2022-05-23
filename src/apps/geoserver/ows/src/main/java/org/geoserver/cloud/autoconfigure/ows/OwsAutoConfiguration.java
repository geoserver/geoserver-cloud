/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.ows;

import org.geoserver.cloud.autoconfigure.core.GeoServerWebMvcMainAutoConfiguration;
import org.geoserver.cloud.ows.controller.OWSController;
import org.geoserver.cloud.virtualservice.VirtualServiceVerifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@AutoConfigureAfter(GeoServerWebMvcMainAutoConfiguration.class)
public class OwsAutoConfiguration {

    public @Bean VirtualServiceVerifier virtualServiceVerifier() {
        return new VirtualServiceVerifier();
    }

    public @Bean OWSController genericOWSServiceController() {
        return new OWSController();
    }
}
