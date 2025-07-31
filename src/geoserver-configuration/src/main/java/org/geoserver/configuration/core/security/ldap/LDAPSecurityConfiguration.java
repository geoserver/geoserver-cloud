/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.configuration.core.security.ldap;

import org.geoserver.cloud.config.factory.ImportFilteredResource;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for the GeoServer LDAP security extension.
 *
 * <p>This configuration enables user authentication, user groups and roles to be stored
 * and managed in an LDAP directory.
 *
 * @since 2.27.0.0
 */
@Configuration
@ImportFilteredResource("jar:gs-sec-ldap-.*!/applicationContext.xml")
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
public class LDAPSecurityConfiguration {}
