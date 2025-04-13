/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.extensions.security.gateway.preauth;

import org.geoserver.cloud.autoconfigure.security.ConditionalOnGeoServerSecurityEnabled;
import org.geoserver.cloud.autoconfigure.security.GeoServerSecurityAutoConfiguration;
import org.geoserver.cloud.security.gateway.GatewayPreAuthenticationConfiguration;
import org.geoserver.cloud.security.gateway.GatewayPreAuthenticationConfigurationWebUI;
import org.geoserver.security.web.auth.AuthenticationFilterPanelInfo;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

// run before GeoServerSecurityAutoConfiguration so the provider is available when
// GeoServerSecurityManager calls GeoServerExtensions.extensions(GeoServerSecurityProvider.class)
@AutoConfiguration(before = GeoServerSecurityAutoConfiguration.class)
@Import({GatewayPreAuthenticationConfiguration.class, GatewayPreAuthenticationAutoConfiguration.WebUi.class})
@ConditionalOnGeoServerSecurityEnabled
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
public class GatewayPreAuthenticationAutoConfiguration {

    @Configuration
    @Import(GatewayPreAuthenticationConfigurationWebUI.class)
    @ConditionalOnClass(AuthenticationFilterPanelInfo.class)
    static class WebUi {}
}
