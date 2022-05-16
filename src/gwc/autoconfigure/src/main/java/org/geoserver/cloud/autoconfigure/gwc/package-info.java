/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
/**
 * Upstream's {@literal applicatonContext.xml} is the following aggregate:
 *
 * <pre>
 * {@code
 * <beans>
 *   <import resource="geowebcache-servlet.xml" />
 *   <import resource="geowebcache-geoserver-context.xml" />
 *   <import resource="geowebcache-geoserver-wms-integration.xml" />
 *   <import resource="geowebcache-geoserver-wmts-integration.xml" />
 * </beans>
 * }
 * </pre>
 * <p>
 * In turn, {@literal geowebcache-servlet.xml} aggregates the following xml files:
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
 */
package org.geoserver.cloud.autoconfigure.gwc;
