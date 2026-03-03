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
import org.geoserver.catalog.Catalog;
import org.geoserver.cloud.gwc.config.core.WebMapServiceMinimalConfiguration;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.rest.RestConfiguration;
import org.geoserver.rest.SuffixStripFilter;
import org.geoserver.rest.catalog.AdminRequestCallback;
import org.geoserver.wms.capabilities.LegendSample;
import org.geoserver.wms.capabilities.LegendSampleImpl;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

/**
 * This configuration is intentionally identical to {@code org.geoserver.cloud.restconfig.RestConfigApplicationConfiguration},
 * (except for the extra {@code LegendSample} bean here)
 * for the gwc rest configuration depends on the same infrastructure.
 */
@Configuration
@ComponentScan(
        basePackageClasses = {
            org.geoserver.gwc.dispatch.GeoServerGWCDispatcherController.class,
            org.geoserver.rest.AbstractGeoServerController.class
        },
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SuffixStripFilter.class))
public class GeoWebCacheApplicationConfiguration extends RestConfiguration {

    /**
     * Required by {@link GeoServerTileLayer#getLegendSample}, excluded by {@link
     * WebMapServiceMinimalConfiguration}
     *
     * @param catalog using {@code rawCatalog} instead of {@code catalog}, to avoid the local
     *     workspace and secured catalog decorators
     */
    @Bean
    @ConditionalOnMissingBean
    LegendSample legendSample(@Qualifier("rawCatalog") Catalog catalog, GeoServerResourceLoader loader) {
        return new LegendSampleImpl(catalog, loader);
    }

    @Bean
    @ConditionalOnMissingBean
    AdminRequestCallback adminRequestCallback() {
        return new AdminRequestCallback();
    }

    /**
     * Override of {@link SuffixStripFilter} making sure getPathInfo() does not return null
     * @param appContext
     * @return
     */
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
