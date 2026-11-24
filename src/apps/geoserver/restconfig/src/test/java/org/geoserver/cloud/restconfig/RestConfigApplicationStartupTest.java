/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.restconfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.OK;

import java.nio.file.Path;
import org.geoserver.cloud.geoserver.GeoServerApplication;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@AutoConfigureTestRestTemplate
@SpringBootTest(
        classes = GeoServerApplication.class,
        webEnvironment = WebEnvironment.RANDOM_PORT,
        properties = {
            "spring.main.banner-mode=off",
            "spring.main.allow-bean-definition-overriding=true",
            // false by default since spring-boot 2.6.0, breaks GeoServer initialization
            "spring.main.allow-circular-references=true",
            "management.server.port="
        })
@ActiveProfiles(RestConfigApplication.SERVICE)
class RestConfigApplicationStartupTest {

    static @TempDir Path datadir;

    @DynamicPropertySource
    static void setUpDataDir(DynamicPropertyRegistry registry) {
        registry.add("geoserver.backend.data-directory.location", datadir::toAbsolutePath);
        registry.add("gwc.cache-directory", () -> datadir.resolve("gwc").toAbsolutePath());
    }

    @Autowired
    private Environment env;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void serviceName() {
        assertThat(env.getProperty("spring.application.name")).isEqualTo("restconfig");
    }

    @Test
    void serviceRole() {
        assertThat(env.getProperty("geoserver.services")).isEqualTo("restconfig");
    }

    // the update sequence endpoint is served under the base path of the service
    @Test
    void basePath() {
        TestRestTemplate admin = restTemplate.withBasicAuth("admin", "geoserver");

        ResponseEntity<String> response = admin.getForEntity("/rest/admin/updatesequence", String.class);

        assertThat(response.getStatusCode()).isEqualTo(OK);
    }

    @Test
    void localPort() {
        Environment local = localEnvironment();

        assertThat(local.getProperty("server.port")).isEqualTo("9105");
        assertThat(local.getProperty("management.server.port")).isEqualTo("8105");
    }

    /** The environment of the service under the {@code local} profile, without starting the service */
    private Environment localEnvironment() {
        SpringApplicationBuilder builder = new SpringApplicationBuilder(EmptyConfig.class)
                .web(WebApplicationType.NONE)
                .bannerMode(Banner.Mode.OFF)
                .profiles(RestConfigApplication.SERVICE, "local");
        try (ConfigurableApplicationContext context = builder.run("--spring.cloud.config.enabled=false")) {
            return context.getEnvironment();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class EmptyConfig {}
}
