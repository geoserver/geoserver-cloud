/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.wms.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import org.geoserver.cloud.geoserver.GeoServerApplication;
import org.geoserver.cloud.test.KeycloakOidcFilter;
import org.geoserver.security.GeoServerSecurityFilterChain;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.impl.ServiceAccessRule;
import org.geoserver.security.impl.ServiceAccessRuleDAO;
import org.geoserver.security.oauth2.config.GeoServerOAuth2LoginFilterConfig;
import org.geoserver.security.oauth2.config.OpenIdRoleSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration test of the Bearer token path of the OIDC extension in the wms service. Keycloak issues the access
 * tokens, and the OIDC filter of the {@code default} chain validates them with the keys published by Keycloak.
 *
 * <p>{@code GetCapabilities} is restricted to the {@code wms-reader} realm role. In the realm, {@code alice} has the
 * role and {@code bob} does not. Their passwords are their user names.
 */
@SpringBootTest(
        classes = GeoServerApplication.class,
        webEnvironment = WebEnvironment.RANDOM_PORT,
        properties = {"geoserver.services=wms"})
@ActiveProfiles("datadir")
@AutoConfigureTestRestTemplate
@Testcontainers(disabledWithoutDocker = true)
class OidcBearerTokenIT {

    private static final String READER_ROLE = "wms-reader";
    private static final String GET_CAPABILITIES = "/wms?service=WMS&version=1.3.0&request=GetCapabilities";

    @Container
    static final KeycloakContainer KEYCLOAK =
            new KeycloakContainer().withRealmImportFile("/keycloak/geoserver-realm.json");

    static @TempDir Path datadir;

    @DynamicPropertySource
    static void setUpDataDir(DynamicPropertyRegistry registry) {
        registry.add("geoserver.backend.data-directory.location", datadir::toAbsolutePath);
        registry.add("gwc.cache-directory", () -> datadir.resolve("gwc").toAbsolutePath());
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private GeoServerSecurityManager securityManager;

    @Autowired
    private ServiceAccessRuleDAO serviceAccessRules;

    // the tests share the application context and the security configuration saved in its data directory
    @BeforeEach
    void setUpSecurityOnce() throws Exception {
        if (securityManager.listFilters().contains(KeycloakOidcFilter.NAME)) {
            return;
        }
        saveOidcFilter();
        KeycloakOidcFilter.addToChain(securityManager, GeoServerSecurityFilterChain.DEFAULT_CHAIN_NAME);
        restrictCapabilitiesToReaders();
    }

    @Test
    void requestWithoutTokenIsChallenged() {
        ResponseEntity<String> response = getCapabilities(new HttpHeaders());

        assertThat(response.getStatusCode()).isEqualTo(UNAUTHORIZED);
    }

    @Test
    void readerTokenGetsCapabilities() {
        ResponseEntity<String> response = getCapabilities(bearer(accessToken("alice")));

        assertThat(response.getStatusCode()).isEqualTo(OK);
        assertThat(response.getBody()).contains("WMS_Capabilities");
    }

    @Test
    void tokenWithoutRoleIsForbidden() {
        ResponseEntity<String> response = getCapabilities(bearer(accessToken("bob")));

        assertThat(response.getStatusCode()).isEqualTo(FORBIDDEN);
    }

    @Test
    void forgedTokenIsRejected() {
        String token = accessToken("alice");
        String forged = token.substring(0, token.lastIndexOf('.') + 1) + "forged-signature";

        ResponseEntity<String> response = getCapabilities(bearer(forged));

        assertThat(response.getStatusCode()).isEqualTo(UNAUTHORIZED);
    }

    private void saveOidcFilter() throws Exception {
        GeoServerOAuth2LoginFilterConfig config = KeycloakOidcFilter.config(KEYCLOAK, port);
        config.setRoleSource(OpenIdRoleSource.AccessToken);
        config.setTokenRolesClaim("realm_access.roles");
        config.setEnableResourceServerMode(true);
        securityManager.saveFilter(config);
    }

    private void restrictCapabilitiesToReaders() throws IOException {
        serviceAccessRules.reload();
        serviceAccessRules.addRule(new ServiceAccessRule("wms", "GetCapabilities", READER_ROLE));
        serviceAccessRules.storeRules();
    }

    private ResponseEntity<String> getCapabilities(HttpHeaders headers) {
        HttpEntity<Void> request = new HttpEntity<>(headers);
        return restTemplate.exchange(GET_CAPABILITIES, HttpMethod.GET, request, String.class);
    }

    private HttpHeaders bearer(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    /** Obtains an access token with the password grant. The passwords of the realm users are their user names. */
    private String accessToken(String username) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", KeycloakOidcFilter.CLIENT_ID);
        form.add("client_secret", KeycloakOidcFilter.CLIENT_SECRET);
        form.add("username", username);
        form.add("password", username);
        form.add("scope", "openid");

        Map<String, Object> tokenResponse = RestClient.create()
                .post()
                .uri(KeycloakOidcFilter.endpoint(KEYCLOAK, "token"))
                .contentType(APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});
        assertThat(tokenResponse).containsKey("access_token");
        return (String) tokenResponse.get("access_token");
    }
}
