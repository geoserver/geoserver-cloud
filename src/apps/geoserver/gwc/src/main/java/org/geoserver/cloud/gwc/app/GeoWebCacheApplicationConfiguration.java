/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.gwc.app;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.geoserver.gwc.dispatch.GeoServerGWCDispatcherController;
import org.geoserver.rest.RestConfiguration;
import org.geoserver.rest.SuffixStripFilter;
import org.geoserver.rest.catalog.AdminRequestCallback;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * This configuration is intentionally identical to
 * {@code org.geoserver.cloud.restconfig.RestConfigApplicationConfiguration}, (except for the extra {@code LegendSample}
 * bean here) for the gwc rest configuration depends on the same infrastructure.
 */
@Configuration
@Import(RestConfiguration.class)
public class GeoWebCacheApplicationConfiguration {

    /**
     * Replaces component scanning on package {@code org.geoserver.gwc.dispatch} from
     * {@literal jar:gs-gwc-<version>.jar!/geowebcache-servlet.xml}
     */
    @Bean
    GeoServerGWCDispatcherController gwcDispatcherController() {
        return new GeoServerGWCDispatcherController();
    }

    @Bean
    @ConditionalOnMissingBean
    AdminRequestCallback adminRequestCallback() {
        return new AdminRequestCallback();
    }

    /** Override of {@link SuffixStripFilter} making sure getPathInfo() does not return null */
    @Bean
    NpeAwareSuffixStripFilter suffixStripFilter(ApplicationContext appContext) {
        return new NpeAwareSuffixStripFilter(appContext);
    }

    static class NpeAwareSuffixStripFilter extends SuffixStripFilter {

        public NpeAwareSuffixStripFilter(ApplicationContext applicationContext) {
            super(applicationContext);
        }

        @Override
        protected void doFilterInternal(
                HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                throws ServletException, IOException {
            request = org.geoserver.cloud.gwc.config.core.GwcRequestPathInfoFilter.adaptRequest(request);
            super.doFilterInternal(request, response, filterChain);
        }
    }
}
