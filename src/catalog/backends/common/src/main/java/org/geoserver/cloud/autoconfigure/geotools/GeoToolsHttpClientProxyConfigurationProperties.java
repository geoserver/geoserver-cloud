/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.geotools;

import java.net.http.HttpClient;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import lombok.Data;
import lombok.NonNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * {@link EnableConfigurationProperties @EnableConfigurationProperties} to
 * configure the HTTP proxy for the GeoTools {@link HttpClient} using Spring
 * environment configuration.
 *
 * @see SpringEnvironmentAwareGeoToolsHttpClientFactory
 */
@ConfigurationProperties(prefix = "geotools.httpclient.proxy")
public @Data class GeoToolsHttpClientProxyConfigurationProperties {

    private boolean enabled = true;
    private ProxyHostConfig http = new ProxyHostConfig();
    private ProxyHostConfig https = new ProxyHostConfig();

    public static @Data class ProxyHostConfig {
        private String host;
        private Integer port;
        private String user;
        private String password;
        private List<Pattern> nonProxyHosts;

        public Optional<ProxyHostConfig> forHost(@NonNull String targetHostname) {
            if (host().isEmpty()) {
                return Optional.empty();
            }
            for (Pattern p : nonProxyHosts()) {
                if (p.matcher(targetHostname).matches()) {
                    return Optional.empty();
                }
            }
            return Optional.of(this);
        }

        public List<Pattern> nonProxyHosts() {
            return this.nonProxyHosts == null ? List.of() : this.nonProxyHosts;
        }

        public Optional<String> host() {
            return StringUtils.hasLength(this.host) ? Optional.of(this.host) : Optional.empty();
        }

        public int port() {
            return port == null ? 80 : port.intValue();
        }

        public boolean isSecured() {
            return StringUtils.hasLength(host) && StringUtils.hasLength(user) && StringUtils.hasLength(password);
        }
    }

    public ProxyHostConfig ofProtocol(@NonNull String protocol) {
        if ("http".equals(protocol)) {
            return http == null ? new ProxyHostConfig() : http;
        } else if ("https".equals(protocol)) {
            return https == null ? new ProxyHostConfig() : https;
        }
        throw new IllegalArgumentException("Unknown protocol %s. Expected http(s)".formatted(protocol));
    }
}
