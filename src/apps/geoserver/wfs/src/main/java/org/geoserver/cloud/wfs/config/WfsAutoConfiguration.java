/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.wfs.config;

import org.geoserver.catalog.Catalog;
import org.geoserver.cloud.autoconfigure.core.GeoServerWebMvcMainAutoConfiguration;
import org.geoserver.cloud.config.factory.FilteringXmlBeanDefinitionReader;
import org.geoserver.cloud.virtualservice.VirtualServiceVerifier;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportResource;

@AutoConfiguration(after = GeoServerWebMvcMainAutoConfiguration.class)
@ImportResource( //
        reader = FilteringXmlBeanDefinitionReader.class, //
        locations = {
            "jar:gs-wfs-.*!/applicationContext.xml#name=.*",
            "jar:gs-flatgeobuf-.*!/applicationContext.xml#name=.*",
            "jar:gs-dxf-core-.*!/applicationContext.xml#name=.*",
            "jar:gs-geopkg-output-.*!/applicationContext.xml#name=.*"
        } //
        )
public class WfsAutoConfiguration {

    @Bean
    VirtualServiceVerifier virtualServiceVerifier(@Qualifier("rawCatalog") Catalog catalog) {
        return new VirtualServiceVerifier(catalog);
    }
}
