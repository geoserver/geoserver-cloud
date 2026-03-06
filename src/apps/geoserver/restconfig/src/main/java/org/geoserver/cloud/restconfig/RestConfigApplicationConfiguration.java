/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.restconfig;

import java.util.Arrays;
import java.util.List;
import org.geoserver.rest.PutIgnoringExtensionContentNegotiationStrategy;
import org.geoserver.rest.RestConfiguration;
import org.geoserver.rest.RestControllerAdvice;
import org.geoserver.rest.SuffixStripFilter;
import org.geoserver.rest.catalog.AdminRequestCallback;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;

/**
 * Core Spring configuration for the GeoServer Cloud REST microservice.
 *
 * <p>This class extends the standard GeoServer {@link RestConfiguration} to adapt the Spring MVC environment for
 * cloud-native microservices. It is responsible for:
 *
 * <ul>
 *   <li>Setting up component scanning for GeoServer REST controllers.
 *   <li>Registering specialized filters like {@link RestRequestPathInfoFilter} and {@link NpeAwareSuffixStripFilter} to
 *       ensure the REST API receives requests in the expected format (servlet path and path info).
 *   <li>Providing {@link RestControllerAdvice} for consistent REST error handling and translation of internal
 *       exceptions to appropriate HTTP status codes.
 *   <li>Configuring content negotiation strategies, specifically addressing cases where URI extensions (e.g.,
 *       {@code .sld}) should not interfere with the desired response format during PUT/POST operations.
 * </ul>
 */
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

    /**
     * Since we use a restricted component scan, we need a {@link RestControllerAdvice} explicitly to handle http error
     * code translations.
     */
    @Bean
    RestControllerAdvice restControllerAdvice() {
        return new RestControllerAdvice();
    }

    /**
     * Provides the {@link RestRequestPathInfoFilter} bean, which is responsible for adapting incoming request paths to
     * meet the GeoServer REST API's expectations for servlet path and path info.
     */
    @Bean
    RestRequestPathInfoFilter restRequestPathInfoFilter() {
        return new RestRequestPathInfoFilter();
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
     * Workaround to support regular response content type when extension is in path.
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
