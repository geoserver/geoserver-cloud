/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.web.core;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Configuration properties for GeoServer Web UI modules.
 * Maps to all {@code @ConditionalOnProperty} conditions used in web-ui auto-configurations.
 */
@Data
@ConfigurationProperties(prefix = "geoserver.web-ui")
public class WebUIConfigurationProperties {

    /**
     * Basic component configuration with enabled flag.
     */
    @Data
    public static class ComponentConfig {
        /**
         * Flag to enable/disable the component.
         */
        private boolean enabled = true;
    }

    /**
     * File browser configuration.
     */
    @Data
    public static class FileBrowserConfig {
        /**
         * Flag to hide the file system in the file browser.
         */
        private boolean hideFileSystem = false;
    }

    /**
     * GeoWebCache UI configuration.
     */
    @Data
    public static class GwcConfig {
        /**
         * Flag to enable/disable GWC UI.
         */
        private boolean enabled = true;

        /**
         * GWC capabilities configuration.
         */
        @NestedConfigurationProperty
        private CapabilitiesConfig capabilities = new CapabilitiesConfig();

        /**
         * GWC capabilities configuration.
         */
        @Data
        public static class CapabilitiesConfig {
            /**
             * Flag to enable/disable TMS capabilities.
             */
            private boolean tms = true;

            /**
             * Flag to enable/disable WMTS capabilities.
             */
            private boolean wmts = true;

            /**
             * Flag to enable/disable WMSC capabilities.
             */
            private boolean wmsc = true;
        }
    }

    /**
     * Security UI configuration.
     * (Maps to {@code geoserver.web-ui.security}).
     */
    @NestedConfigurationProperty
    private ComponentConfig security = new ComponentConfig();

    /**
     * WFS UI configuration.
     * (Maps to {@code geoserver.web-ui.wfs}).
     */
    @NestedConfigurationProperty
    private ComponentConfig wfs = new ComponentConfig();

    /**
     * WMS UI configuration.
     * (Maps to {@code geoserver.web-ui.wms}).
     */
    @NestedConfigurationProperty
    private ComponentConfig wms = new ComponentConfig();

    /**
     * WCS UI configuration.
     * (Maps to {@code geoserver.web-ui.wcs}).
     */
    @NestedConfigurationProperty
    private ComponentConfig wcs = new ComponentConfig();

    /**
     * WPS UI configuration.
     * (Maps to {@code geoserver.web-ui.wps}).
     */
    @NestedConfigurationProperty
    private ComponentConfig wps = new ComponentConfig();

    /**
     * GWC UI configuration.
     * (Maps to {@code geoserver.web-ui.gwc}).
     */
    @NestedConfigurationProperty
    private GwcConfig gwc = new GwcConfig();

    /**
     * ACL UI configuration.
     * (Maps to {@code geoserver.web-ui.acl}).
     */
    @NestedConfigurationProperty
    private ComponentConfig acl = new ComponentConfig();

    /**
     * File browser UI configuration.
     * (Maps to {@code geoserver.web-ui.file-browser}).
     */
    @NestedConfigurationProperty
    private FileBrowserConfig fileBrowser = new FileBrowserConfig();

    /**
     * Demos UI configuration.
     */
    @NestedConfigurationProperty
    private Demos demos = new Demos();

    /**
     * Tools UI configuration.
     */
    @NestedConfigurationProperty
    private Tools tools = new Tools();

    /**
     * Configuration for demo UI components.
     */
    @Data
    public static class Demos {
        /**
         * Flag to enable/disable demos UI.
         * (Maps to {@code geoserver.web-ui.demos.enabled}).
         */
        private boolean enabled = true;

        /**
         * Flag to enable/disable WPS request builder.
         * (Maps to {@code geoserver.web-ui.demos.wps-request-builder}).
         */
        private boolean wpsRequestBuilder = true;

        /**
         * Flag to enable/disable WCS request builder.
         * (Maps to {@code geoserver.web-ui.demos.wcs-request-builder}).
         */
        private boolean wcsRequestBuilder = true;

        /**
         * Flag to enable/disable demo requests.
         * (Maps to {@code geoserver.web-ui.demos.demo-requests}).
         */
        private boolean demoRequests = true;

        /**
         * Flag to enable/disable SRS list.
         * (Maps to {@code geoserver.web-ui.demos.srs-list}).
         */
        private boolean srsList = true;

        /**
         * Flag to enable/disable reprojection console.
         * (Maps to {@code geoserver.web-ui.demos.reprojection-console}).
         */
        private boolean reprojectionConsole = true;

        /**
         * Layer preview configuration.
         * (Maps to {@code geoserver.web-ui.demos.layer-preview-page}).
         */
        @NestedConfigurationProperty
        private LayerPreviewPageConfig layerPreviewPage = new LayerPreviewPageConfig();

        /**
         * Configuration for layer preview page.
         */
        @Data
        public static class LayerPreviewPageConfig {
            /**
             * Flag to enable/disable layer preview page.
             * (Maps to {@code geoserver.web-ui.demos.layer-preview-page.enabled}).
             */
            private boolean enabled = true;

