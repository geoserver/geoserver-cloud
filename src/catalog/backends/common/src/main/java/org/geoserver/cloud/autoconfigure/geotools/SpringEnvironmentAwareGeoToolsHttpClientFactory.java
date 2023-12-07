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

import java.util.List;

/** */
public class SpringEnvironmentAwareGeoToolsHttpClientFactory extends AbstractHTTPClientFactory {

    @Setter(value = AccessLevel.PACKAGE)
    private static GeoToolsHttpClientProxyConfigurationProperties proxyConfig =
            new GeoToolsHttpClientProxyConfigurationProperties();

    @Override
    public List<Class<?>> clientClasses() {
        return List.of(SpringEnvironmentAwareGeoToolsHttpClient.class);
    }

    @Override
    public final HTTPClient createClient(List<Class<? extends HTTPBehavior>> behaviors) {
        return new SpringEnvironmentAwareGeoToolsHttpClient(proxyConfig);
    }
}
