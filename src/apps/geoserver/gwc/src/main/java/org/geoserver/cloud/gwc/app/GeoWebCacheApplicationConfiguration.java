/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.gwc.app;

import org.geoserver.catalog.Catalog;
import org.geoserver.cloud.gwc.config.core.WebMapServiceMinimalConfiguration;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.rest.RequestInfo;
import org.geoserver.rest.RestConfiguration;
import org.geoserver.wms.capabilities.LegendSample;
import org.geoserver.wms.capabilities.LegendSampleImpl;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
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
    public LegendSample legendSample(
            @Qualifier("rawCatalog") Catalog catalog, GeoServerResourceLoader loader) {
        return new LegendSampleImpl(catalog, loader);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        super.configureContentNegotiation(configurer);
        configurer.favorPathExtension(true);
    }

    /**
     * "Deprecate use of path extensions in request mapping and content negotiation" {@code
     * https://github.com/spring-projects/spring-framework/issues/24179}
     */
    @SuppressWarnings("deprecation")
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
        handlerMapping.setUseRegisteredSuffixPatternMatch(true);

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
