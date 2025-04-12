/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.security.gateway;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayPreAuthenticationConfiguration {

    @Bean
    GatewayPreAuthenticationProvider gatewayPreAuthenticationProvider() {
        return new GatewayPreAuthenticationProvider();
    }
}
