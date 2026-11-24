/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.web.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.util.tester.WicketTester;
import org.geoserver.cloud.autoconfigure.extensions.test.ConditionalTestAutoConfiguration;
import org.geoserver.cloud.autoconfigure.web.core.WebUIContextInitializer;
import org.geoserver.cloud.geoserver.SpringMvcOwnership;
import org.geoserver.cloud.web.WicketTesters;
import org.geoserver.wcs.web.WCSAdminPage;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.GeoServerHomePage;
import org.geoserver.web.MenuPageInfo;
import org.geoserver.web.ServicesPanel;
import org.geoserver.web.admin.LogPage;
import org.geoserver.web.wicket.WicketHierarchyPrinter;
import org.geoserver.wps.web.WPSAdminPage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(
        classes = org.geoserver.cloud.geoserver.GeoServerApplication.class,
        properties = {"geoserver.services=webui"})
@ActiveProfiles("test") // see bootstrap-test.yml
class WebUIApplicationTest {

    private @Autowired GeoServerApplication app;
    private WicketTester tester;

    static @TempDir Path tmpdir;
    static Path datadir;

    @DynamicPropertySource
    static void setUpDataDir(DynamicPropertyRegistry registry) throws IOException {
        datadir = tmpdir.resolve("datadir");
        Path gwcdir = datadir.resolve("gwc");
        if (!Files.isDirectory(datadir)) {
            datadir = Files.createDirectory(datadir);
        }
        if (!Files.isDirectory(gwcdir)) {
            datadir = Files.createDirectory(gwcdir);
        }
        registry.add("geoserver.backend.data-directory.location", datadir::toAbsolutePath);
        registry.add("gwc.cache-directory", gwcdir::toAbsolutePath);
    }

    static @BeforeAll void beforeAll() {
        System.setProperty("wicket.configuration", "deployment");
        // Disable CSRF protection for tests, since the test framework doesn't set the Referer
        System.setProperty(GeoServerApplication.GEOSERVER_CSRF_DISABLED, "true");
        // make sure that we check the english i18n when needed
        Locale.setDefault(Locale.ENGLISH);
    }

    @BeforeEach
    void setUpWicketTester() throws IOException {
        tester = WicketTesters.create(app, tmpdir);
    }

    @AfterEach
    void tearDownWicketTester() {
        if (null != tester) {
            tester.destroy();
        }
        logout();
    }

    void login() {
        login("admin", "geoserver", "ROLE_ADMINISTRATOR");
    }

    void logout() {
        login("anonymousUser", "", "ROLE_ANONYMOUS");
    }

