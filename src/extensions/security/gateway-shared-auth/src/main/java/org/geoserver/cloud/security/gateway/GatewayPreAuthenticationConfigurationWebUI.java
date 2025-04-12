/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.security.gateway;

import org.geoserver.platform.ExtensionPriority;
import org.geoserver.security.config.RequestHeaderAuthenticationFilterConfig;
import org.geoserver.security.web.auth.HeaderAuthFilterPanel;
import org.geoserver.security.web.auth.HeaderAuthFilterPanelInfo;
import org.geoserver.web.LoginFormInfo;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayPreAuthenticationConfigurationWebUI {

    @Bean
    HeaderAuthFilterPanelInfo gatewayPreAuthPanelInfo() {
        HeaderAuthFilterPanelInfo panelInfo = new HeaderAuthFilterPanelInfo();
        panelInfo.setId("security.gatewayPreAuthFilter");
        panelInfo.setShortTitleKey("GatewayPreAuthFilterPanel.short");
        panelInfo.setTitleKey("GatewayPreAuthFilterPanel.title");
        panelInfo.setDescriptionKey("GatewayPreAuthFilterPanel.description");

        panelInfo.setComponentClass(HeaderAuthFilterPanel.class);
        panelInfo.setServiceClass(GatewayPreAuthenticationFilter.class);
        panelInfo.setServiceConfigClass(RequestHeaderAuthenticationFilterConfig.class);

        return panelInfo;
    }

    @SuppressWarnings("unchecked")
    @Bean
    LoginFormInfo gatewayPreAuthLoginFormInfo() {
        PrioritizableLoginFormInfo lif = new PrioritizableLoginFormInfo();
        lif.setPriority(ExtensionPriority.LOWEST + 1);
        lif.setId("gatewayLoginFormInfo");
        lif.setName("gateway");
        lif.setLoginPath("/login");

        @SuppressWarnings("rawtypes")
        Class componentClass = GatewayPreAuthenticationConfiguration.class;
        lif.setComponentClass(componentClass);
        lif.setIcon("oidc.png");

        lif.setTitleKey("GatewayLoginFormInfo.title");
        lif.setDescriptionKey("GatewayLoginFormInfo.description");
        @SuppressWarnings("rawtypes")
        Class class1 = GatewayPreAuthenticationFilter.class;
        lif.setFilterClass(class1);
        return lif;
    }
}
