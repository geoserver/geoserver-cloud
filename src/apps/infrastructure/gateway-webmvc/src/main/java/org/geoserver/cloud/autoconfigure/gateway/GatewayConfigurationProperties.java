/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.gateway;

import java.util.regex.Pattern;
import lombok.Data;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.DeprecatedConfigurationProperty;

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
     * The gateway reads the client address from the X-Forwarded-For header, and the request id of the logs from the
     * X-Request-ID, http.request.id, or trace-id headers, only when a trusted proxy sent the request. From other
     * clients, the client address is the address of the connection, and the gateway creates the request id. Unset by
     * default: the gateway trusts no proxy.
     */
    private @Nullable Pattern trustedProxies;

    private RequestId requestId = new RequestId();

    /**
     * The trusted proxies, from {@code geoserver.gateway.trusted-proxies}, or from the deprecated request-id property
     */
    public @Nullable Pattern resolveTrustedProxies() {
        if (trustedProxies != null) {
            return trustedProxies;
        }
        return requestId.getTrustedProxies();
    }

    /** The deprecated {@code geoserver.gateway.request-id} properties */
    public static class RequestId {

        private @Nullable Pattern trustedProxies;

        /** @deprecated replaced by {@code geoserver.gateway.trusted-proxies} */
        @Deprecated
        @DeprecatedConfigurationProperty(replacement = "geoserver.gateway.trusted-proxies")
        public @Nullable Pattern getTrustedProxies() {
            return trustedProxies;
        }

        public void setTrustedProxies(@Nullable Pattern trustedProxies) {
            this.trustedProxies = trustedProxies;
        }
    }
}
