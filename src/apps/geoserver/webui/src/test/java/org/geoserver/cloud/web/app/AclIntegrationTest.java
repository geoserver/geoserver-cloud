/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.web.app;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.apache.wicket.util.tester.WicketTester;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.GeoServerHomePage;
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
            "geoserver.acl.enabled: true",
            "geoserver.acl.client.basePath: http://localhost:9000/api",
            "geoserver.acl.client.startupCheck: false",
            "geoserver.acl.client.username: acluser",
            "geoserver.acl.client.password: s3cr3t",
            "geoserver.web-ui.acl.enabled: true",
            "logging.level.org.geoserver.acl: debug"
        })
@ActiveProfiles("test") // see bootstrap-test.yml
class AclIntegrationTest {

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

    @Test
    void HomePage_ACL_enabled_smoke_test() {
        login();
        GeoServerHomePage page = tester.startPage(GeoServerHomePage.class);
        assertNotNull(page);

        assertNotNull(app.getBean("securityCategory"));
        assertNotNull(app.getBean("aclServiceConfigPageMenuInfo"));
        assertNotNull(app.getBean("accessRulesACLPageMenuInfo"));
        assertNotNull(app.getBean("adminRulesAclPageMenuInfo"));
    }
}
