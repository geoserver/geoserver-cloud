/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.restconfig;


import org.geoserver.rest.CallbackInterceptor;
import org.geoserver.rest.RequestInfo;
import org.geoserver.rest.RestInterceptor;
import org.geoserver.rest.catalog.AdminRequestCallback;
import org.geoserver.rest.resources.ResourceController;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.servlet.resource.ResourceUrlProvider;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

@Configuration
@ComponentScan(
        basePackageClasses = org.geoserver.rest.AbstractGeoServerController.class, //
        /*
         * Exclude AdminRequestCallback from component-scan. For some reason it's not being loaded in
         * vanilla geoserver (from gs-restconfig's applicationContext.xml) and causes a difference in behavior. At some
         * point it'll have to be fixed upstream and re-enabled here.
         */
        excludeFilters =
                @ComponentScan.Filter(
                        type = FilterType.ASSIGNABLE_TYPE,
                        classes = AdminRequestCallback.class))
public class RestConfigApplicationConfiguration extends WebMvcConfigurationSupport {

    @Override
    protected void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RestInterceptor());
        registry.addInterceptor(new CallbackInterceptor());
    }

    /**
     * Override {@link PutIgnoringExtensionContentNegotiationStrategy} defined in {@link
     * StyleController StyleController$StyleControllerConfiguration} as it causes a {@literal
     * java.lang.IllegalArgumentException: Expected lookupPath in request attribute
     * "org.springframework.web.util.UrlPathHelper.PATH".} exception for {@literal /actuator/*}
     * requests.
     *
     * <p>REVISIT: should no longer be needed since we're running the actuator on a separate port to
     * avoid any such conflict with geoserver beans/filters
     */

    /**
     * "Deprecate use of path extensions in request mapping and content negotiation" {@code
     * https://github.com/spring-projects/spring-framework/issues/24179}
     */
    @Bean
    public RequestMappingHandlerMapping requestMappingHandlerMapping(
            @Qualifier("mvcContentNegotiationManager")
                    ContentNegotiationManager contentNegotiationManager,
            @Qualifier("mvcConversionService") FormattingConversionService conversionService,
            @Qualifier("mvcResourceUrlProvider") ResourceUrlProvider resourceUrlProvider) {

        RequestMappingHandlerMapping handlerMapping =
                super.requestMappingHandlerMapping(
                        contentNegotiationManager, conversionService, resourceUrlProvider);

        handlerMapping.setUseSuffixPatternMatch(true);
        // handlerMapping.setUseTrailingSlashMatch(true);
        // handlerMapping.setAlwaysUseFullPath(true);

        return handlerMapping;
    }

    @Bean
    SetRequestPathInfoFilter setRequestPathInfoFilter() {
        return new SetRequestPathInfoFilter();
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

        protected ServletRequest adaptRequest(HttpServletRequest request) {
            final String requestURI = request.getRequestURI();
            final int restIdx = requestURI.indexOf("/rest");
            if (restIdx > -1) {
                final String pathToRest = requestURI.substring(0, restIdx + "/rest".length());
                final String pathInfo = requestURI.substring(pathToRest.length());

                return new HttpServletRequestWrapper(request) {
                    public @Override String getServletPath() {
                        return "/rest";
                    }

                    public @Override String getPathInfo() {
                        return pathInfo;
                    }
                };
            }
            return request;
        }
    }
}
