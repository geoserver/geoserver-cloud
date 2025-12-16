/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.configuration.community.security.geonode;

import org.geoserver.spring.config.annotations.TranspileXmlConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Configuration class that defines the web UI components for GeoNode OAuth2 authentication.
 *
 * <p>
 * This class provides the necessary beans for the GeoServer web UI to display and handle
 * GeoNode OAuth2 authentication, including:
 * <ul>
 *   <li>The authentication provider panel info that appears in the security settings</li>
 *   <li>The login button that appears on the login page</li>
 * </ul>
 *
 * <p>
 * These components enable users to configure and use GeoNode OAuth2 authentication
 * through the GeoServer web interface.
 *
 * @since 2.27.0
 */
@Configuration(proxyBeanMethods = false)
@TranspileXmlConfig(
        locations = "jar:gs-sec-oauth2-geonode-.*!/applicationContext.xml",
        includes = GeoNodeOAuth2Configuration.UI_BEANS)
@Import(GeoNodeOAuth2WebUIConfiguration_Generated.class)
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
public class GeoNodeOAuth2WebUIConfiguration {}
