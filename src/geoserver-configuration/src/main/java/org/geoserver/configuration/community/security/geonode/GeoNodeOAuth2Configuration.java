/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.configuration.community.security.geonode;

import org.geoserver.spring.config.annotations.TranspileXmlConfig;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Configuration for GeoNode OAuth2 authentication extension.
 *
 * @since 2.27.0
 */
@Configuration(proxyBeanMethods = false)
// gs-sec-oauth2-core and gs-sec-oauth2-web are transitive but not required for this specific functionality
// This ComponentScan is the only contribution from gs-sec-oauth2-core. Adding it directly instead of transpiling its
// applicationContext.xml
@ComponentScan(basePackages = "org.geoserver.security.oauth2")
@TranspileXmlConfig(
        locations = "jar:gs-sec-oauth2-geonode-.*!/applicationContext.xml",
        excludes = GeoNodeOAuth2Configuration.UI_BEANS)
@Import(GeoNodeOAuth2Configuration_Generated.class)
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
public class GeoNodeOAuth2Configuration {

    static final String UI_BEANS = "geoNodeOAuth2AuthPanelInfo|geonodeFormLoginButton";
}
