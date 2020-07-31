package org.geoserver.cloud.core;

import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.ProxifyingURLMangler;
import org.geoserver.ows.Request;
import org.geoserver.ows.URLMangler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

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

    private static class CloudProxifyingURLMangler implements URLMangler {

        @Override
        public void mangleURL(
                StringBuilder baseURL, StringBuilder path, Map<String, String> kvp, URLType type) {
            // If the request is not an OWS request, does not proxy the URL
            Request request = Dispatcher.REQUEST.get();
            if (request == null) {
                return;
            }
            HttpServletRequest httpRequest = request.getHttpRequest();
            String fproto = httpRequest.getHeader("X-Forwarded-Proto");
            String fhost = httpRequest.getHeader("X-Forwarded-Host");
            String fpath = httpRequest.getHeader("X-Forwarded-Path");
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
