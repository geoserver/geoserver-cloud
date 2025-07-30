/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.configuration.extension.ogcapi.core;

import java.util.Map;
import org.geoserver.catalog.Catalog;
import org.geoserver.cloud.config.factory.ImportFilteredResource;
import org.geoserver.ogcapi.APIDispatcher;
import org.geoserver.ogcapi.CloseableIteratorModule;
import org.geoserver.ows.ClasspathPublisher;
import org.geoserver.ows.OWSHandlerMapping;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;

/**
 * Provides all components of {@code gs-ogcapi-core} jar's
 * {@code applicationContext.xml} but avoiding the too wide component scan on
 * package {@code org.geoserver.ogcapi}.
 * <p>
 * We should make the {@code gs-ogcapi-core} module upstream avoid doing a
 * catch-all component scan, since concrete extensions (e.g. gs-ogcapi-features)
 * will make a component scan on their specific packages (e.g.
 * {@code org.geoserver.ogcapi.v1.features}).
 * <p>
 * This is not an auto-configuration, but meant to be included by concrete APIs
 * auto-configurations, in order to avoid these core OGC API components
 * contributed to unrelated services.
 */
@Configuration
@ImportFilteredResource(
        "jar:gs-ogcapi-core-.*!/applicationContext.xml#name=^(?!apiURLMapping|apiClasspathPublisherMapping).*$")
public class OgcApiCoreConfiguration {

    /**
     * Override the bean definition from {@code applicationContext.xml} to set
     * {@code order} to {@code Ordered.HIGHEST_PRECEDENCE}, the provided value of
     * {@code 0} does not have enough precedence over the default spring boot webmvc
     * {@code HandlerMapping}
     *
     * <pre>
     * {@code
     *  <!-- api http url mapping -->
     *  <bean id="apiURLMapping" class="org.geoserver.ows.OWSHandlerMapping">
     *    <constructor-arg ref="catalog"/>
     *    <property name="alwaysUseFullPath" value="true"/>
     *    <property name="useTrailingSlashMatch" value="true"/>
     *    <property name="order" value="0"/>
     *    <property name="mappings">
     *      <props>
     *        <prop key="/ogc">apiDispatcher</prop>
     *        <prop key="/ogc/**">apiDispatcher</prop>
     *      </props>
     *    </property>
     *  </bean>
     * }
     */
    @Bean
    OWSHandlerMapping apiURLMapping(@Qualifier("catalog") Catalog catalog, APIDispatcher apiDispatcher) {
        OWSHandlerMapping mapping = new OWSHandlerMapping(catalog);
        mapping.setAlwaysUseFullPath(true);
        mapping.setUseTrailingSlashMatch(true);
        mapping.setOrder(Ordered.HIGHEST_PRECEDENCE);
        mapping.setUrlMap(Map.of("/ogc", apiDispatcher, "/ogc/**", apiDispatcher));
        return mapping;
    }
    /**
     * Override the bean definition from {@code applicationContext.xml} to set
     * {@code order} to {@code Ordered.HIGHEST_PRECEDENCE}, the provided value of
     * {@code 0} does not have enough precedence over the default spring boot webmvc
     * {@code HandlerMapping}
     * <p>
     * Also configures the {@code classpathPublisher} to handle  requests to {@code /webresources/ogcapi/**}.
     * This should be centralized in a future iteration, hopefully making the gateway handle static resources directly
     * without proxying to a backend service.
     *
     * <pre>{@code
     * <bean id="apiClasspathPublisherMapping"
     *        class="org.springframework.web.servlet.handler.SimpleUrlHandlerMapping">
     *    <property name="alwaysUseFullPath" value="true"/>
     *    <property name="mappings">
     *      <props>
     *        <prop key="/apicss/**">classpathPublisher</prop>
     *        <prop key="/swagger-ui/**">classpathPublisher</prop>
     *      </props>
     *    </property>
     *  </bean>
     * }</pre>
     */
    @Bean
    SimpleUrlHandlerMapping apiClasspathPublisherMapping(
            @Qualifier("classpathPublisher") ClasspathPublisher classpathPublisher) {
        SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
        mapping.setOrder(Ordered.HIGHEST_PRECEDENCE);
        mapping.setAlwaysUseFullPath(true);
        mapping.setUrlMap(Map.of(
                "/apicss/**",
                classpathPublisher,
                "/swagger-ui/**",
                classpathPublisher,
                "/webresources/ogcapi/**",
                classpathPublisher));
        return mapping;
    }

    /**
     * Registers the {@link CloseableIteratorModule} as a bean.
     *
     * <p>This ensures the module is automatically picked up by Spring Boot's auto-configured {@link
     * com.fasterxml.jackson.databind.ObjectMapper}, which is particularly important in GeoServer Cloud where Spring Boot's
     * {@link org.springframework.http.converter.json.MappingJackson2HttpMessageConverter} is used instead of the custom
     * GeoServer one.
     *
     * <p>The module is also discoverable via Java ServiceLoader (see {@code
     * META-INF/services/com.fasterxml.jackson.databind.Module}) for environments where explicit Spring configuration is not
     * used.
     */
    @Bean
    com.fasterxml.jackson.databind.Module closeableIteratorModule() {
        return new CloseableIteratorModule();
    }
}
