/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.configuration.core.web.sec;

import org.geoserver.configuration.core.security.ldap.LDAPSecurityConfiguration;
import org.geoserver.spring.config.annotations.TranspileXmlConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Configuration for the GeoServer LDAP security web UI components.
 *
 * <p>
 * This configuration registers the LDAP security web UI components like panel
 * info classes for configuration through the GeoServer web admin interface.
 *
 * @since 2.27.0.0
 * @see LDAPSecurityConfiguration
 * @see WebSecLdapConfiguration_Generated
 */
@Configuration(proxyBeanMethods = false)
@TranspileXmlConfig(locations = "jar:gs-web-sec-ldap-.*!/applicationContext.xml")
@Import({LDAPSecurityConfiguration.class, WebSecLdapConfiguration_Generated.class})
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
public class WebSecLdapConfiguration {}
