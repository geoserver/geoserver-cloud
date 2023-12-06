/*
 * (c) 2021 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.geotools;

import lombok.AccessLevel;
import lombok.Setter;

import org.geotools.http.AbstractHTTPClientFactory;
import org.geotools.http.HTTPBehavior;
import org.geotools.http.HTTPClient;
import org.geotools.http.HTTPConnectionPooling;
import org.geotools.http.LoggingHTTPClient;

import java.util.List;

/** */
public class SpringEnvironmentAwareGeoToolsHttpClientFactory extends AbstractHTTPClientFactory {

    private static @Setter(value = AccessLevel.PACKAGE)
    GeoToolsHttpClientProxyConfigurationProperties proxyConfig =
            new GeoToolsHttpClientProxyConfigurationProperties();

    @Override
    public List<Class<?>> clientClasses() {
        return List.of(SpringEnvironmentAwareGeoToolsHttpClient.class);
    }

    @Override
    public final HTTPClient createClient(List<Class<? extends HTTPBehavior>> behaviors) {
        return new SpringEnvironmentAwareGeoToolsHttpClient(proxyConfig);
    }

    protected @Override HTTPClient createLogging(HTTPClient client) {
        return new LoggingConnectionPoolingHTTPClient(client);
    }

    static class LoggingConnectionPoolingHTTPClient extends LoggingHTTPClient
            implements HTTPConnectionPooling {

        public LoggingConnectionPoolingHTTPClient(HTTPClient delegate) {
            super(delegate);
        }

        public LoggingConnectionPoolingHTTPClient(HTTPClient delegate, String charset) {
            super(delegate, charset);
        }

        @Override
        public int getMaxConnections() {
            return ((HTTPConnectionPooling) delegate).getMaxConnections();
        }

        @Override
        public void setMaxConnections(int maxConnections) {
            ((HTTPConnectionPooling) delegate).setMaxConnections(maxConnections);
        }

        @Override
        public void close() {}
    }
}
