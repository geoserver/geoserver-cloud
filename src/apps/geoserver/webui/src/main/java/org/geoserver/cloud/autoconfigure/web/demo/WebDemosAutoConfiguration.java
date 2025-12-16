/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.web.demo;

import lombok.Getter;
import org.geoserver.cloud.autoconfigure.web.core.AbstractWebUIAutoConfiguration;
import org.geoserver.configuration.core.web.WebDemoLayerPreviewConfiguration;
import org.geoserver.configuration.core.web.WebDemoRequestsConfiguration;
import org.geoserver.configuration.core.web.demo.LayerPreviewGmlConfiguration;
import org.geoserver.configuration.core.web.demo.LayerPreviewKmlConfiguration;
import org.geoserver.configuration.core.web.demo.LayerPreviewOpenLayersConfiguration;
import org.geoserver.configuration.core.web.demo.ReprojectionConsoleConfiguration;
import org.geoserver.configuration.core.web.demo.SRSListConfiguration;
import org.geoserver.configuration.core.web.demo.WCSRequestBuilderConfiguration;
import org.geoserver.configuration.core.web.demo.WPSRequestBuilderConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;

/**
 * Auto configuration for demo web ui components.
 * <p>
 *
 * The following transpiled configurations are imported by this class' inner configurations based on their conditionals:
 * <ul>
 * <li> {@link ReprojectionConsoleConfiguration}
 * <li> {@link WebDemoRequestsConfiguration}
 * <li> {@link WebDemoLayerPreviewConfiguration}
 * <li> {@link LayerPreviewOpenLayersConfiguration}
 * <li> {@link LayerPreviewGmlConfiguration}
 * <li> {@link LayerPreviewKmlConfiguration}
 * <li> {@link ReprojectionConsoleConfiguration}
 * <li> {@link WCSRequestBuilderConfiguration}
 * <li> {@link WPSRequestBuilderConfiguration}
 * </ul>
 *
 * (see {@code WebDemoConfigurationTranspileAggregator})
 */
@AutoConfiguration
@ConditionalOnClass(name = "org.geoserver.web.demo.DemoRequest")
@ConditionalOnProperty( // enabled by default
        name = "geoserver.web-ui.demos.enabled",
        havingValue = "true",
        matchIfMissing = true)
@Import({
    WebDemosAutoConfiguration.LayerPreview.class,
    WebDemosAutoConfiguration.DemoRequests.class,
    WebDemosAutoConfiguration.ReprojectionConsole.class,
    WebDemosAutoConfiguration.SRSList.class,
    WebDemosAutoConfiguration.WCSRequestBuilder.class,
    WebDemosAutoConfiguration.WPSRequestBuilder.class
})
public class WebDemosAutoConfiguration {

    /**
     * @see WebDemoRequestsConfiguration
     */
    @AutoConfiguration
    @ConditionalOnClass(name = "org.geoserver.web.demo.SRSListPage")
    @ConditionalOnProperty( // enabled by default
            name = "geoserver.web-ui.demos.demo-requests",
            havingValue = "true",
            matchIfMissing = true)
    @Import(WebDemoRequestsConfiguration.class)
    static class DemoRequests extends AbstractWebUIAutoConfiguration {
        @Getter
        private final String configPrefix = "geoserver.web-ui.demos.demo-requests";
    }

    @AutoConfiguration
    @ConditionalOnClass(name = "org.geoserver.web.demo.SRSListPage")
    @ConditionalOnProperty(
            name = "geoserver.web-ui.demos.reprojection-console",
            havingValue = "true",
            matchIfMissing = true)
    @Import(ReprojectionConsoleConfiguration.class)
    static class ReprojectionConsole extends AbstractWebUIAutoConfiguration {
        @Getter
        private final String configPrefix = "geoserver.web-ui.demos.reprojection-console";
    }

    @AutoConfiguration
    @ConditionalOnClass(name = "org.geoserver.web.demo.SRSListPage")
    @ConditionalOnProperty(name = "geoserver.web-ui.demos.srs-list", havingValue = "true", matchIfMissing = true)
    @Import(SRSListConfiguration.class)
    class SRSList extends AbstractWebUIAutoConfiguration {
        @Getter
        private final String configPrefix = "geoserver.web-ui.demos.srs-list";
    }

    @AutoConfiguration
    @ConditionalOnClass(name = "org.geoserver.wcs.web.demo.WCSRequestBuilder")
    @ConditionalOnProperty(
            name = "geoserver.web-ui.demos.wcs-request-builder",
            havingValue = "true",
            matchIfMissing = true)
    @Import(WCSRequestBuilderConfiguration.class)
    class WCSRequestBuilder extends AbstractWebUIAutoConfiguration {
        @Getter
        private final String configPrefix = "geoserver.web-ui.demos.wcs-request-builder";
    }

    @AutoConfiguration
    @ConditionalOnClass(name = "org.geoserver.wps.web.WPSRequestBuilder")
    @ConditionalOnProperty(
            name = "geoserver.web-ui.demos.wps-request-builder",
            havingValue = "true",
            matchIfMissing = true)
    @Import(WPSRequestBuilderConfiguration.class)
    class WPSRequestBuilder extends AbstractWebUIAutoConfiguration {
        @Getter
        private final String configPrefix = "geoserver.web-ui.demos..wps-request-builder";
    }

    /**
     * @see WebDemoLayerPreviewConfiguration
     * @see LayerPreviewOpenLayersConfiguration
     * @see LayerPreviewKmlConfiguration
     * @see LayerPreviewGmlConfiguration
     */
    @AutoConfiguration
    @ConditionalOnClass(name = "org.geoserver.web.demo.MapPreviewPage")
    @ConditionalOnProperty(
            name = "geoserver.web-ui.demos.layer-preview-page.enabled",
            havingValue = "true",
            matchIfMissing = true)
    @Import({
        WebDemoLayerPreviewConfiguration.class,
        LayerPreview.OpenLayers.class,
        LayerPreview.GML.class,
        LayerPreview.KML.class
    })
    static class LayerPreview extends AbstractWebUIAutoConfiguration {

        @Override
        public String getConfigPrefix() {
            return "geoserver.web-ui.demos.layer-preview-page";
        }

        @AutoConfiguration
        @Import(LayerPreviewOpenLayersConfiguration.class)
        @ConditionalOnProperty(
                name = "geoserver.web-ui.demos.layer-preview-page.openlayers",
                havingValue = "true",
                matchIfMissing = true)
        static class OpenLayers extends AbstractWebUIAutoConfiguration {
            @Getter
            private final String configPrefix = "geoserver.web-ui.demos.layer-preview-page.openlayers";
        }

        @AutoConfiguration
        @Import(LayerPreviewGmlConfiguration.class)
        @ConditionalOnProperty(
                name = "geoserver.web-ui.demos.layer-preview-page.gml",
                havingValue = "true",
                matchIfMissing = true)
        static class GML extends AbstractWebUIAutoConfiguration {
            @Getter
            private final String configPrefix = "geoserver.web-ui.demos.layer-preview-page.gml";
        }

        @AutoConfiguration
        @Import(LayerPreviewKmlConfiguration.class)
        @ConditionalOnProperty(
                name = "geoserver.web-ui.demos.layer-preview-page.kml",
                havingValue = "true",
                matchIfMissing = true)
        static class KML extends AbstractWebUIAutoConfiguration {
            @Getter
            private final String configPrefix = "geoserver.web-ui.demos.layer-preview-page.kml";
        }
    }
}
