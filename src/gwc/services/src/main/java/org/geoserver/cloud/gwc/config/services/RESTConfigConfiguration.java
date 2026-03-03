/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.gwc.config.services;

import org.geoserver.catalog.Catalog;
import org.geoserver.cloud.gwc.config.core.CloudGwcUrlHandlerMapping;
import org.geoserver.gwc.controller.GwcUrlHandlerMapping;
import org.geoserver.gwc.layer.GWCGeoServerRESTConfigurationProvider;
import org.geoserver.rest.RestControllerAdvice;
import org.geowebcache.rest.converter.GWCConverter;
import org.geowebcache.util.ApplicationContextProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * The original {@literal gs-gwc-rest.jar!applicationContext.xml}:
 *
 * <pre>{@code
 *   <!-- Used by org.geoserver.rest.RestConfiguration when setting up converters -->
 *   <bean id="gwcConverter" class="org.geowebcache.rest.converter.GWCConverter">
 *     <constructor-arg ref="gwcAppCtx" />
 *   </bean>
 *
 *   <bean id="GWCGeoServerRESTConfigurationProvider" class="org.geoserver.gwc.layer.GWCGeoServerRESTConfigurationProvider">
 *     <description>
 *       XmlConfiguration contributor to set up XStream with GeoServer provided configuration objects for GWC's REST API
 *     </description>
 *     <constructor-arg ref="catalog"/>
 *   </bean>
 *
 *   <!-- Specific URL mapping for GWC WMTS REST API -->
 *   <bean id="gwcWmtsRestUrlHandlerMapping" class="org.geoserver.gwc.controller.GwcUrlHandlerMapping">
 *     <constructor-arg ref="catalog" />
 *     <constructor-arg type="java.lang.String" value="/gwc/rest/wmts" />
 *     <property name="alwaysUseFullPath" value="true" />
 *     <property name="order" value="10" />
 *   </bean>
 *
 *   <context:component-scan base-package="org.geowebcache.rest, org.geowebcache.diskquota.rest.controller" />
 *
 * </beans>
 *
 * }</pre>
 *
 * <p>scans too much. We're only scanning {@literal org.geowebcache.rest}. {@literal
 * org.geowebcache.diskquota.rest.controller} is up to {@link DiskQuotaAutoConfiguration}, and is
 * omitted, I can't find any {@code @Controller} in there, might need to revisit;
 *
 * <p>Conditionals: see {@link ConditionalOnGeoWebCacheRestConfigEnabled}
 *
 * @since 1.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(GWCConverter.class)
@ComponentScan(basePackages = "org.geowebcache.rest")
public class RESTConfigConfiguration {

    /**
     * Since we don't scan the {@literal org.geowebcache.rest}, we need a {@link RestControllerAdvice}
     * explicitly to handle http error code translations.
     * <p>
     * For example, it ensures that {@code org.geoserver.rest.ResourceNotFoundException}
     * is correctly mapped to a 404 response instead of a default 500 error.
     * </p>
     */
    @Bean
    RestControllerAdvice restControllerAdvice() {
        return new RestControllerAdvice();
    }

    /**
     * The original {@literal geowebcache-rest-context.xml}:
     *
     * <pre>{@code
     * <!-- Used by org.geoserver.rest.RestConfiguration when setting up converters -->
     * <bean id="gwcConverter" class="org.geowebcache.rest.converter.GWCConverter">
     *   <constructor-arg ref="gwcAppCtx" />
     * </bean>
     * }</pre>
     *
     * @param appCtx
     */
    @Bean
    @SuppressWarnings("rawtypes")
    GWCConverter gwcConverter(ApplicationContextProvider appCtx) {
        return new GWCConverter(appCtx);
    }

    /**
     * The original {@literal geowebcache-rest-context.xml}:
     *
     * <pre>{@code
     * <bean id="GWCGeoServerRESTConfigurationProvider" class="org.geoserver.gwc.layer.GWCGeoServerRESTConfigurationProvider">
     *   <description>
     *        XmlConfiguration contributor to set up XStream with GeoServer provided configuration objects for GWC's REST API
     *  </description>
     *      <constructor-arg ref="catalog"/>
     * </bean>
     * }</pre>
     *
     * @param catalog
     */
    @Bean(name = "GWCGeoServerRESTConfigurationProvider")
    @SuppressWarnings("java:S6830")
    GWCGeoServerRESTConfigurationProvider gwcGeoServerRESTConfigurationProvider(Catalog catalog) {
        return new GWCGeoServerRESTConfigurationProvider(catalog);
    }

    /**
     * The original {@literal geowebcache-rest-context.xml}:
     *
     * <pre>{@code
     * <!-- Specific URL mapping for GWC WMTS REST API -->
     * <bean id="gwcWmtsRestUrlHandlerMapping" class="org.geoserver.gwc.controller.GwcUrlHandlerMapping">
     *   <constructor-arg ref="catalog" />
     *   <constructor-arg type="java.lang.String" value="/gwc/rest/wmts" />
     *   <property name="alwaysUseFullPath" value="true" />
     *   <property name="order" value="10" />
     * </bean>
     * }</pre>
     *
     * @param catalog
     */
    @Bean
    @SuppressWarnings({"deprecation", "java:S1874"})
    GwcUrlHandlerMapping gwcWmtsRestUrlHandlerMapping(Catalog catalog) {
        GwcUrlHandlerMapping handler = new CloudGwcUrlHandlerMapping(catalog, "/gwc/rest/wmts");
        handler.setAlwaysUseFullPath(true);
        handler.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return handler;
    }
}
