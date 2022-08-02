/*
 * (c) 2021 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.wms.extensions;

import org.geoserver.cloud.autoconfigure.wms.extensions.WmsExtensionsConfigProperties.Wms.WmsOutputFormatsConfigProperties.VectorTilesConfigProperties;
import org.geoserver.cloud.config.factory.FilteringXmlBeanDefinitionReader;
import org.geoserver.wms.vector.VectorTileMapOutputFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

import javax.annotation.PostConstruct;

/**
 * @since 1.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(VectorTileMapOutputFormat.class)
@EnableConfigurationProperties(WmsExtensionsConfigProperties.class)
@ImportResource( //
        reader = FilteringXmlBeanDefinitionReader.class, //
        locations = {"jar:gs-vectortiles-.*!/applicationContext.xml#name=(VectorTilesExtension)"})
class VectorTilesConfiguration {

    static final String PREFIX = "geoserver.wms.output-formats.vector-tiles";

    private @Autowired @Qualifier("VectorTilesExtension") org.geoserver.platform.ModuleStatusImpl
            extensionInfo;
    private @Autowired WmsExtensionsConfigProperties config;

    public @PostConstruct void init() {
        VectorTilesConfigProperties vt = config.getWms().getOutputFormats().getVectorTiles();
        extensionInfo.setEnabled(vt.anyEnabled());
    }

    @ConditionalOnProperty(
            prefix = PREFIX + ".mapbox",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true)
    @ImportResource( //
            reader = FilteringXmlBeanDefinitionReader.class, //
            locations = {
                "jar:gs-vectortiles-.*!/applicationContext.xml#name=(wmsMapBoxBuilderFactory|wmsMapBoxMapOutputFormat)"
            })
    static @Configuration class MapBox {}

    @ConditionalOnProperty(
            prefix = PREFIX + ".geojson",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true)
    @ImportResource( //
            reader = FilteringXmlBeanDefinitionReader.class, //
            locations = {
                "jar:gs-vectortiles-.*!/applicationContext.xml#name=(wmsGeoJsonBuilderFactory|wmsGeoJsonMapOutputFormat)"
            })
    static @Configuration class GeoJson {}

    @ConditionalOnProperty(
            prefix = PREFIX + ".topojson",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true)
    @ImportResource( //
            reader = FilteringXmlBeanDefinitionReader.class, //
            locations = {
                "jar:gs-vectortiles-.*!/applicationContext.xml#name=(wmsTopoJSONBuilderFactory|wmsTopoJSONMapOutputFormat)"
            })
    static @Configuration class TopoJson {}
}
