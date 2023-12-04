/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.web.demo;

import org.geoserver.cloud.autoconfigure.web.core.AbstractWebUIAutoConfiguration;
import org.geoserver.cloud.autoconfigure.web.demo.LayerPreviewConfiguration.GmlCommonFormatsConfiguration;
import org.geoserver.cloud.autoconfigure.web.demo.LayerPreviewConfiguration.KmlCommonFormatsConfiguration;
import org.geoserver.cloud.autoconfigure.web.demo.LayerPreviewConfiguration.OpenLayersCommonFormatsConfiguration;
import org.geoserver.cloud.config.factory.FilteringXmlBeanDefinitionReader;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;

@Configuration
@ConditionalOnClass(name = "org.geoserver.web.demo.MapPreviewPage")
@ConditionalOnProperty( // enabled by default
        prefix = LayerPreviewConfiguration.CONFIG_PREFIX,
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
@ImportResource( //
        reader = FilteringXmlBeanDefinitionReader.class, //
        locations = {"jar:gs-web-demo-.*!/applicationContext.xml#name=layerListDemo2"})
@Import({
    OpenLayersCommonFormatsConfiguration.class,
    GmlCommonFormatsConfiguration.class,
    KmlCommonFormatsConfiguration.class
})
public class LayerPreviewConfiguration extends AbstractWebUIAutoConfiguration {

    static final String CONFIG_PREFIX = "geoserver.web-ui.demos.layer-preview-page";
    static final String COMMON_FORMATS_PREFIX = CONFIG_PREFIX + ".common-formats";

    @Override
    public String getConfigPrefix() {
        return CONFIG_PREFIX;
    }

    @Configuration
    @ConditionalOnProperty(
            prefix = LayerPreviewConfiguration.COMMON_FORMATS_PREFIX,
            name = "open-layers",
            havingValue = "true",
            matchIfMissing = true)
    @ImportResource( //
            reader = FilteringXmlBeanDefinitionReader.class, //
            locations = {"jar:gs-web-demo-.*!/applicationContext.xml#name=openLayersPreview"})
    public class OpenLayersCommonFormatsConfiguration extends AbstractWebUIAutoConfiguration {

        static final String CONFIG_PREFIX =
                LayerPreviewConfiguration.COMMON_FORMATS_PREFIX + ".open-layers";

        @Override
        public String getConfigPrefix() {
            return CONFIG_PREFIX;
        }
    }

    @Configuration
    @ConditionalOnProperty(
            prefix = LayerPreviewConfiguration.COMMON_FORMATS_PREFIX,
            name = "gml",
            havingValue = "true",
            matchIfMissing = true)
    @ImportResource( //
            reader = FilteringXmlBeanDefinitionReader.class, //
            locations = {"jar:gs-web-demo-.*!/applicationContext.xml#name=gMLPreview"})
    public class GmlCommonFormatsConfiguration extends AbstractWebUIAutoConfiguration {

        static final String CONFIG_PREFIX =
                LayerPreviewConfiguration.COMMON_FORMATS_PREFIX + ".gml";

        @Override
        public String getConfigPrefix() {
            return CONFIG_PREFIX;
        }
    }

    @Configuration
    @ConditionalOnProperty(
            prefix = LayerPreviewConfiguration.COMMON_FORMATS_PREFIX,
            name = "kml",
            havingValue = "true",
            matchIfMissing = true)
    @ImportResource( //
            reader = FilteringXmlBeanDefinitionReader.class, //
            locations = {"jar:gs-web-demo-.*!/applicationContext.xml#name=kMLPreview"})
    public class KmlCommonFormatsConfiguration extends AbstractWebUIAutoConfiguration {

        static final String CONFIG_PREFIX =
                LayerPreviewConfiguration.COMMON_FORMATS_PREFIX + ".kml";

        @Override
        public String getConfigPrefix() {
            return CONFIG_PREFIX;
        }
    }
}
