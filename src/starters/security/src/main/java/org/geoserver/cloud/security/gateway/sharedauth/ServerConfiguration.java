/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.security.gateway.sharedauth;

import static org.geoserver.cloud.security.gateway.sharedauth.GatewaySharedAuthenticationProvider.Mode.SERVER;

import org.apache.wicket.model.IModel;
import org.geoserver.security.web.auth.AuthenticationFilterPanel;
import org.geoserver.security.web.auth.AuthenticationFilterPanelInfo;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Contributes a {@link GatewaySharedAuthenticationProvider} in server mode.
 *
 * @see ClientConfiguration
 * @see DisabledConfiguration
 * @since 1.9
 */
@Configuration
public class ServerConfiguration {

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
