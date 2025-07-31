/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.extensions.security.geonode;

import lombok.extern.slf4j.Slf4j;
import org.geoserver.cloud.autoconfigure.extensions.ConditionalOnGeoServerWebUI;
import org.geoserver.configuration.community.security.geonode.GeoNodeOAuth2WebUIConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * Auto-configuration for GeoNode OAuth2 Web UI components.
 *
 * <p>
 * This configuration is only active when both GeoNode OAuth2 is enabled and the
 * application is the GeoServer WebUI. It configures the necessary UI components
 * for the GeoNode OAuth2 authentication provider.
 * </p>
 *
 * @since 2.27.0
 * @see GeoNodeOAuth2WebUIConfiguration
 */
@AutoConfiguration
@ConditionalOnGeoNodeOAuth2
@ConditionalOnGeoServerWebUI
@Import(GeoNodeOAuth2WebUIConfiguration.class)
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.extensions.security.geonode")
@SuppressWarnings("java:S1118") // public constructor required
public class GeoNodeOAuth2WebUIAutoConfiguration {

    static final String UI_BEANS = "geoNodeOAuth2AuthPanelInfo|geonodeFormLoginButton";
    static final String EXCLUDE_UI_BEANS = "#name=^(?!" + UI_BEANS + ").*$";

    public GeoNodeOAuth2WebUIAutoConfiguration() {
        log.debug("GeoNode OAuth2 Web UI components activated");
    }
}
