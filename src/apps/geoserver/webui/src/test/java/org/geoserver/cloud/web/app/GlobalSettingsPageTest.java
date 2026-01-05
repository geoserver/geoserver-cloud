/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.web.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTester;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.GeoServerHomePage;
import org.geoserver.web.GeoServerLoginPage;
import org.geoserver.web.admin.GlobalSettingsPage;
import org.geoserver.web.wicket.Select2DropDownChoice;
import org.geoserver.web.wicket.WicketHierarchyPrinter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(
        properties = {
            "spring.cloud.bus.enabled: false",
            "spring.cloud.config.enabled: false",
            "spring.cloud.config.discovery.enabled: false",
            "eureka.client.enabled: false"
        })
@ActiveProfiles("test") // see bootstrap-test.yml
class GlobalSettingsPageTest {

    private GeoServer gs;

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
    void setUpWicketTester() {
        boolean init = true;
        tester = new WicketTester(app, init);
    }

    @BeforeEach
    void reset() {
        gs = app.getGeoServer();
        GeoServerInfo info = gs.getGlobal();
        info.getSettings().setVerbose(false);
        gs.save(info);
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

    @SuppressWarnings("unused")
    private void print(Component component) {
        boolean dumpClass = true;
        boolean dumpValue = false;
        boolean dumpPath = true;
        WicketHierarchyPrinter.print(component, dumpClass, dumpValue, dumpPath);
    }

    @Test
    void testValues() {
        GeoServerInfo info = gs.getGlobal();

        login();
        tester.startPage(GlobalSettingsPage.class);
        tester.assertComponent("form:verbose", CheckBox.class);
        tester.assertModelValue("form:verbose", info.getSettings().isVerbose());

        tester.assertComponent(
                "form:webAdminInterfaceSettingsFragment:loggedInUserDisplayMode", Select2DropDownChoice.class);
        tester.assertModelValue(
                "form:webAdminInterfaceSettingsFragment:loggedInUserDisplayMode",
                info.getUserDetailsDisplaySettings().getLoggedInUserDisplayMode());
        tester.assertComponent("form:webAdminInterfaceSettingsFragment:showProfileColumnsInUserList", CheckBox.class);
        tester.assertModelValue(
                "form:webAdminInterfaceSettingsFragment:showProfileColumnsInUserList",
                info.getUserDetailsDisplaySettings().getShowProfileColumnsInUserList());
        tester.assertComponent("form:webAdminInterfaceSettingsFragment:emailDisplayMode", Select2DropDownChoice.class);
        tester.assertModelValue(
                "form:webAdminInterfaceSettingsFragment:emailDisplayMode",
                info.getUserDetailsDisplaySettings().getEmailDisplayMode());
        tester.assertComponent("form:webAdminInterfaceSettingsFragment:revealEmailAtClick", CheckBox.class);
        tester.assertModelValue(
                "form:webAdminInterfaceSettingsFragment:revealEmailAtClick",
                info.getUserDetailsDisplaySettings().getRevealEmailAtClick());
        tester.assertComponent("form:webAdminInterfaceSettingsFragment:showCreatedTimeCols", CheckBox.class);
        tester.assertModelValue(
                "form:webAdminInterfaceSettingsFragment:showCreatedTimeCols",
                info.getSettings().isShowCreatedTimeColumnsInAdminList());
        tester.assertComponent("form:webAdminInterfaceSettingsFragment:showModifiedTimeCols", CheckBox.class);
        tester.assertModelValue(
                "form:webAdminInterfaceSettingsFragment:showModifiedTimeCols",
                info.getSettings().isShowModifiedTimeColumnsInAdminList());
        tester.assertComponent("form:webAdminInterfaceSettingsFragment:showModifiedByCols", CheckBox.class);
        tester.assertModelValue(
                "form:webAdminInterfaceSettingsFragment:showModifiedByCols",
                info.getSettings().isShowModifiedUserInAdminList());

        tester.assertComponent("form:trailingSlashMatch", CheckBox.class);
        tester.assertModelValue("form:trailingSlashMatch", info.isTrailingSlashMatch());
    }

    @Test
    void testSave() {
        login();
        tester.startPage(GlobalSettingsPage.class);
        FormTester ft = tester.newFormTester("form");
        ft.setValue("verbose", true);
        ft.setValue("trailingSlashMatch", false);
        ft.submit("submit");

        tester.assertRenderedPage(GeoServerHomePage.class);
        assertTrue(gs.getSettings().isVerbose());
        assertFalse(gs.getGlobal().isTrailingSlashMatch());
    }

    @Test
    void testApply() {
        login();
        tester.startPage(GlobalSettingsPage.class);
        FormTester ft = tester.newFormTester("form");
        ft.setValue("verbose", true);
        ft.setValue("trailingSlashMatch", false);
        ft.submit("apply");

        tester.assertRenderedPage(GlobalSettingsPage.class);
        assertTrue(gs.getSettings().isVerbose());
        assertFalse(gs.getGlobal().isTrailingSlashMatch());
    }

    @Test
    void testDefaultLocale() {
        login();
        tester.startPage(GlobalSettingsPage.class);
        FormTester ft = tester.newFormTester("form");
        ft.select("defaultLocale", 10);
        ft.submit("submit");
        assertNotNull(gs.getSettings().getDefaultLocale());
    }

    @Test
    void testLoggedOut() {
        logout();
        tester.startPage(GlobalSettingsPage.class);
        tester.assertRenderedPage(GeoServerLoginPage.class);
    }

    @Test
    void testHiddenComponentsForGeoServerCloud() {
        login();
        tester.startPage(GlobalSettingsPage.class);
        tester.assertRenderedPage(GlobalSettingsPage.class);
        GlobalSettingsPage page = (GlobalSettingsPage) tester.getLastRenderedPage();
        assertNotNull(page);
        assertHidden("proxyBaseUrlContainer");
        assertHidden("useHeadersProxyURL");
        assertHidden("loggingSettingsContainer");
        assertHidden("lockProviderContainer");
        assertHidden("webUISettingsContainer");
    }

    protected void assertHidden(String id) {
        TagTester tag = tester.getTagById(id);
        String msg = "expected custom 'unused' css class to hide the %s form inputs in custom GlobalSettingsPage.html"
                .formatted(id);
        assertEquals("unused", tag.getAttribute("class"), msg);
    }
}
