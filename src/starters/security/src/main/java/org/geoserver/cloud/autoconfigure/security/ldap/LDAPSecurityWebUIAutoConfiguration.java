/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.security.ldap;

import org.geoserver.cloud.autoconfigure.security.ConditionalOnGeoServerSecurityEnabled;
import org.geoserver.cloud.autoconfigure.security.GeoServerSecurityAutoConfiguration;
import org.geoserver.cloud.config.factory.FilteringXmlBeanDefinitionReader;
import org.geoserver.security.web.auth.AuthenticationFilterPanelInfo;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ImportResource;

/**
 * {@link AutoConfiguration @AutoConfiguration} to enable {@code gs-web-sec-ldap} when running with
 * the webui components in the classpath
 */
// run before GeoServerSecurityAutoConfiguration so the provider is available when
// GeoServerSecurityManager calls GeoServerExtensions.extensions(GeoServerSecurityProvider.class)
@AutoConfiguration(before = GeoServerSecurityAutoConfiguration.class)
@EnableConfigurationProperties(LDAPSecurityConfigProperties.class)
@ConditionalOnClass(AuthenticationFilterPanelInfo.class)
@ConditionalOnGeoServerSecurityEnabled
@ConditionalOnProperty(
        name = "geoserver.security.ldap",
        havingValue = "true",
        matchIfMissing = true)
@ImportResource(
        reader = FilteringXmlBeanDefinitionReader.class, //
        locations = "jar:gs-web-sec-ldap-.*!/applicationContext.xml")
public class LDAPSecurityWebUIAutoConfiguration {}
