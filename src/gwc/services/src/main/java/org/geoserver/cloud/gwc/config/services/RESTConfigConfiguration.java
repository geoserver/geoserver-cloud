/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.gwc.config.services;

import org.geowebcache.rest.converter.GWCConverter;
import org.geowebcache.util.ApplicationContextProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * The original {@literal geowebcache-rest-context.xml}:
 *
 * <pre>{@code
 *  <!-- Used by org.geoserver.rest.RestConfiguration when setting up converters -->
 *  <bean id="gwcConverter" class="org.geowebcache.rest.converter.GWCConverter">
 *    <constructor-arg ref="gwcAppCtx" />
 *  </bean>
 *
 *  <context:component-scan base-package=
 * "org.geowebcache.rest, org.geowebcache.diskquota.rest.controller, org.geowebcache.service.wmts" />
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
    @SuppressWarnings("rawtypes")
    public @Bean GWCConverter<?> gwcConverter(ApplicationContextProvider appCtx) {
        return new GWCConverter(appCtx);
    }
}
