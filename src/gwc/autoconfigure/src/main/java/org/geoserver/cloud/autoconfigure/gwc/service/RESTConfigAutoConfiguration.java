/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.gwc.service;

import lombok.extern.slf4j.Slf4j;

import org.geoserver.cloud.autoconfigure.gwc.ConditionalOnGeoWebCacheRestConfigEnabled;
import org.geoserver.cloud.autoconfigure.gwc.core.DiskQuotaAutoConfiguration;
import org.geoserver.cloud.gwc.config.core.GeoWebCacheConfigurationProperties;
import org.geoserver.cloud.gwc.config.services.RESTConfigConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Import;

import javax.annotation.PostConstruct;

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
@AutoConfiguration
@ConditionalOnGeoWebCacheRestConfigEnabled
@ConditionalOnClass(RESTConfigConfiguration.class)
@Import(RESTConfigConfiguration.class)
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.gwc.service")
public class RESTConfigAutoConfiguration {

    public @PostConstruct void log() {
        log.info("{} enabled", GeoWebCacheConfigurationProperties.RESTCONFIG_ENABLED);
    }
}
