/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.security.gateway.sharedauth;

import static org.geoserver.cloud.security.gateway.sharedauth.GatewaySharedAuthenticationProvider.Mode.SERVER;

import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.wicket.model.IModel;
import org.geoserver.cloud.security.gateway.sharedauth.GatewaySharedAuthenticationFilter;
import org.geoserver.cloud.security.gateway.sharedauth.GatewaySharedAuthenticationProvider;
import org.geoserver.security.web.auth.AuthenticationFilterPanel;
import org.geoserver.security.web.auth.AuthenticationFilterPanelInfo;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for the Gateway Shared Authentication system in server mode.
 *
 * <p>This configuration is automatically activated when running in the GeoServer WebUI application
 * and creates a {@link GatewaySharedAuthenticationProvider} in SERVER mode. This is intended for use
 * in the WebUI service to add authentication headers to responses.</p>
 *
 * <p>Additionally, this configuration contributes Wicket UI panels for the authentication filter
 * in the GeoServer admin interface, allowing administrators to configure the filter through the UI.</p>
 *
 * <p>Note: Previously, this was controlled by the configuration property
 * {@code geoserver.extension.security.gateway-shared-auth.server=true}, but now the mode
 * is automatically determined based on the application type.
 *
 * @see GatewaySharedAuthenticationFilter.ServerFilter
 * @see ClientModeConfiguration
 * @see DisabledModeConfiguration
 * @since 1.9
 */
@Configuration
@ConditionalOnServerMode
@Slf4j
class ServerModeConfiguration {

    @PostConstruct
    void log() {
        log.info("gateway-shared-auth enabled in server mode");
    }

    @Bean
    GatewaySharedAuthenticationProvider gatewaySharedAuthenticationProvider() {
        return new GatewaySharedAuthenticationProvider(SERVER);
    }

    @Bean
    GatewaySharedAuthFilterPanelInfo gatewaySharedAuthPanelInfo() {
        var panelInfo = new GatewaySharedAuthFilterPanelInfo();
        panelInfo.setId("security.gatewaySharedAuthFilter");
        panelInfo.setShortTitleKey("GatewaySharedAuthFilterPanel.short");
        panelInfo.setTitleKey("GatewaySharedAuthFilterPanel.title");
        panelInfo.setDescriptionKey("GatewaySharedAuthFilterPanel.description");

        panelInfo.setComponentClass(GatewaySharedAuthAuthFilterPanel.class);
        panelInfo.setServiceClass(GatewaySharedAuthenticationFilter.class);
        panelInfo.setServiceConfigClass(GatewaySharedAuthenticationFilter.Config.class);

        return panelInfo;
    }

    @SuppressWarnings("serial")
    public static class GatewaySharedAuthFilterPanelInfo
            extends AuthenticationFilterPanelInfo<
                    GatewaySharedAuthenticationFilter.Config, GatewaySharedAuthAuthFilterPanel> {

        public GatewaySharedAuthFilterPanelInfo() {
            setServiceClass(GatewaySharedAuthenticationFilter.class);
            setServiceConfigClass(GatewaySharedAuthenticationFilter.Config.class);
            setComponentClass(GatewaySharedAuthAuthFilterPanel.class);
        }
    }

    @SuppressWarnings({"serial", "java:S110"})
    public static class GatewaySharedAuthAuthFilterPanel
            extends AuthenticationFilterPanel<GatewaySharedAuthenticationFilter.Config> {

        public GatewaySharedAuthAuthFilterPanel(String id, IModel<GatewaySharedAuthenticationFilter.Config> model) {
            super(id, model);
        }
    }
}
