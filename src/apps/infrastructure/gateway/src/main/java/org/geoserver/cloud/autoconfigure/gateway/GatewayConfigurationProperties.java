/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.gateway;

import java.util.regex.Pattern;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.lang.Nullable;

/**
 * The {@code geoserver.gateway} configuration properties.
 *
 * @see org.geoserver.cloud.gateway.filter.TrustedProxies
 */
@ConfigurationProperties(prefix = "geoserver.gateway")
@Data
public class GatewayConfigurationProperties {

    /**
     * Regular expression matching the addresses of the reverse proxies in front of the gateway, as seen by the gateway.
     * The gateway reads the client address from the X-Forwarded-For header only when a trusted proxy sent the request.
     * From other clients, the client address is the address of the connection. Unset by default: the gateway trusts
     * no proxy.
     */
    private @Nullable String trustedProxies;

    /** The compiled {@link #getTrustedProxies() trustedProxies}, or null when unset or blank */
    public @Nullable Pattern trustedProxiesPattern() {
        if (trustedProxies == null || trustedProxies.isBlank()) {
            return null;
        }
        return Pattern.compile(trustedProxies);
    }
}
