/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.configuration.community.security.geonode;

import org.geoserver.cloud.config.factory.ImportFilteredResource;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for GeoNode OAuth2 authentication extension.
 *
 * @since 2.27.0
 */
@Configuration
@ComponentScan(basePackages = "org.geoserver.security.oauth2")
@ImportFilteredResource(
        // gs-sec-oauth2-core and gs-sec-oauth2-web are transitive but not required for
        // this specific functionality
        "jar:gs-sec-oauth2-geonode-.*!/applicationContext.xml" + GeoNodeOAuth2Configuration.EXCLUDE_UI_BEANS)
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
public class GeoNodeOAuth2Configuration {

    static final String UI_BEANS = "geoNodeOAuth2AuthPanelInfo|geonodeFormLoginButton";
    static final String EXCLUDE_UI_BEANS = "#name=^(?!" + UI_BEANS + ").*$";
}