    protected void login(String username, String password, String... roles) {
        SecurityContextHolder.setContext(new SecurityContextImpl());
        List<GrantedAuthority> l = new ArrayList<>();
        for (String role : roles) {
            l.add(new SimpleGrantedAuthority(role));
        }

        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(username, password, l));
    }

    void print(Component component) {
        boolean dumpClass = true;
        boolean dumpValue = true;
        boolean dumpPath = true;
        WicketHierarchyPrinter.print(component, dumpClass, dumpValue, dumpPath);
    }

    @Test
    void GeoServerHomePage_smoke_test_anonymous() {
        GeoServerHomePage page = tester.startPage(GeoServerHomePage.class);
        assertNotNull(page);
        tester.assertInvisible("administration");
        tester.assertComponent("providedCaps", ListView.class);
    }

    @Test
    void GeoServerHomePage_smoke_test_logged_in() {
        login();
        GeoServerHomePage page = tester.startPage(GeoServerHomePage.class);
        assertNotNull(page);
        tester.assertVisible("administration");
        tester.assertComponent("administration:adminContent", ListView.class);
        tester.assertComponent("providedCaps", ListView.class);
    }

    @Test
    void GeoServerHomePage_smoke_test_service_links() {
        GeoServerHomePage page = tester.startPage(GeoServerHomePage.class);
        assertNotNull(page);
        tester.assertComponent("serviceList", ServicesPanel.class);
        tester.assertVisible("serviceList");
        tester.assertComponent("serviceList:serviceDescriptions:0", ListItem.class);
        tester.assertComponent("serviceList:serviceDescriptions:0:links:0", ListItem.class);

        tester.assertComponent("serviceList:serviceDescriptions:2", ListItem.class);
        tester.assertComponent("serviceList:serviceDescriptions:2:links:0", ListItem.class);

        tester.assertComponent("serviceList:serviceDescriptions:3", ListItem.class);
        tester.assertComponent("serviceList:serviceDescriptions:3:links:0", ListItem.class);

        tester.assertComponent("serviceList:serviceDescriptions:4", ListItem.class);
        tester.assertComponent("serviceList:serviceDescriptions:4:links:0", ListItem.class);
    }

    /** @see WebUIContextInitializer */
    @Test
    void homePageSelectionModeDefaultsToTEXT() {
        assertThat(System.getProperty("GeoServerHomePage.selectionMode")).isEqualTo("TEXT");
    }

    // The LogPage shows the log file, and the services log to the console only
    @Test
    void noLogPageMenuEntry() {
        List<MenuPageInfo> menuPages = app.getBeansOfType(MenuPageInfo.class);

        assertThat(menuPages)
                .extracting(MenuPageInfo::getComponentClass)
                .isNotEmpty()
                .doesNotContain(LogPage.class);
    }

    /**
     * A service serving neither the REST API nor GWC has no reason to pull GeoServer's {@code RestConfiguration} into
     * the context, and keeps Spring Boot's auto-configured Web MVC setup in charge of the handler adapter and mapping.
     * The web UI itself does not own Spring MVC: {@code gs-web-rest}'s {@code WebRestConfiguration} only contributes
     * beans, it is not a {@code WebMvcConfigurationSupport} subclass. The OGC API Features extension does register one
     * ({@code APIConfigurationSupport}, reached through its own web UI integration), but unordered relative to Boot's
     * auto-configuration, so it is parsed after Boot's and contributes only its own {@code @Bean} methods rather than
     * claiming the handler adapter or mapping. See {@link SpringMvcOwnership} for why that still counts as Boot owning
     * Spring MVC.
     */
    @Test
    void springBootOwnsSpringMvc() {
        SpringMvcOwnership.assertSpringBootOwnsSpringMvc(app.getApplicationContext());
    }

    /**
     * Tests the service-specific conditional annotations.
     *
     * <p>Verifies that only the WebUI conditional bean is activated in this service, based on the
     * geoserver.service.webui.enabled=true property set in bootstrap.yml. This test relies on the
     * ConditionalTestAutoConfiguration class from the extensions-core test-jar, which contains beans conditionally
     * activated based on each GeoServer service type.
     */
    @Test
    void testServiceConditionalAnnotations() {
        // Access the application context through the app bean
        ApplicationContext context = app.getApplicationContext();

        // This should exist in WebUI service
        assertThat(context.containsBean("webUiConditionalBean")).isTrue();

        ConditionalTestAutoConfiguration.ConditionalTestBean bean =
                context.getBean("webUiConditionalBean", ConditionalTestAutoConfiguration.ConditionalTestBean.class);
        assertThat(bean.getServiceName()).isEqualTo("WebUI");

        // These should not exist in WebUI service
        assertThat(context.containsBean("wfsConditionalBean")).isFalse();
        assertThat(context.containsBean("wcsConditionalBean")).isFalse();
        assertThat(context.containsBean("wmsConditionalBean")).isFalse();
        assertThat(context.containsBean("wpsConditionalBean")).isFalse();
        assertThat(context.containsBean("restConditionalBean")).isFalse();
    }

    /** A deployment without a wcs service hides the WCS pages and keeps the WPS ones. */
    @Nested
    @TestPropertySource(properties = "geoserver.web-ui.wcs.enabled=false")
    class WcsPagesDisabled {

        @Autowired
        GeoServerApplication wcsPagesDisabledApp;

        @Test
        void noWcsAdminPageMenuEntry() {
            List<MenuPageInfo> menuPages = wcsPagesDisabledApp.getBeansOfType(MenuPageInfo.class);

            assertThat(menuPages)
                    .extracting(MenuPageInfo::getComponentClass)
                    .contains(WPSAdminPage.class)
                    .doesNotContain(WCSAdminPage.class);
        }
    }
}
