/*
 * (c) 2021 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.wms.extensions;

import org.geoserver.cloud.autoconfigure.wms.extensions.WmsExtensionsConfigProperties.Wms.WmsOutputFormatsConfigProperties.VectorTilesConfigProperties;
import org.geoserver.cloud.config.factory.FilteringXmlBeanDefinitionReader;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

/**
 * @since 1.0
 */
@Configuration
@EnableConfigurationProperties(WmsExtensionsConfigProperties.class)
@ImportResource( //
        reader = FilteringXmlBeanDefinitionReader.class, //
        locations = {"jar:gs-geopkg-output-.*!/applicationContext.xml"})
class GeoPkgOutputConfiguration {

    @ConditionalOnProperty(
            name = "geoserver.wms.output-formats.geopkg.enabled",
            havingValue = "true",
            matchIfMissing = true)
    @ImportResource( //
            reader = FilteringXmlBeanDefinitionReader.class, //
            locations = {
                "jar:gs-geopkg-output-.*!/applicationContext.xml"
            })
    static class Enabled {}
}
