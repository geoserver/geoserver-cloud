/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.gateway;

import java.util.regex.Pattern;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties of the request id, logged by the gateway and the services for each request.
 *
 * @see org.geoserver.cloud.gateway.filter.RequestIdTrustFilter
 */
@ConfigurationProperties(prefix = "geoserver.gateway.request-id")
@Data
public class RequestIdConfigurationProperties {

    /**
     * Regular expression matching the addresses of the proxies allowed to set the request id, with the X-Request-ID,
     * http.request.id, or trace-id request headers. The gateway ignores these headers from other clients, and creates
     * its own id. Unset by default: the gateway trusts no proxy.
     */
    private Pattern trustedProxies;
}
