/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.geoserver;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import org.geoserver.cloud.security.gateway.sharedauth.GatewaySharedAuthenticationFilter;
import org.geoserver.security.impl.GsCloudLayerGroupContainmentCache;
import org.geoserver.security.impl.LayerGroupContainmentCache;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Verifies the combined application's out-of-the-box defaults are safe to run as a single, gateway-less instance: every
 * setting a microservices deployment must externalize to turn on (a message broker, gateway-trusted headers) stays off
 * until the deployer opts in.
 *
 * <p>Deliberately does not use the {@code test} Spring profile: {@code application-test.yml} sets
 * {@code spring.cloud.bus.enabled=false} itself, which would outrank the shipped
 * {@code geoserver_spring_cloud_profiles.yml} default and defeat the point of {@link #busDisabledByDefault} - this test
 * exists to guard the shipped default, not a test-only override of it. Only the properties genuinely needed to boot
 * {@code services=all} outside the {@code test} profile are inlined below.
 */
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
class MonolithSafeDefaultsTest {

    static @TempDir Path datadir;

    @DynamicPropertySource
    static void setUpDataDir(DynamicPropertyRegistry registry) {
        registry.add("geoserver.backend.data-directory.location", datadir::toAbsolutePath);
        registry.add("gwc.cache-directory", () -> datadir.resolve("gwc").toAbsolutePath());
    }

    @Autowired
    private ApplicationContext context;

    /**
     * Trusts {@code x-gsc-*} request headers for authentication, which is only safe behind a gateway that strips them
     * from client requests. Must stay off by default, see {@code geoserver.extension.security.gateway-shared-auth}.
     */
    @Test
    void sharedAuthFilterBeanAbsent() {
        assertThat(context.getBeansOfType(GatewaySharedAuthenticationFilter.class))
                .isEmpty();
    }

    /**
     * The health endpoint must report {@code UP} out of the box, without a message broker to connect to: a fresh
     * single-pod deployment with no externalized configuration must pass a liveness/readiness probe.
     */
    @Test
    void healthEndpointUpWithoutBroker(@LocalServerPort int port) {
        TestRestTemplate rest = new TestRestTemplate();
        ResponseEntity<String> health =
                rest.getForEntity("http://localhost:%d/actuator/health".formatted(port), String.class);
        assertThat(health.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(health.getBody()).contains("\"status\":\"UP\"");
    }

    /**
     * The Spring Cloud Bus, which needs a message broker, must stay off until a multi-pod deployment externalizes
     * config to enable it.
     */
    @Test
    void busDisabledByDefault(@Autowired Environment env) {
        Boolean springCloudBusEnabled = env.getProperty("spring.cloud.bus.enabled", Boolean.class);
        Boolean geoServerBusEnabled = env.getProperty("geoserver.bus.enabled", Boolean.class);
        // no default argument: Spring Cloud Bus's own matchIfMissing is true, meaning an absent
        // property here must fail this test rather than silently read as false
        assertThat(springCloudBusEnabled).isNotNull().isFalse();
        assertThat(geoServerBusEnabled).isNotNull().isFalse();
    }

    /**
     * The security rules of a layer group reach its layers through the layer group containment cache. An instance
     * started with no configuration builds it.
     */
    @Test
    void containmentCacheOnByDefault() {
        LayerGroupContainmentCache cache = context.getBean(LayerGroupContainmentCache.class);

        assertThat(cache).isInstanceOf(GsCloudLayerGroupContainmentCache.class);
    }

    /**
     * The ACL extension needs a companion service and stays off until the deployer opts in through externalized
     * configuration or environment variables; the jar defines no default for it.
     */
    @Test
    void aclExtensionUnsetByDefault(@Autowired Environment env) {
        assertThat(env.getProperty("geoserver.extension.security.acl.enabled")).isNull();
        assertThat(env.getProperty("geoserver.acl.client.enabled")).isNull();
    }
}
