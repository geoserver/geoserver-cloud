/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.extensions.security.geonode;

import org.geoserver.security.GeoServerSecurityProvider;
import org.geoserver.web.GeoServerBasePage;
import org.geoserver.web.LoginFormInfo;
import org.geoserver.web.security.oauth2.GeoNodeOAuth2AuthProviderPanelInfo;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
@Configuration
class WebUIComponents {
    /**
     * Creates the GeoNode OAuth2 authentication provider panel info bean.
     *
     * <p>
     * This bean defines the configuration panel that appears in the GeoServer security settings
     * for the GeoNode OAuth2 authentication provider. It sets properties such as the panel's
     * ID and localized text keys for titles and descriptions.
     *
     * @return The configured GeoNodeOAuth2AuthProviderPanelInfo bean
     */
    @Bean
    GeoNodeOAuth2AuthProviderPanelInfo geoNodeOAuth2AuthPanelInfo() {
        GeoNodeOAuth2AuthProviderPanelInfo panelInfo = new GeoNodeOAuth2AuthProviderPanelInfo();
        panelInfo.setId("security.GeoNodeOAuth2AuthProvider");
        panelInfo.setShortTitleKey("GeoNodeOAuth2AuthProviderPanel.short");
        panelInfo.setTitleKey("GeoNodeOAuth2AuthProviderPanel.title");
        panelInfo.setDescriptionKey("GeoNodeOAuth2AuthProviderPanel.description");
        return panelInfo;
    }

    /**
     * Creates the GeoNode login form button bean.
     *
     * <p>
     * This bean defines the GeoNode OAuth2 login button that appears on the GeoServer login page.
     * It configures properties such as:
     * <ul>
     *   <li>Button ID and name</li>
     *   <li>Description and icon</li>
     *   <li>The authentication filter class that handles the OAuth2 authentication process</li>
     *   <li>The login path that initiates the OAuth2 flow</li>
     * </ul>
     *
     * <p>
     * Note that the component and filter classes are loaded dynamically using Class.forName()
     * to avoid direct dependencies on the implementation classes.
     *
     * @return The configured LoginFormInfo bean for GeoNode OAuth2 authentication
     * @throws ClassNotFoundException if the required component or filter classes cannot be found
     */
    @SuppressWarnings("unchecked")
    @Bean
    LoginFormInfo geonodeFormLoginButton() throws ClassNotFoundException {
        LoginFormInfo loginForm = new LoginFormInfo();
        loginForm.setId("geonodeFormLoginButton");
        loginForm.setTitleKey("");
        loginForm.setDescriptionKey("GeoNodeOAuth2AuthProviderPanel.description");
        loginForm.setComponentClass((Class<GeoServerBasePage>)
                Class.forName("org.geoserver.web.security.oauth2.GeoNodeOAuth2AuthProviderPanel"));
        loginForm.setName("geonode");
        loginForm.setIcon("geonode.png");
        loginForm.setFilterClass((Class<GeoServerSecurityProvider>)
                Class.forName("org.geoserver.security.oauth2.GeoNodeOAuthAuthenticationFilter"));
        loginForm.setLoginPath("web/j_spring_oauth2_geonode_login");
        return loginForm;
    }
}
