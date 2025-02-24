/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.security.ldap;

import org.geoserver.cloud.autoconfigure.security.ConditionalOnGeoServerSecurityEnabled;
import org.geoserver.cloud.autoconfigure.security.GeoServerSecurityAutoConfiguration;
import org.geoserver.cloud.config.factory.ImportFilteredResource;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/** {@link AutoConfiguration @AutoConfiguration} to enable {@code gs-sec-ldap} */
// run before GeoServerSecurityAutoConfiguration so the provider is available when
// GeoServerSecurityManager calls GeoServerExtensions.extensions(GeoServerSecurityProvider.class)
@AutoConfiguration(before = GeoServerSecurityAutoConfiguration.class)
@EnableConfigurationProperties(LDAPSecurityConfigProperties.class)
@ConditionalOnGeoServerSecurityEnabled
@ConditionalOnProperty(name = "geoserver.security.ldap", havingValue = "true", matchIfMissing = true)
@ImportFilteredResource("jar:gs-sec-ldap-.*!/applicationContext.xml")
public class LDAPSecurityAutoConfiguration {}
