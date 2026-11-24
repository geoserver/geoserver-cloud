/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.web.demo;

import org.geoserver.cloud.autoconfigure.web.core.AbstractWebUIAutoConfiguration;
import org.geoserver.cloud.autoconfigure.web.demo.LayerPreviewAutoConfiguration.GmlCommonFormatsConfiguration;
import org.geoserver.cloud.autoconfigure.web.demo.LayerPreviewAutoConfiguration.KmlCommonFormatsConfiguration;
import org.geoserver.cloud.autoconfigure.web.demo.LayerPreviewAutoConfiguration.OpenLayersCommonFormatsConfiguration;
import org.geoserver.configuration.core.web.WebDemoLayerPreviewConfiguration;
import org.geoserver.configuration.core.web.demo.LayerPreviewGmlConfiguration;
import org.geoserver.configuration.core.web.demo.LayerPreviewKmlConfiguration;
import org.geoserver.configuration.core.web.demo.LayerPreviewOpenLayersConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @see WebDemoLayerPreviewConfiguration
 * @see LayerPreviewOpenLayersConfiguration
 * @see LayerPreviewGmlConfiguration
 * @see LayerPreviewKmlConfiguration
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(name = "org.geoserver.web.demo.MapPreviewPage")
@ConditionalOnProperty( // enabled by default
        prefix = LayerPreviewAutoConfiguration.CONFIG_PREFIX,
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
@Import({
    // layer preview menu link
    WebDemoLayerPreviewConfiguration.class,
    // openlayers common format with conditional
    OpenLayersCommonFormatsConfiguration.class,
    // GML common format with conditional
    GmlCommonFormatsConfiguration.class,
    // KML common format with conditional
    KmlCommonFormatsConfiguration.class
})
public class LayerPreviewAutoConfiguration extends AbstractWebUIAutoConfiguration {

    static final String CONFIG_PREFIX = "geoserver.web-ui.demos.layer-preview-page";
    static final String COMMON_FORMATS_PREFIX = CONFIG_PREFIX + ".common-formats";

    @Override
    public String getConfigPrefix() {
        return CONFIG_PREFIX;
    }

    @Configuration
    @ConditionalOnProperty(
            prefix = LayerPreviewAutoConfiguration.COMMON_FORMATS_PREFIX,
            name = "open-layers",
            havingValue = "true",
            matchIfMissing = true)
    @Import(LayerPreviewOpenLayersConfiguration.class)
    public class OpenLayersCommonFormatsConfiguration extends AbstractWebUIAutoConfiguration {

        static final String CONFIG_PREFIX = LayerPreviewAutoConfiguration.COMMON_FORMATS_PREFIX + ".open-layers";

        @Override
        public String getConfigPrefix() {
            return CONFIG_PREFIX;
        }
    }

    @Configuration
    @ConditionalOnProperty(
            prefix = LayerPreviewAutoConfiguration.COMMON_FORMATS_PREFIX,
            name = "gml",
            havingValue = "true",
            matchIfMissing = true)
    @Import(LayerPreviewGmlConfiguration.class)
    public class GmlCommonFormatsConfiguration extends AbstractWebUIAutoConfiguration {

        static final String CONFIG_PREFIX = LayerPreviewAutoConfiguration.COMMON_FORMATS_PREFIX + ".gml";

        @Override
        public String getConfigPrefix() {
            return CONFIG_PREFIX;
        }
    }

    @Configuration
    @ConditionalOnProperty(
            prefix = LayerPreviewAutoConfiguration.COMMON_FORMATS_PREFIX,
            name = "kml",
            havingValue = "true",
            matchIfMissing = true)
    @Import(LayerPreviewKmlConfiguration.class)
    public class KmlCommonFormatsConfiguration extends AbstractWebUIAutoConfiguration {

        static final String CONFIG_PREFIX = LayerPreviewAutoConfiguration.COMMON_FORMATS_PREFIX + ".kml";

        @Override
        public String getConfigPrefix() {
            return CONFIG_PREFIX;
        }
    }
}
