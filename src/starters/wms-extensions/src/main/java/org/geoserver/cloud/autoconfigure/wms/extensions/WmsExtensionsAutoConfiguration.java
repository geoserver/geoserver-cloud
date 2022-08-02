/*
 * (c) 2021 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.wms.extensions;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @since 1.0
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(WmsExtensionsConfigProperties.class)
@Import(
        value = {
            CssStylingConfiguration.class,
            MapBoxStylingConfiguration.class,
            VectorTilesConfiguration.class
        })
public class WmsExtensionsAutoConfiguration {}
