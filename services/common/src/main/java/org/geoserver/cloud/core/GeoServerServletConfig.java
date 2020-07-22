/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.core;

import javax.servlet.Filter;
import org.geoserver.GeoserverInitStartupListener;
import org.geoserver.filters.FlushSafeFilter;
import org.geoserver.filters.SessionDebugFilter;
import org.geoserver.filters.SpringDelegatingFilter;
import org.geoserver.filters.ThreadLocalsCleanupFilter;
import org.geoserver.platform.AdvancedDispatchFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.web.context.request.RequestContextListener;

@Configuration
@ImportResource({
    "classpath*:/applicationContext.xml",
    "classpath*:/applicationSecurityContext.xml"
})
public class GeoServerServletConfig {

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
    public @Bean FlushSafeFilter flushSafeFilter() {
        return new FlushSafeFilter();
    }

    public @Bean SessionDebugFilter sessionDebugFilter() {
        return new SessionDebugFilter();
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

    /**
     * Allows for filters to be loaded via spring rather than registered here in web.xml. One thing
     * to note is that for such filters init() is not called. INstead any initialization is
     * performed via spring ioc.
     */
    public @Bean SpringDelegatingFilter springDelegatingFilter() {
        return new SpringDelegatingFilter();
    }

    /** Cleans up thread locals Geotools is setting up for concurrency and performance reasons */
    public @Bean ThreadLocalsCleanupFilter threadLocalsCleanupFilter() {
        return new ThreadLocalsCleanupFilter();
    }

    public @Bean FilterRegistrationBean<FlushSafeFilter> flushSafeFilterReg() {
        return newRegistration(flushSafeFilter(), 1);
    }

    public @Bean FilterRegistrationBean<SessionDebugFilter> sessionDebugFilterFilterReg() {
        return newRegistration(sessionDebugFilter(), 2);
    }

    public @Bean FilterRegistrationBean<AdvancedDispatchFilter> advancedDispatchFilterReg() {
        return newRegistration(advancedDispatchFilter(), 4);
    }

    public @Bean FilterRegistrationBean<SpringDelegatingFilter> springDelegatingFilterReg() {
        return newRegistration(springDelegatingFilter(), 3);
    }

    public @Bean FilterRegistrationBean<ThreadLocalsCleanupFilter> threadLocalsCleanupFilterReg() {
        return newRegistration(threadLocalsCleanupFilter(), 5);
    }

    private <T extends Filter> FilterRegistrationBean<T> newRegistration(T filter, int order) {
        FilterRegistrationBean<T> registration = new FilterRegistrationBean<>(filter);
        registration.addUrlPatterns("/*");
        registration.setOrder(order);
        return registration;
    }
}
