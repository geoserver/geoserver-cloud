/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.restconfig;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.geoserver.rest.RequestInfo;
import org.geoserver.rest.RestConfiguration;
import org.geoserver.rest.SuffixStripFilter;
import org.geoserver.rest.catalog.AdminRequestCallback;
import org.geoserver.rest.resources.ResourceController;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

@Configuration
@ComponentScan(
        basePackageClasses = org.geoserver.rest.AbstractGeoServerController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SuffixStripFilter.class))
public class RestConfigApplicationConfiguration extends RestConfiguration {

    @Bean
    @ConditionalOnMissingBean
    AdminRequestCallback adminRequestCallback() {
        return new AdminRequestCallback();
    }

    @Bean
    SetRequestPathInfoFilter setRequestPathInfoFilter(ApplicationContext appContext) {
        return new SetRequestPathInfoFilter();
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

    /**
     * GeoSever REST API always expect the {@link HttpServletRequest#getServletPath()} to be
     * {@literal /rest}, and {@link HttpServletRequest#getPathInfo()} whatever comes after in the
     * request URI.
     *
     * <p>for example: {@link RequestInfo} constructor, {@link ResourceController#resource}, etc.
     */
    static class SetRequestPathInfoFilter implements Filter {
        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {

            request = adaptRequest((HttpServletRequest) request);
            chain.doFilter(request, response);
        }
    }

    static class NpeAwareSuffixStripFilter extends SuffixStripFilter {

        public NpeAwareSuffixStripFilter(ApplicationContext applicationContext) {
            super(applicationContext);
        }

        @Override
        protected void doFilterInternal(
                HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                throws ServletException, IOException {
            request = adaptRequest(request);
            super.doFilterInternal(request, response, filterChain);
        }
    }

    static HttpServletRequest adaptRequest(HttpServletRequest request) {
        final String requestURI = request.getRequestURI();
        final String restBasePath = "/rest";
        final int restIdx = requestURI.indexOf(restBasePath);
        if (restIdx > -1) {
            final String pathToRest = requestURI.substring(0, restIdx + restBasePath.length());
            final String pathInfo = requestURI.substring(pathToRest.length());

            return new HttpServletRequestWrapper(request) {
                @Override
                public String getServletPath() {
                    return restBasePath;
                }

                @Override
                public String getPathInfo() {
                    return pathInfo;
                }
            };
        }
        return request;
    }
}
