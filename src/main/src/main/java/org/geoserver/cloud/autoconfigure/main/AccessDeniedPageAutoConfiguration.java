/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.main;

import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;

/**
 * Serves the {@code /accessDenied.html} page to which GeoServer's exception translation filters forward a 403.
 *
 * <p>GeoServer maps that path to its {@code filePublisher}, which resolves the page to a real file in the servlet
 * context. That works for an exploded application, which is how the images run, but answers a 500 when the page sits
 * inside a jar, as it does on the test classpath used by failsafe. This mapping takes precedence over upstream's and
 * serves the same page from the classpath, jar or not, leaving the 403 status already set by the filter untouched.
 *
 * <p>The page lives in {@code META-INF/resources} of this module, one copy for every service, where the servlet
 * container also finds it. That is what lets the filters register it as their error page at startup instead of logging
 * that it cannot be found on every filter chain build.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = Type.SERVLET)
@ConditionalOnClass(ResourceHttpRequestHandler.class)
public class AccessDeniedPageAutoConfiguration {

    static final String ACCESS_DENIED_PAGE = "/accessDenied.html";

    /** One step ahead of upstream's {@code filePublisherMapping}, registered at {@code -50}. */
    static final int ORDER = -60;

    @Bean
    SimpleUrlHandlerMapping accessDeniedPageMapping(ResourceHttpRequestHandler accessDeniedPageHandler) {
        return new SimpleUrlHandlerMapping(Map.of(ACCESS_DENIED_PAGE, accessDeniedPageHandler), ORDER);
    }

    @Bean
    ResourceHttpRequestHandler accessDeniedPageHandler() {
        ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();
        handler.setLocationValues(List.of("classpath:/META-INF/resources/"));
        return handler;
    }
}
