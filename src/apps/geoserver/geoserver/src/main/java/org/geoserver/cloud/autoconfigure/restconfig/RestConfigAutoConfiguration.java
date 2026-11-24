/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.restconfig;

import java.util.Arrays;
import java.util.List;
import org.geoserver.cloud.autoconfigure.extensions.ConditionalOnGeoServerREST;
import org.geoserver.configuration.core.rest.RestConfigConfiguration;
import org.geoserver.configuration.core.rest.RestConfigConfiguration_Generated;
import org.geoserver.configuration.core.rest.RestRequestPathInfoFilter;
import org.geoserver.configuration.core.rest.RestconfigWcsConfiguration;
import org.geoserver.configuration.core.rest.RestconfigWfsConfiguration;
import org.geoserver.configuration.core.rest.RestconfigWmsConfiguration;
import org.geoserver.configuration.core.rest.RestconfigWmtsConfiguration;
import org.geoserver.rest.PutIgnoringExtensionContentNegotiationStrategy;
import org.geoserver.rest.RestConfiguration;
import org.geoserver.rest.catalog.StyleController;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.http.MediaType;

/**
 * Auto-configuration for the GeoServer REST API in the combined GeoServer application.
 *
 * <p>{@link RestConfigConfiguration} imports GeoServer's {@link RestConfiguration}, which has to reach the context
 * before Spring Boot's Web MVC auto-configuration is evaluated. This class declares no ordering of its own:
 * {@link GeoServerRestFrameworkAutoConfiguration} registers {@link RestConfiguration} early enough, under a condition
 * that matches whenever this one does. Spring keeps the first registration of an imported configuration class and
 * ignores later ones, hence the same import arriving here through {@link RestConfigConfiguration} changes nothing.
 *
 * <p>The {@code suffixStripFilter} bean is deliberately not declared here: both this configuration and the GWC
 * configuration need it, and its unified replacement lives in {@link SuffixStripFilterAutoConfiguration} to avoid a
 * bean name conflict when both services run in the same application context.
 */
@AutoConfiguration
@ConditionalOnGeoServerREST
@Import({
    RestConfigConfiguration.class,
    RestconfigWcsConfiguration.class,
    RestconfigWfsConfiguration.class,
    RestconfigWmsConfiguration.class,
    RestconfigWmtsConfiguration.class
})
public class RestConfigAutoConfiguration {

    /**
     * Places {@link RestRequestPathInfoFilter} ahead of GeoServer's {@code filterChainProxy}, which matches the
     * security chain patterns that protect {@code /rest/**} against the servlet path and path info this filter adapts.
     * Running it afterwards leaves the REST API unauthenticated.
     *
     * <p>Spring Boot registers filter beans that declare no order at {@link Ordered#LOWEST_PRECEDENCE} and breaks ties
     * by bean definition order, which no configuration should have to reason about. One step ahead of that puts this
     * filter before every such filter, {@code filterChainProxy} included, while leaving the explicitly ordered filters
     * of the GeoServer servlet context in front of it.
     */
    private static final int REST_PATH_INFO_FILTER_ORDER = Ordered.LOWEST_PRECEDENCE - 1;

    /**
     * Provides the {@link RestRequestPathInfoFilter} bean, which is responsible for adapting incoming request paths to
     * meet the GeoServer REST API's expectations for servlet path and path info.
     */
    @Bean
    RestRequestPathInfoFilter restRequestPathInfoFilter() {
        return new RestRequestPathInfoFilter();
    }

    /** Registers {@link #restRequestPathInfoFilter()} at {@link #REST_PATH_INFO_FILTER_ORDER}. */
    @Bean
    FilterRegistrationBean<RestRequestPathInfoFilter> restRequestPathInfoFilterRegistration(
            RestRequestPathInfoFilter filter) {
        FilterRegistrationBean<RestRequestPathInfoFilter> registration = new FilterRegistrationBean<>(filter);
        registration.addUrlPatterns("/*");
        registration.setOrder(REST_PATH_INFO_FILTER_ORDER);
        return registration;
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
}
