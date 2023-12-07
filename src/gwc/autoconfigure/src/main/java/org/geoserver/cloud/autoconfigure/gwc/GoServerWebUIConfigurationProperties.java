/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.gwc;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the Wicket GeoServer UI
 *
 * <p>Available properties:
 *
 * <pre>{@code
 * geoserver.web-ui.gwc:
 *   enabled: true
 *   capabilities:
 *     tms: true
 *     wmts: true
 *     wmsc: true
 * }</pre>
 *
 * @since 1.0
 */
@ConfigurationProperties(prefix = GoServerWebUIConfigurationProperties.PREFIX)
public @Data class GoServerWebUIConfigurationProperties {

    static final String PREFIX = "geoserver.web-ui.gwc";
    public static final String GWC_WEBUI_ENABLED_PROPERTY = PREFIX + ".enabled";
    public static final String CAPABILITIES_TMS = PREFIX + ".capabilities.tms";
    public static final String CAPABILITIES_WMTS = PREFIX + ".capabilities.wmts";
    public static final String CAPABILITIES_WMSC = PREFIX + ".capabilities.wmsc";

    /**
     * Enables the core GeoWebCache functionality and integration with GeoServer tile layers. All
     * other config properties depend on this one to be enabled.
     */
    private boolean enabled = true;

    private CapabilitiesConfig capabilities = new CapabilitiesConfig();

    public static @Data class CapabilitiesConfig {
        private boolean wmts = true;
        private boolean tms = true;
        private boolean wmsc = true;
    }
}
