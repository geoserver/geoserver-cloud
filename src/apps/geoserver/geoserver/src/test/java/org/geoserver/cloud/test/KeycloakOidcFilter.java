/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.test;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import java.util.ArrayList;
import java.util.List;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.RequestFilterChain;
import org.geoserver.security.config.SecurityManagerConfig;
import org.geoserver.security.oauth2.config.GeoServerOAuth2LoginFilterConfig;
import org.geoserver.security.oauth2.login.GeoServerOAuth2LoginAuthenticationFilter;

/**
 * Configures an OIDC filter for the {@code geoserver} realm of {@code keycloak/geoserver-realm.json}, as an
 * administrator does in the web UI. In the realm, the passwords of the users are their user names.
 */
public final class KeycloakOidcFilter {

    public static final String REALM = "geoserver";
    public static final String CLIENT_ID = "geoserver";
    public static final String CLIENT_SECRET = "geoserver-secret";
    public static final String NAME = "openid-connect";

    private KeycloakOidcFilter() {}

    /** The filter configuration for a GeoServer listening on {@code localhost:port}. */
    public static GeoServerOAuth2LoginFilterConfig config(KeycloakContainer keycloak, int port) {
        GeoServerOAuth2LoginFilterConfig config = new GeoServerOAuth2LoginFilterConfig();
        config.setName(NAME);
        config.setClassName(GeoServerOAuth2LoginAuthenticationFilter.class.getName());
        config.setOidcEnabled(true);
        config.setOidcClientId(CLIENT_ID);
        config.setOidcClientSecret(CLIENT_SECRET);
        config.setOidcScopes("openid profile email");
        config.setOidcUserNameAttribute("preferred_username");
        config.setOidcAuthorizationUri(endpoint(keycloak, "auth"));
        config.setOidcTokenUri(endpoint(keycloak, "token"));
        config.setOidcUserInfoUri(endpoint(keycloak, "userinfo"));
        config.setOidcJwkSetUri(endpoint(keycloak, "certs"));
        // Keycloak runs on plain HTTP in the container
        config.setOidcForceAuthorizationUriHttps(false);
        config.setOidcForceTokenUriHttps(false);
        // the configuration derives these URIs from the current request, and the tests save it outside of one
        config.setBaseRedirectUri("http://localhost:%d/".formatted(port));
        config.calculateRedirectUris();
        config.setPostLogoutRedirectUri("http://localhost:%d/web/".formatted(port));
        return config;
    }

    /** Adds the filter to a filter chain, before the anonymous filter. */
    public static void addToChain(GeoServerSecurityManager securityManager, String chainName) throws Exception {
        SecurityManagerConfig config = securityManager.loadSecurityConfig();
        RequestFilterChain chain = config.getFilterChain().getRequestChainByName(chainName);
        List<String> filterNames = new ArrayList<>(chain.getFilterNames());
        filterNames.add(filterNames.indexOf("anonymous"), NAME);
        chain.setFilterNames(filterNames);
        securityManager.saveSecurityConfig(config);
    }

    /** The URL of an endpoint of the OpenID Connect protocol in the realm, such as {@code token}. */
    public static String endpoint(KeycloakContainer keycloak, String name) {
        return "%s/realms/%s/protocol/openid-connect/%s".formatted(keycloak.getAuthServerUrl(), REALM, name);
    }
}
