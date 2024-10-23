/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.authzn;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties to control enablement of the GeoServer Cloud specific "gateway shared
 * authentication" mechanism, by which the authentication in the webui service is conveyed to the
 * other serices using the GeoServer Cloud gateway as intermediary.
 *
 * @since 1.9
 */
@ConfigurationProperties(value = GatewaySharedAuthConfigProperties.PREFIX)
@Data
public class GatewaySharedAuthConfigProperties {

    static final String PREFIX = "geoserver.security.gateway-shared-auth";
    static final String ENABLED_PROP = PREFIX + ".enabled";
    static final String AUTO_PROP = PREFIX + ".auto";
    static final String SERVER_PROP = PREFIX + ".server";

    /**
     * Whether the gateway-shared-auth webui authentication conveyor protocol is enabled. Note the
     * same configuration must be applied to the gateway-service.
     */
    private boolean enabled = true;

    /**
     * Whether to automatically create the gateway-shared-auth authentication filter and append it
     * to the filter chains when enabled
     */
    private boolean auto = true;

    /** true to act as server (i.e. to be set in the webui service) or client (default) */
    private boolean server = false;
}
