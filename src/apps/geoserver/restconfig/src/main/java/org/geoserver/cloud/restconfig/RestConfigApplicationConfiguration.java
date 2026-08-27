/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.restconfig;

import java.io.IOException;
import java.util.List;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import org.geoserver.rest.RequestInfo;
import org.geoserver.rest.RestConfiguration;
import org.geoserver.rest.catalog.AdminRequestCallback;
import org.geoserver.rest.resources.ResourceController;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.http.MediaType;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.accept.ContentNegotiationStrategy;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.servlet.resource.ResourceUrlProvider;

@Configuration
@ComponentScan(basePackageClasses = org.geoserver.rest.AbstractGeoServerController.class)
@SuppressWarnings("deprecation")
public class RestConfigApplicationConfiguration extends RestConfiguration {

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        super.configureContentNegotiation(configurer);
        configurer.favorPathExtension(true);
    }

    @Bean
    @ConditionalOnMissingBean
    AdminRequestCallback adminRequestCallback() {
        return new AdminRequestCallback();
    }

    /**
     * "Deprecate use of path extensions in request mapping and content negotiation" {@code
     * https://github.com/spring-projects/spring-framework/issues/24179}
     */
    @Bean
    @Override
    public RequestMappingHandlerMapping requestMappingHandlerMapping(
            @Qualifier("mvcContentNegotiationManager") ContentNegotiationManager contentNegotiationManager,
            @Qualifier("mvcConversionService") FormattingConversionService conversionService,
            @Qualifier("mvcResourceUrlProvider") ResourceUrlProvider resourceUrlProvider) {

        RequestMappingHandlerMapping handlerMapping =
                super.requestMappingHandlerMapping(contentNegotiationManager, conversionService, resourceUrlProvider);

        handlerMapping.setUseSuffixPatternMatch(true);
        handlerMapping.setUseRegisteredSuffixPatternMatch(true);

        return handlerMapping;
    }

    /**
     * Restores the {@code format} query parameter as the authority over the response representation for
     * {@code /rest/resource/**} requests.
     *
     * <p>Upstream ships this as {@code ResourceController$ResourceControllerConfiguration}, but its patterns
     * ({@code /resource}, {@code /resource/**}) lack the {@code /rest} prefix and never match the request lookup
     * path, leaving format resolution to the path-extension and Accept-header strategies. A metadata request for
     * {@code probe.txt} then negotiates {@code text/plain} from the file extension, and a directory listing follows
     * the Accept header; neither type has a message converter for the REST wrapper and both fail with a 500.
     *
     * <p>{@link RestConfiguration}'s delegating strategy finds this bean through {@code GeoServerExtensions} and
     * consults it before the extension and header strategies. File content downloads are unaffected: their content
     * type is preset on the {@code ResponseEntity} the controller returns, bypassing negotiation.
     */
    @Bean
    ContentNegotiationStrategy resourceApiFormatContentNegotiationStrategy() {
        return new ResourceApiFormatContentNegotiationStrategy();
    }

    static class ResourceApiFormatContentNegotiationStrategy implements ContentNegotiationStrategy {

        @SuppressWarnings("java:S1075") // base path is fixed
        static final String RESOURCE_API_BASE_PATH = "/rest/resource";

        @Override
        public List<MediaType> resolveMediaTypes(NativeWebRequest webRequest) {
            HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
            if (request == null || !isResourceApiRequest(request)) {
                return List.of();
            }
            return List.of(requestedFormat(request));
        }

        private boolean isResourceApiRequest(HttpServletRequest request) {
            String path =
                    request.getRequestURI().substring(request.getContextPath().length());
            if (!path.startsWith(RESOURCE_API_BASE_PATH)) {
                return false;
            }
            int basePathEnd = RESOURCE_API_BASE_PATH.length();
            return path.length() == basePathEnd || path.charAt(basePathEnd) == '/';
        }

        /** Mirrors {@code ResourceController.getFormat}: xml and json are honored, anything else means html */
        private MediaType requestedFormat(HttpServletRequest request) {
            String format = request.getParameter("format");
            if ("xml".equals(format)) {
                return MediaType.APPLICATION_XML;
            }
            if ("json".equals(format)) {
                return MediaType.APPLICATION_JSON;
            }
            return MediaType.TEXT_HTML;
        }
    }

    /**
     * Named {@code restRequestPathInfoFilter} because the GWC starter's {@code GeoWebCacheCoreConfiguration}
     * contributes a {@code setRequestPathInfoFilter} bean; with bean definition overriding enabled, reusing that
     * name would replace this filter and leave REST API requests without servlet path and path info (issue #913).
     */
    @Bean
    SetRequestPathInfoFilter restRequestPathInfoFilter() {
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
}
