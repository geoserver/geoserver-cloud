/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.core;

import java.util.Map;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.ProxifyingURLMangler;
import org.geoserver.ows.URLMangler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.context.request.NativeWebRequest;

@Configuration
public class UrlProxifyingConfiguration {

    /**
     * {@link URLMangler} to adapt returned URL's to the gateway-service provided {@code
     * X-Forwarded-Proto}, {@code X-Forwarded-Host}, and {@code X-Forwarded-Path} request headers.
     *
     * <p>Unlike the regular {@link ProxifyingURLMangler}, this {@link URLMangler} does not depend
     * on {@link GeoServerInfo#isUseHeadersProxyURL() geoserver's configuration}, but always adapts
     * URL's if the request headers are given. </pre>
     */
    public @Primary @Bean URLMangler cloudProxifyingURLMangler() {
        return new CloudProxifyingURLMangler();
    }

    /**
     * {@link URLMangler} similar to {@link ProxifyingURLMangler} that doesn't depend on {@link
     * GeoServerInfo#isUseHeadersProxyURL()}, since we expect to have an api-gateway service in
     * front providing the required {@code X-Forwarded-*} request headers.
     */
    private static class CloudProxifyingURLMangler implements URLMangler {

        /**
         * Provides access to the current http request so we can get the {@code X-Forwarded-**}
         * request headers sent by the gateway service regardless of {@link Dispatcher#REQUEST}
         * being set or not (i.e. also works for the restconfig api)
         */
        private @Autowired NativeWebRequest nativeRequest;

        @Override
        public void mangleURL(
                StringBuilder baseURL, StringBuilder path, Map<String, String> kvp, URLType type) {

            String fproto = nativeRequest.getHeader("X-Forwarded-Proto");
            String fhost = nativeRequest.getHeader("X-Forwarded-Host");
            String fpath = nativeRequest.getHeader("X-Forwarded-Path");
            if (fproto == null || fhost == null) {
                return;
            }
            if (fpath == null) {
                fpath = "";
            }
            String proxyfiedURL = String.format("%s://%s/%s", fproto, fhost, fpath);
            baseURL.setLength(0);
            baseURL.append(proxyfiedURL);
        }
    }
}
