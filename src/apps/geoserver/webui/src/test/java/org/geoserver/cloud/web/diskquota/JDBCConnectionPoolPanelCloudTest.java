/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.web.diskquota;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.Serial;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.WicketTester;
import org.geoserver.cloud.web.app.WebUIApplication;
import org.geoserver.gwc.web.diskquota.JDBCConnectionPoolPanel;
import org.geoserver.web.GeoServerApplication;
import org.geowebcache.diskquota.jdbc.JDBCConfiguration.ConnectionPoolConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Verifies that {@link JDBCConnectionPoolPanel}, exposed by the cloud webui module's
 * {@code DisquotaWebUIConfiguration}, renders inside the cloud Wicket harness without leaking the
 * password into the response markup.
 *
 * <p>Mirrors the upstream {@code JDBCConnectionPoolPanelTest} but uses the Spring-Boot-driven cloud
 * {@code GeoServerApplication}.
 */
@SpringBootTest(
        classes = WebUIApplication.class,
        properties = {
            "spring.cloud.bus.enabled: false",
            "spring.cloud.config.enabled: false",
            "spring.cloud.config.discovery.enabled: false",
            "eureka.client.enabled: false"
        })
@ActiveProfiles("test")
class JDBCConnectionPoolPanelCloudTest {

    private @Autowired GeoServerApplication app;
    private WicketTester tester;

    static @TempDir Path tmpdir;

    @DynamicPropertySource
    static void setUpDataDir(DynamicPropertyRegistry registry) throws IOException {
        Path datadir = tmpdir.resolve("datadir");
        Path gwcdir = datadir.resolve("gwc");
        Files.createDirectories(gwcdir);
        registry.add("geoserver.backend.data-directory.location", datadir::toAbsolutePath);
        registry.add("gwc.cache-directory", gwcdir::toAbsolutePath);
        // Enable disk-quota so DisquotaWebUIConfiguration imports the gs-web-gwc menu page bean.
        registry.add("gwc.disk-quota.enabled", () -> "true");
    }

    static @BeforeAll void beforeAll() {
        System.setProperty("wicket.configuration", "deployment");
        System.setProperty(GeoServerApplication.GEOSERVER_CSRF_DISABLED, "true");
        Locale.setDefault(Locale.ENGLISH);
    }

    @BeforeEach
    void setUpWicketTester() {
        tester = new WicketTester(app, true);
    }

    @AfterEach
    void tearDownWicketTester() {
        if (tester != null) {
            tester.destroy();
        }
    }

    @Test
    void renderingDoesNotLeakPassword() {
        ConnectionPoolConfiguration pool = new ConnectionPoolConfiguration();
        pool.setDriver("org.postgresql.Driver");
        pool.setUrl("jdbc:postgresql://example:5432/quota");
        pool.setUsername("sa");
        pool.setPassword("topsecret");
        pool.setMinConnections(1);
        pool.setMaxConnections(1);
        pool.setMaxOpenPreparedStatements(50);

        FormTestPage page = new FormTestPage(new JDBCConnectionPoolPanel("panel", new Model<>(pool)));
        tester.startPage(page);

        String body = tester.getLastResponseAsString();
        assertThat(body).as("password must not appear in rendered HTML").doesNotContain(pool.getPassword());
    }

    /**
     * Minimal Wicket test page that wraps a single panel in a Form. The companion HTML file lives
     * next to the compiled class so Wicket can locate the markup via classpath.
     */
    public static class FormTestPage extends WebPage {

        @Serial
        private static final long serialVersionUID = 1L;

        public FormTestPage(JDBCConnectionPoolPanel panel) {
            Form<?> form = new Form<>("form");
            add(form);
            form.add(panel);
        }
    }
}
