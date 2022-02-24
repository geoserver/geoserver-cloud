/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.restconfig;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_XML;
import static org.springframework.http.MediaType.TEXT_HTML;

import org.geoserver.rest.CallbackInterceptor;
import org.geoserver.rest.PutIgnoringExtensionContentNegotiationStrategy;
import org.geoserver.rest.RestInterceptor;
import org.geoserver.rest.catalog.StyleController;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.http.MediaType;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.servlet.resource.ResourceUrlProvider;
import org.springframework.web.util.UrlPathHelper;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

@Configuration
@ComponentScan(basePackageClasses = org.geoserver.rest.AbstractGeoServerController.class)
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
     */
    @Bean
    @Primary
    PutIgnoringExtensionContentNegotiationStrategy stylePutContentNegotiationStrategy() {
        return new PutIgnoringExtensionContentNegotiationStrategy(
                new PatternsRequestCondition(
                        "/styles/{styleName}", "/workspaces/{workspaceName}/styles/{styleName}"),
                List.of(APPLICATION_JSON, APPLICATION_XML, TEXT_HTML)) {

            @Override
            public List<MediaType> resolveMediaTypes(NativeWebRequest webRequest)
                    throws HttpMediaTypeNotAcceptableException {
                HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
                String lookupPath = (String) request.getAttribute(UrlPathHelper.PATH_ATTRIBUTE);
                if (null == lookupPath) {
                    return List.of(MediaType.APPLICATION_JSON);
                }
                return super.resolveMediaTypes(webRequest);
            }
        };
    }

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
}
