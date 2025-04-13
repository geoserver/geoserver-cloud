/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.geotools;

import java.util.List;
import lombok.AccessLevel;
import lombok.Setter;
import org.geotools.http.AbstractHTTPClientFactory;
import org.geotools.http.HTTPBehavior;
import org.geotools.http.HTTPClient;

/**
 * A GeoTools HTTP client factory that creates clients aware of Spring Environment configurations.
 *
 * <p>
 * This factory creates {@link SpringEnvironmentAwareGeoToolsHttpClient} instances that use
 * proxy configuration from Spring Boot's externalized configuration system. This allows
 * GeoTools HTTP clients to use proxy settings defined in application properties or YAML
 * files, environment variables, or other Spring configuration sources.
 * </p>
 *
 * <p>
 * The factory uses {@link GeoToolsHttpClientProxyConfigurationProperties} to access the
 * proxy configurations. It is registered with GeoTools through the standard Java
 * Service Provider Interface (SPI) mechanism.
 * </p>
 *
 * <p>
 * Example usage:
 * </p>
 * <pre>
 * // GeoTools will discover and use this factory automatically
 * HTTPClient client = HTTPClientFinder.createClient();
 * </pre>
 *
 * @see SpringEnvironmentAwareGeoToolsHttpClient
 * @see GeoToolsHttpClientProxyConfigurationProperties
 * @see AbstractHTTPClientFactory
 */
public class SpringEnvironmentAwareGeoToolsHttpClientFactory extends AbstractHTTPClientFactory {

    /**
     * Proxy configuration properties used by the HTTP clients created by this factory.
     *
     * <p>
     * This static field is initialized with default settings and can be updated
     * by Spring or other configuration mechanisms through the setter method.
     * </p>
     */
    @Setter(value = AccessLevel.PACKAGE)
    private static GeoToolsHttpClientProxyConfigurationProperties proxyConfig =
            new GeoToolsHttpClientProxyConfigurationProperties();

    /**
     * Returns the list of client classes this factory can create.
     *
     * <p>
     * This implementation returns a list containing only
     * {@link SpringEnvironmentAwareGeoToolsHttpClient}.
     * </p>
     *
     * @return a list of supported HTTP client classes
     */
    @Override
    public List<Class<?>> clientClasses() {
        return List.of(SpringEnvironmentAwareGeoToolsHttpClient.class);
    }

    /**
     * Creates a new HTTP client instance with the current proxy configuration.
     *
     * <p>
     * This method creates a new {@link SpringEnvironmentAwareGeoToolsHttpClient}
     * using the current proxy configuration. The behaviors parameter is not used in
     * this implementation.
     * </p>
     *
     * @param behaviors a list of HTTP behaviors (ignored in this implementation)
     * @return a new HTTP client instance
     */
    @Override
    public final HTTPClient createClient(List<Class<? extends HTTPBehavior>> behaviors) {
        return new SpringEnvironmentAwareGeoToolsHttpClient(proxyConfig);
    }
}
