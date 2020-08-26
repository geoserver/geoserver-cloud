/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.servlet;

import javax.servlet.Filter;
import org.geoserver.GeoserverInitStartupListener;
import org.geoserver.cloud.config.main.GeoServerMainConfiguration;
import org.geoserver.filters.FlushSafeFilter;
import org.geoserver.filters.SessionDebugFilter;
import org.geoserver.filters.SpringDelegatingFilter;
import org.geoserver.filters.ThreadLocalsCleanupFilter;
import org.geoserver.platform.AdvancedDispatchFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.context.request.RequestContextListener;

@Configuration
@Import(GeoServerMainConfiguration.class)
public class GeoServerServletContextConfiguration {

    private static final int FLUSH_SAFE_FILTER_ORDER = 1;
    private static final int SESSION_DEBUG_FILTER_ORDER = 2;
    private static final int SPRING_DELEGATING_FILTER_ORDER = 3;
    private static final int ADVANCED_DISPATCH_FILTER_ORDER = 4;
    private static final int THREAD_LOCALS_CLEANUP_FILTER_ORDER = 5;

    // Listeners
    public @Bean GeoserverInitStartupListener initStartupListener() {
        return new GeoserverInitStartupListener();
    }

    public @Bean GeoServerServletInitializer contextLoaderListener() {
        return new GeoServerServletInitializer();
    }

    @ConditionalOnMissingBean(RequestContextListener.class)
    public @Bean RequestContextListener requestContextListener() {
        return new RequestContextListener();
    }
    // Filters

    /**
     * A servlet filter making sure we cannot end up calling flush() on the response output stream
     * after close() has been called (https://osgeo-org.atlassian.net/browse/GEOS-5985)
     */
    @ConditionalOnProperty(
        prefix = "geoserver.servlet.filter.flush-safe",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
    )
    public @Bean FlushSafeFilter flushSafeFilter() {
        return new FlushSafeFilter();
    }

    @ConditionalOnProperty(
        prefix = "geoserver.servlet.filter.flush-safe",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
    )
    public @Bean FilterRegistrationBean<FlushSafeFilter> flushSafeFilterReg() {
        return newRegistration(flushSafeFilter(), FLUSH_SAFE_FILTER_ORDER);
    }

    @ConditionalOnProperty(
        prefix = "geoserver.servlet.filter.session-debug",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
    )
    public @Bean SessionDebugFilter sessionDebugFilter() {
        return new SessionDebugFilter();
    }

    @ConditionalOnProperty(
        prefix = "geoserver.servlet.filter.session-debug",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
    )
    public @Bean FilterRegistrationBean<SessionDebugFilter> sessionDebugFilterFilterReg() {
        return newRegistration(sessionDebugFilter(), SESSION_DEBUG_FILTER_ORDER);
    }

    /**
     * Allows for a single mapping ({@code /*}) for all requests to the spring dispatcher. It
     * creates a wrapper around the servlet request object that "fakes" the serveltPath property to
     * make it look like the mapping was created in web.xml when in actuality it was created in
     * spring.
     *
     * <p>This is useful, for instance, for the rest api's {@code RequestInfo} object to build URLs
     * properly, instead of getting {@code null} from {@code
     * HttpServletRequest.request.getPathInfo()}
     */
    public @Bean AdvancedDispatchFilter advancedDispatchFilter() {
        return new AdvancedDispatchFilter();
    }

    public @Bean FilterRegistrationBean<AdvancedDispatchFilter> advancedDispatchFilterReg() {
        return newRegistration(advancedDispatchFilter(), ADVANCED_DISPATCH_FILTER_ORDER);
    }

    /**
     * Allows for filters to be loaded via spring rather than registered here in web.xml. One thing
     * to note is that for such filters init() is not called. INstead any initialization is
     * performed via spring ioc.
     */
    public @Bean SpringDelegatingFilter springDelegatingFilter() {
        return new SpringDelegatingFilter();
    }

    public @Bean FilterRegistrationBean<SpringDelegatingFilter> springDelegatingFilterReg() {
        return newRegistration(springDelegatingFilter(), SPRING_DELEGATING_FILTER_ORDER);
    }

    /** Cleans up thread locals GeoTools is setting up for concurrency and performance reasons */
    public @Bean ThreadLocalsCleanupFilter threadLocalsCleanupFilter() {
        return new ThreadLocalsCleanupFilter();
    }

    public @Bean FilterRegistrationBean<ThreadLocalsCleanupFilter> threadLocalsCleanupFilterReg() {
        return newRegistration(threadLocalsCleanupFilter(), THREAD_LOCALS_CLEANUP_FILTER_ORDER);
    }

    private <T extends Filter> FilterRegistrationBean<T> newRegistration(T filter, int order) {
        FilterRegistrationBean<T> registration = new FilterRegistrationBean<>(filter);
        registration.addUrlPatterns("/*");
        registration.setOrder(order);
        return registration;
    }
}
