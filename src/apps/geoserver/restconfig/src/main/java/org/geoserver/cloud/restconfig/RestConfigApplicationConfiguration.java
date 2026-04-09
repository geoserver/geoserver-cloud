/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.restconfig;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.geoserver.configuration.core.rest.RestConfigConfiguration;
import org.geoserver.configuration.core.rest.RestConfigConfiguration_Generated;
import org.geoserver.configuration.core.rest.RestRequestPathInfoFilter;
import org.geoserver.configuration.core.rest.RestconfigWcsConfiguration;
import org.geoserver.configuration.core.rest.RestconfigWfsConfiguration;
import org.geoserver.configuration.core.rest.RestconfigWmsConfiguration;
import org.geoserver.configuration.core.rest.RestconfigWmtsConfiguration;
import org.geoserver.rest.PutIgnoringExtensionContentNegotiationStrategy;
import org.geoserver.rest.RestConfiguration;
import org.geoserver.rest.SuffixStripFilter;
import org.geoserver.rest.catalog.StyleController;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.EnableRetry;

@Configuration(proxyBeanMethods = false)
@EnableRetry
@Import({
    RestConfigConfiguration.class,
    RestconfigWcsConfiguration.class,
    RestconfigWfsConfiguration.class,
    RestconfigWmsConfiguration.class,
    RestconfigWmtsConfiguration.class
})
public class RestConfigApplicationConfiguration {

    /**
     * Provides the {@link RestRequestPathInfoFilter} bean, which is responsible for adapting incoming request paths to
     * meet the GeoServer REST API's expectations for servlet path and path info.
     */
    @Bean
    RestRequestPathInfoFilter restRequestPathInfoFilter() {
        return new RestRequestPathInfoFilter();
    }

    /**
     * Workaround to support regular response content type when extension is in path.
     *
     * <p>Overcomes the fact that {@link RestConfigConfiguration_Generated.ComponentScannedBeans#styleController()} bean
     * method won't pick up the inner {@link StyleController.StyleControllerConfiguration} configuration class.
     *
     * <p>Without this, a PUT request to {@code /styles/{styleName}.sld} would be interpreted by Spring's content
     * negotiation as asking for an {@code application/vnd.ogc.sld+xml} response, which the controller does not produce,
     * resulting in a 406 Not Acceptable error.
     *
     * <p>This bean is typically provided by an inner class in {@code StyleController}'s
     * {@code StyleControllerConfiguration} inner configuration class, but since we use a restricted component scan, we
     * must declare it explicitly.
     */
    @Bean
    PutIgnoringExtensionContentNegotiationStrategy stylePutContentNegotiationStrategy() {
        return new PutIgnoringExtensionContentNegotiationStrategy(
                List.of("/rest/styles/{styleName}", "/rest/workspaces/{workspaceName}/styles/{styleName}"),
                Arrays.asList(MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_HTML));
    }

    /**
     * Override of {@link SuffixStripFilter} making sure getPathInfo() does not return null
     *
     * @return SuffixStripFilter as required by {@link RestConfiguration#configureContentNegotiation}'s call to
     *     {@code GeoServerExtensions.bean(SuffixStripFilter.class)}
     */
    @Bean
    SuffixStripFilter suffixStripFilter(ApplicationContext appContext) {
        return new NpeAwareSuffixStripFilter(appContext);
    }

    /**
     * Specialized version of {@link SuffixStripFilter} that ensures the request is properly adapted before processing,
     * preventing potential NullPointerExceptions in the base filter logic when running in the GeoServer Cloud
     * environment.
     */
    static class NpeAwareSuffixStripFilter extends SuffixStripFilter {

        public NpeAwareSuffixStripFilter(ApplicationContext applicationContext) {
            super(applicationContext);
        }

        @Override
        protected void doFilterInternal(
                HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                throws ServletException, IOException {
            request = RestRequestPathInfoFilter.adaptRequest(request);
            super.doFilterInternal(request, response, filterChain);
        }
    }
}
