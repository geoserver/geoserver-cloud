/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.web.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import java.io.IOException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.geoserver.cloud.geoserver.GeoServerApplication;
import org.geoserver.cloud.test.KeycloakOidcFilter;
import org.geoserver.platform.ModuleStatus;
import org.geoserver.security.GeoServerSecurityFilterChain;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.oauth2.login.GeoServerOAuth2LoginAuthenticationProvider;
import org.geoserver.web.security.oauth2.login.OAuth2LoginAuthProviderPanelInfo;
import org.geoserver.web.security.oauth2.login.OAuth2LoginButtonManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.HtmlUtils;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration test of the OIDC extension in the webui service. It boots the {@link GeoServerApplication} as the webui
 * service with its default settings, and logs in through Keycloak the way a browser does.
 *
 * <p>The OIDC filter is only in the {@code web} chain: its login and callback paths are under {@code /web/}.
 *
 * @since 3.0.3
 */
@SpringBootTest(
        classes = GeoServerApplication.class,
        webEnvironment = WebEnvironment.RANDOM_PORT,
        properties = {
            "geoserver.services=webui",
            "spring.cloud.bus.enabled=false",
            "spring.cloud.config.enabled=false",
            "spring.cloud.config.discovery.enabled=false",
            "spring.cloud.consul.enabled=false",
            "spring.cloud.consul.discovery.enabled=false"
        })
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class OidcWebUIApplicationIT {

    private static final String LOGIN_PATH = "web/oauth2/authorization/openid-connect__oidc";
    private static final Pattern KEYCLOAK_LOGIN_FORM = Pattern.compile("<form[^>]*id=\"kc-form-login\"[^>]*>");
    private static final Pattern FORM_ACTION = Pattern.compile("action=\"([^\"]+)\"");
    private static final Pattern LOGGED_IN_USER = Pattern.compile("class=\"username-text\"[^>]*>([^<]*)<");

    @Container
    static final KeycloakContainer KEYCLOAK =
            new KeycloakContainer().withRealmImportFile("/keycloak/geoserver-realm.json");

    static @TempDir Path tmpdir;
    static Path datadir;

    @DynamicPropertySource
    static void setUpDataDir(DynamicPropertyRegistry registry) throws IOException {
        datadir = tmpdir.resolve("datadir");
        Path gwcdir = datadir.resolve("gwc");
        if (!Files.isDirectory(datadir)) {
            Files.createDirectory(datadir);
        }
        if (!Files.isDirectory(gwcdir)) {
            Files.createDirectory(gwcdir);
        }
        registry.add("geoserver.backend.data-directory.location", datadir::toAbsolutePath);
        registry.add("gwc.cache-directory", gwcdir::toAbsolutePath);
    }

    @LocalServerPort
    private int port;

    @Autowired
    private ApplicationContext appContext;

    @Autowired
    private GeoServerSecurityManager securityManager;

    private RestTestClient browser;

    @BeforeEach
    void setUp() throws Exception {
        // the tests share the application context and the security configuration saved in its data directory
        if (!securityManager.listFilters().contains(KeycloakOidcFilter.NAME)) {
            securityManager.saveFilter(KeycloakOidcFilter.config(KEYCLOAK, port));
            KeycloakOidcFilter.addToChain(securityManager, GeoServerSecurityFilterChain.WEB_CHAIN_NAME);
        }
        browser = newBrowser();
    }

    @Test
    void oidcBeansRegisteredWhenEnabled() {
        assertThat(appContext.getBeansOfType(GeoServerOAuth2LoginAuthenticationProvider.class))
                .as("OIDC authentication provider must be registered when the extension is enabled")
                .hasSize(1);
        assertThat(appContext.getBean("openIdConnectCoreExtension", ModuleStatus.class))
                .hasFieldOrPropertyWithValue("module", "gs-sec-oidc-core");
    }

    @Test
    void oidcWebUIBeansRegisteredInWebUI() {
        assertThat(appContext.getBeansOfType(OAuth2LoginAuthProviderPanelInfo.class))
                .as("OIDC admin-UI panel info must be registered in the webui service")
                .hasSize(1);
        assertThat(appContext.getBeansOfType(OAuth2LoginButtonManager.class))
                .as("OIDC login button manager must be registered in the webui service")
                .hasSize(1);
        assertThat(appContext.getBean("openIdConnectWebExtension", ModuleStatus.class))
                .hasFieldOrPropertyWithValue("module", "gs-sec-oidc-web");
    }

    @Test
    void homePageLinksToProvider() {
        String homePage = getPage("/web/");

        assertThat(homePage).contains(LOGIN_PATH);
        assertThat(loggedInUser(homePage)).isNull();
    }

    @Test
    void loginThroughKeycloak() {
        String keycloakLoginPage = getPage("/" + LOGIN_PATH);

        String landingPage = submitCredentials(keycloakLoginPage, "alice");

        assertThat(loggedInUser(landingPage)).isEqualTo("alice");
        assertThat(loggedInUser(getPage("/web/"))).isEqualTo("alice");
    }

    /** A client that keeps the cookies of GeoServer and Keycloak and follows their redirects, as a browser does. */
    private RestTestClient newBrowser() {
        HttpClient httpClient = HttpClient.newBuilder()
                .cookieHandler(new LocalhostCookieHandler())
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        return RestTestClient.bindToServer(requestFactory)
                .baseUrl("http://localhost:%d".formatted(port))
                .build();
    }

    private String getPage(String path) {
        RestTestClient.ResponseSpec response = browser.get().uri(path).exchange();
        return bodyOf(response);
    }

    /** Fills in the Keycloak login form. The password of a realm user is its user name. */
    private String submitCredentials(String keycloakLoginPage, String username) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("username", username);
        form.add("password", username);
        RestTestClient.ResponseSpec response = browser.post()
                .uri(loginFormAction(keycloakLoginPage))
                .contentType(APPLICATION_FORM_URLENCODED)
                .body(form)
                .exchange();
        return bodyOf(response);
    }

    private static String bodyOf(RestTestClient.ResponseSpec response) {
        response.expectStatus().isOk();
        return response.expectBody(String.class).returnResult().getResponseBody();
    }

    private static URI loginFormAction(String keycloakLoginPage) {
        Matcher form = KEYCLOAK_LOGIN_FORM.matcher(keycloakLoginPage);
        assertThat(form.find())
                .as("Keycloak login form in %s", keycloakLoginPage)
                .isTrue();
        Matcher action = FORM_ACTION.matcher(form.group());
        assertThat(action.find()).as("action of %s", form.group()).isTrue();
        return URI.create(HtmlUtils.htmlUnescape(action.group(1)));
    }

    /** The user name shown by the web UI, or null for an anonymous user. */
    private static String loggedInUser(String page) {
        Matcher matcher = LOGGED_IN_USER.matcher(page);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1).trim();
    }

    /**
     * Keeps the cookies of {@code localhost} without their {@code Secure} attribute. Keycloak marks its login cookies
     * secure even over plain HTTP. Browsers send them back to {@code localhost} anyway, and {@link CookieManager} does
     * not.
     */
    private static class LocalhostCookieHandler extends CookieHandler {

        private static final Pattern SECURE_ATTRIBUTE = Pattern.compile(";\\s*Secure(?=;|$)", Pattern.CASE_INSENSITIVE);

        private final CookieManager cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);

        @Override
        public Map<String, List<String>> get(URI uri, Map<String, List<String>> requestHeaders) throws IOException {
            return cookieManager.get(uri, requestHeaders);
        }

        @Override
        public void put(URI uri, Map<String, List<String>> responseHeaders) throws IOException {
            List<String> cookies = responseHeaders.entrySet().stream()
                    .filter(header -> "Set-Cookie".equalsIgnoreCase(header.getKey()))
                    .flatMap(header -> header.getValue().stream())
                    .map(cookie -> SECURE_ATTRIBUTE.matcher(cookie).replaceAll(""))
                    .toList();
            cookieManager.put(uri, Map.of("Set-Cookie", cookies));
        }
    }
}