            /**
             * Layer preview common formats configuration.
             * (Maps to {@code geoserver.web-ui.demos.layer-preview-page.common-formats}).
             */
            @NestedConfigurationProperty
            private CommonFormatsConfig commonFormats = new CommonFormatsConfig();

            /**
             * Configuration for layer preview common formats.
             */
            @Data
            public static class CommonFormatsConfig {
                /**
                 * Flag to enable/disable OpenLayers common format.
                 * (Maps to {@code geoserver.web-ui.demos.layer-preview-page.common-formats.open-layers}).
                 */
                private boolean openLayers = true;

                /**
                 * Flag to enable/disable GML common format.
                 * (Maps to {@code geoserver.web-ui.demos.layer-preview-page.common-formats.gml}).
                 */
                private boolean gml = true;

                /**
                 * Flag to enable/disable KML common format.
                 * (Maps to {@code geoserver.web-ui.demos.layer-preview-page.common-formats.kml}).
                 */
                private boolean kml = true;
            }
        }
    }

    /**
     * Configuration for tools UI components.
     */
    @Data
    public static class Tools {
        /**
         * Flag to enable/disable tools UI.
         * (Maps to {@code geoserver.web-ui.tools.enabled}).
         */
        private boolean enabled = true;

        /**
         * Flag to enable/disable resource browser.
         * (Maps to {@code geoserver.web-ui.tools.resource-browser}).
         */
        private boolean resourceBrowser = true;

        /**
         * Flag to enable/disable catalog bulk load tool.
         * (Maps to {@code geoserver.web-ui.tools.catalog-bulk-load}).
         */
        private boolean catalogBulkLoad = true;

        /**
         * Flag to enable/disable reprojection console.
         * (Maps to {@code geoserver.web-ui.tools.reprojection-console}).
         */
        private boolean reprojectionConsole = true;
    }

    /**
     * Returns whether the class name from a wicket bookmarkable page URL should be enabled or hidden. E.g. for {@code org.geoserver.wms.web.WMSAdminPage}
     * from {@code /web/wicket/bookmarkable/org.geoserver.wms.web.WMSAdminPage?filter=false}, will return {@code false} if {@link #wms} is disabled.
     */
    public boolean enablePageClassUrl(String bookmarkablePageClassName) {
        if (bookmarkablePageClassName == null || bookmarkablePageClassName.isBlank()) {
            return true;
        }

        // Check the page class name to determine which component it belongs to
        if (bookmarkablePageClassName.contains(".wms.web.")) {
            return wms.isEnabled();
        } else if (bookmarkablePageClassName.contains(".wfs.web.")) {
            return wfs.isEnabled();
        } else if (bookmarkablePageClassName.contains(".wcs.web.")) {
            return wcs.isEnabled();
        } else if (bookmarkablePageClassName.contains(".wps.web.")) {
            return wps.isEnabled();
        } else if (bookmarkablePageClassName.contains(".security.web.")) {
            return security.isEnabled();
        } else if (bookmarkablePageClassName.contains(".acl.web.")) {
            return acl.isEnabled();
        } else if (bookmarkablePageClassName.contains(".gwc.web.")) {
            return gwc.isEnabled();
        } else if (bookmarkablePageClassName.contains(".web.demo.")) {
            if (!demos.isEnabled()) {
                return false;
            }

            // Check specific demo pages
            if (bookmarkablePageClassName.contains("LayerPreviewPage")) {
                return demos.getLayerPreviewPage().isEnabled();
            } else if (bookmarkablePageClassName.contains("DemoRequest")) {
                return demos.isDemoRequests();
            } else if (bookmarkablePageClassName.contains("SRSListPage")) {
                return demos.isSrsList();
            } else if (bookmarkablePageClassName.contains("ReprojectPage")) {
                return demos.isReprojectionConsole();
            } else if (bookmarkablePageClassName.contains("WCSRequestBuilder")) {
                return demos.isWcsRequestBuilder();
            } else if (bookmarkablePageClassName.contains("WPSRequestBuilder")) {
                return demos.isWpsRequestBuilder();
            }

            // Default for other demo pages
            return true;
        } else if (bookmarkablePageClassName.contains(".web.resources.")
                || bookmarkablePageClassName.contains(".web.catalogstresstool.")) {

            if (!tools.isEnabled()) {
                return false;
            }

            // Check specific tool pages
            if (bookmarkablePageClassName.contains("PageResourceBrowser")) {
                return tools.isResourceBrowser();
            } else if (bookmarkablePageClassName.contains("CatalogStressTester")) {
                return tools.isCatalogBulkLoad();
            } else if (bookmarkablePageClassName.contains("ReprojectPage")) {
                return tools.isReprojectionConsole();
            }

            // Default for other tool pages
            return true;
        }

        // By default, allow access to other pages
        return true;
    }
}
