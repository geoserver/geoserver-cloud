/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.security.gateway;

import org.geoserver.security.config.RequestHeaderAuthenticationFilterConfig;

@SuppressWarnings("serial")
public class GatewayPreAuthenticationFilterConfig extends RequestHeaderAuthenticationFilterConfig {

    public GatewayPreAuthenticationFilterConfig() {
        setPrincipalHeaderAttribute("sec-username");
    }
}
