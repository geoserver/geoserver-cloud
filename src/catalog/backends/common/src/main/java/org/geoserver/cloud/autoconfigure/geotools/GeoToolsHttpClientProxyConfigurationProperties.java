/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.geotools;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import lombok.Data;
import lombok.NonNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * Configuration properties for HTTP proxy settings used by GeoTools HTTP clients.
 *
 * <p>
 * This class provides a mechanism to configure HTTP/HTTPS proxy settings for GeoTools
 * HTTP clients through Spring Boot's externalized configuration system. It allows
 * configuring different proxy settings for HTTP and HTTPS protocols, including
 * host, port, authentication credentials, and non-proxy hosts patterns.
 * </p>
 *
 * <p>
 * Example application.yml configuration:
 * </p>
 *
 * <pre>@{code
 * geotools:
 *   httpclient:
 *     proxy:
 *       enabled: true
 *       http:
 *         host: proxy.example.com
 *         port: 8080
 *         user: proxyuser
 *         password: proxypass
 *         non-proxy-hosts:
 *           - localhost
 *           - 127\.0\.0\.1
 *           - .*\.internal\.org
 *       https:
 *         host: secure-proxy.example.com
 *         port: 8443
 *         user: proxyuser
 *         password: proxypass
 *         non-proxy-hosts:
 *           - localhost
 *           - 127\.0\.0\.1
 * }</pre>
 *
 * <p>
 * This class is used by {@link SpringEnvironmentAwareGeoToolsHttpClientFactory} to create
 * HTTP clients with the configured proxy settings. It's automatically configured when using
 * the Spring Boot autoconfiguration for GeoTools.
 * </p>
 *
 * @see SpringEnvironmentAwareGeoToolsHttpClientFactory
 * @see SpringEnvironmentAwareGeoToolsHttpClient
 * @see EnableConfigurationProperties
 * @see ConfigurationProperties
 */
@ConfigurationProperties(prefix = "geotools.httpclient.proxy")
public @Data class GeoToolsHttpClientProxyConfigurationProperties {

    /**
     * Whether proxy configuration is enabled.
     * When set to false, no proxy will be applied to HTTP connections
     * regardless of other configuration.
     * Defaults to true.
     */
    private boolean enabled = true;

    /**
     * Proxy configuration for HTTP protocol connections.
     * This configuration is used for HTTP URLs.
     */
    private ProxyHostConfig http = new ProxyHostConfig();

    /**
     * Proxy configuration for HTTPS protocol connections.
     * This configuration is used for HTTPS URLs.
     */
    private ProxyHostConfig https = new ProxyHostConfig();

    /**
     * Configuration for a specific proxy host with associated settings.
     *
     * <p>
     * This class represents a single proxy configuration including host, port,
     * authentication credentials, and exceptions (non-proxy hosts).
     * </p>
     */
    public static @Data class ProxyHostConfig {
        /**
         * The hostname or IP address of the proxy server.
         * This is required for the proxy configuration to be applied.
         */
        private String host;

        /**
         * The port number of the proxy server.
         * If not specified, defaults to 80.
         */
        private Integer port;

        /**
         * The username for proxy authentication.
         * Required only if the proxy server requires authentication.
         */
        private String user;

        /**
         * The password for proxy authentication.
         * Required only if the proxy server requires authentication.
         */
        private String password;

        /**
         * List of host patterns that should bypass the proxy.
         * Each entry is a regular expression pattern that will be matched against
         * the target hostname. If a match is found, the connection will be made directly
         * without using the proxy.
         */
        private List<String> nonProxyHosts;

        /**
         * Cached compiled patterns for nonProxyHosts.
         * This field is transient and not part of the configuration properties.
         */
        @SuppressWarnings("java:S2065") // transient to not be considered part of the config props
        private transient List<Pattern> compiledPatterns;

        /**
         * Determines if this proxy configuration should be used for the given host.
         *
         * <p>
         * This method checks if the proxy should be applied for a specific target hostname
         * by checking against the nonProxyHosts patterns. If the target hostname matches
         * any of the nonProxyHosts patterns, the proxy should not be used.
         * </p>
         *
         * @param targetHostname the hostname to check
         * @return an Optional containing this proxy configuration if it should be used,
         *         or an empty Optional if the proxy should be bypassed for this host
         * @throws NullPointerException if targetHostname is null
         */
        public Optional<ProxyHostConfig> forHost(@NonNull String targetHostname) {
            if (host().isEmpty()) {
                return Optional.empty();
            }

            for (Pattern p : getCompiledPatterns()) {
                if (p.matcher(targetHostname).matches()) {
                    return Optional.empty();
                }
            }
            return Optional.of(this);
        }

        /**
         * Gets the list of host patterns that should bypass the proxy.
         *
         * @return a list of non-proxy host patterns, never null
         */
        public List<String> nonProxyHosts() {
            return this.nonProxyHosts == null ? List.of() : this.nonProxyHosts;
        }

        /**
         * Gets the compiled regular expression patterns for nonProxyHosts.
         *
         * <p>
         * Compiles the nonProxyHosts strings into Pattern objects if not already cached.
         * This is done lazily to avoid unnecessarily compiling patterns that may not be used.
         * </p>
         *
         * @return a list of compiled Pattern objects
         */
        public List<Pattern> getCompiledPatterns() {
            if (compiledPatterns == null) {
                compiledPatterns =
                        nonProxyHosts().stream().map(Pattern::compile).toList();
            }
            return compiledPatterns;
        }

        /**
         * Gets the proxy host as an Optional.
         *
         * @return an Optional containing the host if it's not empty, or an empty Optional
         */
        public Optional<String> host() {
            return StringUtils.hasLength(this.host) ? Optional.of(this.host) : Optional.empty();
        }

        /**
         * Gets the proxy port number.
         *
         * @return the configured port number, or 80 if not set
         */
        public int port() {
            return port == null ? 80 : port.intValue();
        }

        /**
         * Determines if this proxy configuration requires authentication.
         *
         * <p>
         * A proxy is considered to require authentication if host, user, and password
         * are all non-empty.
         * </p>
         *
         * @return true if this proxy configuration requires authentication
         */
        public boolean isSecured() {
            return StringUtils.hasLength(host) && StringUtils.hasLength(user) && StringUtils.hasLength(password);
        }
    }

    /**
     * Gets the proxy configuration for a specific protocol.
     *
     * <p>
     * This method returns the appropriate proxy configuration based on the
     * specified protocol (either "http" or "https").
     * </p>
     *
     * @param protocol the protocol to get proxy configuration for, must be "http" or "https"
     * @return the proxy configuration for the specified protocol
     * @throws NullPointerException if protocol is null
     * @throws IllegalArgumentException if protocol is neither "http" nor "https"
     */
    public ProxyHostConfig ofProtocol(@NonNull String protocol) {
        if ("http".equals(protocol)) {
            return http == null ? new ProxyHostConfig() : http;
        } else if ("https".equals(protocol)) {
            return https == null ? new ProxyHostConfig() : https;
        }
        throw new IllegalArgumentException("Unknown protocol %s. Expected http(s)".formatted(protocol));
    }
}
