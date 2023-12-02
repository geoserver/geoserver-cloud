/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.web.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTester;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.GeoServerHomePage;
import org.geoserver.web.GeoServerLoginPage;
import org.geoserver.web.ServicesPanel;
import org.geoserver.web.admin.GlobalSettingsPage;
import org.geoserver.web.wicket.WicketHierarchyPrinter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@SpringBootTest(
        properties = {
            "spring.cloud.bus.enabled: false",
            "spring.cloud.config.enabled: false",
            "spring.cloud.config.discovery.enabled: false",
            "eureka.client.enabled: false"
        })
@ActiveProfiles("test") // see bootstrap-test.yml
public class WebUIApplicationTest {

    private @Autowired GeoServerApplication app;
    private WicketTester tester;

    static @BeforeAll void beforeAll() {
        System.setProperty("wicket.configuration", "deployment");
        // Disable CSRF protection for tests, since the test framework doesn't set the Referer
        System.setProperty(GeoServerApplication.GEOSERVER_CSRF_DISABLED, "true");
        // make sure that we check the english i18n when needed
        Locale.setDefault(Locale.ENGLISH);
    }

    @BeforeEach
    void setUpWicketTester() {
        boolean init = true;
        tester = new WicketTester(app, init);
    }

    @AfterEach
    void tearDownWicketTester() {
        if (null != tester) tester.destroy();
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

    @SuppressWarnings("unused")
    private void print(Component component) {
        boolean dumpClass = true;
        boolean dumpValue = false;
        boolean dumpPath = true;
        WicketHierarchyPrinter.print(component, dumpClass, dumpValue, dumpPath);
    }

    @Test
    void GeoServerHomePage_smoke_test_anonymous() {
        GeoServerHomePage page = tester.startPage(GeoServerHomePage.class);
        assertNotNull(page);
        // print(page);
        tester.assertInvisible("catalogLinks");
        tester.assertComponent("providedCaps", ListView.class);
    }

    @Test
    void GeoServerHomePage_smoke_test_logged_in() {
        login();
        GeoServerHomePage page = tester.startPage(GeoServerHomePage.class);
        assertNotNull(page);
        // print(page);
        tester.assertComponent("catalogLinks:layersLink", BookmarkablePageLink.class);
        tester.assertComponent("catalogLinks:storesLink", BookmarkablePageLink.class);
        tester.assertComponent("catalogLinks:workspacesLink", BookmarkablePageLink.class);
        tester.assertComponent("providedCaps", ListView.class);
    }

    @Test
    void GlobalSettingsPage_smoke_test_loggedout() {
        logout();
        tester.startPage(GlobalSettingsPage.class);
        tester.assertRenderedPage(GeoServerLoginPage.class);
    }

    @Test
    void GlobalSettingsPage_smoke_test() {
        login();
        tester.startPage(GlobalSettingsPage.class);
        tester.assertRenderedPage(GlobalSettingsPage.class);
        GlobalSettingsPage page = (GlobalSettingsPage) tester.getLastRenderedPage();
        assertNotNull(page);
        // print(page);
        assertHidden("proxyBaseUrlContainer");
        assertHidden("useHeadersProxyURL");
        assertHidden("loggingSettingsContainer");
        assertHidden("lockProviderContainer");
        assertHidden("webUISettingsContainer");
    }

    @Test
    void GeoServerHomePage_smoke_test_service_links() {
        GeoServerHomePage page = tester.startPage(GeoServerHomePage.class);
        assertNotNull(page);
        // print(page);
        tester.assertComponent("serviceList", ServicesPanel.class);
        tester.assertVisible("serviceList");
        tester.assertComponent("serviceList:serviceDescriptions:0", ListItem.class);
        tester.assertComponent("serviceList:serviceDescriptions:0:links:0", ListItem.class);

        tester.assertComponent("serviceList:serviceDescriptions:1", ListItem.class);
        tester.assertComponent("serviceList:serviceDescriptions:1:links:0", ListItem.class);

        tester.assertComponent("serviceList:serviceDescriptions:2", ListItem.class);
        tester.assertComponent("serviceList:serviceDescriptions:2:links:0", ListItem.class);

        tester.assertComponent("serviceList:serviceDescriptions:3", ListItem.class);
        tester.assertComponent("serviceList:serviceDescriptions:3:links:0", ListItem.class);

        tester.assertComponent("serviceList:serviceDescriptions:4", ListItem.class);
        tester.assertComponent("serviceList:serviceDescriptions:4:links:0", ListItem.class);
    }

    protected void assertHidden(String id) {
        TagTester tag = tester.getTagById(id);
        String msg =
                "expected custom 'unused' css class to hide the "
                        + id
                        + " form inputs in custom GlobalSettingsPage.html";
        assertEquals("unused", tag.getAttribute("class"), msg);
    }
}
