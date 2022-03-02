/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.gwc.core;

import org.geoserver.cloud.autoconfigure.gwc.ConditionalOnGeoWebCacheEnabled;
import org.geoserver.cloud.autoconfigure.gwc.GeoWebCacheConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Replaces upstream's {@literal geowebcache-servlet.xml} which is an aggregate of several xml
 * files.
 * <p>
 * The original {@literal geowebcache-servlet.xml}:
 *
 * <pre>
 * {@code
 *   <import resource="geowebcache-core-context.xml"/>
 *   <import resource="geowebcache-georss-context.xml"/>
 *   <import resource="geowebcache-gmaps-context.xml"/>
 *   <import resource="geowebcache-kmlservice-context.xml"/>
 *   <import resource="geowebcache-rest-context.xml"/>
 *   <import resource="geowebcache-tmsservice-context.xml"/>
 *   <import resource="geowebcache-virtualearth-context.xml"/>
 *   <import resource="geowebcache-wmsservice-context.xml"/>
 *   <import resource="geowebcache-wmtsservice-context.xml"/>
 *   <import resource="geowebcache-diskquota-context.xml"/>
 *   <!--
 *     This mappings are different from the standalone gwc ones in that they prepend the /gwc prefix to the context so it
 *     ends up being, for example, /geoserver/gwc/*
 *   -->
 *   <bean class="org.springframework.beans.factory.config.PropertyPlaceholderConfigurer">
 *     <property name="ignoreUnresolvablePlaceholders" value="true" />
 *     <property name="location">
 *       <value>classpath:application.properties</value>
 *     </property>
 *   </bean>
 *   <context:component-scan base-package="org.geoserver.gwc.dispatch"/>
 * }
 *
 * <p>
 * This auto-configuration only integrates the minimal components to have gwc integrated with
 * GeoServer, while allowing to disable certain components through configuration properties.
 *
 * @since 1.0
 */
@Configuration(proxyBeanMethods = true)
@ConditionalOnGeoWebCacheEnabled
@Import({ //
    GwcCoreAutoConfiguration.class, //
    GeoServerIntegrationAutoConfiguration.class, //
    DiskQuotaAutoConfiguration.class
})
@EnableConfigurationProperties(GeoWebCacheConfigurationProperties.class)
public class GeoWebCacheAutoConfiguration {}
