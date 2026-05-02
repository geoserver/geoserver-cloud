/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.gwc.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.OK;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * End-to-end integration test for the GeoWebCache {@code DiskQuotaController} on the {@code
 * pgconfig-diskquota} profile.
 *
 * <p>Boots the GWC microservice with the pgconfig backend pointing at a Postgres testcontainer
 * and {@code gwc.disk-quota.enabled=true}, then drives the {@code /gwc/rest/diskquota} REST API
 * to verify the full GET/PUT round-trip works. This exercises the whole stack:
 *
 * <ol>
 *   <li>Flyway runs the {@code V3_1_0__DiskQuota_Tables.sql} migration in the pgconfig schema.
 *   <li>{@code PgconfigDiskQuotaAutoConfiguration} wires a {@code JDBCQuotaStore} on the pgconfig
 *       DataSource and replaces the upstream {@code DiskQuotaStoreProvider}.
 *   <li>{@code DiskQuotaMonitor} starts up (env post-processor leaves {@code GWC_DISKQUOTA_DISABLED}
 *       unset) and the REST controller's PUT applies a new {@code DiskQuotaConfig} that subsequent
 *       GETs see.
 * </ol>
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ActiveProfiles({"test", "pgconfig-diskquota"})
@Testcontainers(disabledWithoutDocker = true)
class DiskQuotaControllerPgconfigIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

    @Autowired
    private TestRestTemplate restTemplate;

    static @TempDir Path datadir;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) throws IOException {
        Path gwcdir = datadir.resolve("gwc");
        if (!Files.exists(gwcdir)) {
            Files.createDirectory(gwcdir);
        }
        registry.add("geoserver.backend.data-directory.location", () -> datadir.toAbsolutePath()
                .toString());
        registry.add("gwc.cache-directory", () -> gwcdir.toAbsolutePath().toString());
        registry.add("pgconfig.host", postgres::getHost);
        registry.add("pgconfig.port", () -> postgres.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT));
        registry.add("pgconfig.database", postgres::getDatabaseName);
        registry.add("pgconfig.schema", () -> "pgconfig");
        registry.add("pgconfig.username", postgres::getUsername);
        registry.add("pgconfig.password", postgres::getPassword);
    }

    @Test
    @Order(1)
    @DirtiesContext
    void smokeTest() {
        // separate test to absorb the first-request 403 quirk seen in GeoWebCacheApplicationTest
        TestRestTemplate authed = restTemplate.withBasicAuth("admin", "geoserver");
        ResponseEntity<String> resp = authed.getForEntity("/gwc/rest/layers", String.class);
        assertThat(resp.getStatusCode().is2xxSuccessful()
                        || resp.getStatusCode().value() == 403)
                .as("first call status: %s", resp.getStatusCode())
                .isTrue();
    }

    @Test
    @Order(2)
    void getDefaultDiskQuotaConfigJson() {
        TestRestTemplate authed = restTemplate.withBasicAuth("admin", "geoserver");
        ResponseEntity<String> resp = authed.getForEntity("/gwc/rest/diskquota.json", String.class);
        assertThat(resp.getStatusCode()).isEqualTo(OK);
        assertThat(resp.getHeaders().getContentType()).isNotNull();
        JsonObject root = JsonParser.parseString(resp.getBody()).getAsJsonObject();
        assertThat(root.has("org.geowebcache.diskquota.DiskQuotaConfig")).isTrue();
        JsonObject cfg = root.getAsJsonObject("org.geowebcache.diskquota.DiskQuotaConfig");
        assertThat(cfg.has("enabled")).isTrue();
    }

    @Test
    @Order(3)
    void putThenGetRoundTripsCustomConfig() {
        TestRestTemplate authed = restTemplate.withBasicAuth("admin", "geoserver");

        String payload =
                """
                {
                  "org.geowebcache.diskquota.DiskQuotaConfig": {
                    "enabled": true,
                    "cacheCleanUpFrequency": 5,
                    "cacheCleanUpUnits": "SECONDS",
                    "maxConcurrentCleanUps": 4,
                    "globalExpirationPolicyName": "LFU"
                  }
                }
                """;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> putResp = authed.exchange(
                "/gwc/rest/diskquota.json", HttpMethod.PUT, new HttpEntity<>(payload, headers), String.class);
        assertThat(putResp.getStatusCode().is2xxSuccessful())
                .as("PUT status: %s, body: %s", putResp.getStatusCode(), putResp.getBody())
                .isTrue();

        ResponseEntity<String> getResp = authed.getForEntity("/gwc/rest/diskquota.json", String.class);
        assertThat(getResp.getStatusCode()).isEqualTo(OK);
        JsonObject cfg = JsonParser.parseString(getResp.getBody())
                .getAsJsonObject()
                .getAsJsonObject("org.geowebcache.diskquota.DiskQuotaConfig");

        assertThat(cfg.get("enabled").getAsBoolean()).isTrue();
        assertThat(cfg.get("cacheCleanUpFrequency").getAsInt()).isEqualTo(5);
        assertThat(cfg.get("cacheCleanUpUnits").getAsString()).isEqualTo("SECONDS");
        assertThat(cfg.get("maxConcurrentCleanUps").getAsInt()).isEqualTo(4);
        assertThat(cfg.get("globalExpirationPolicyName").getAsString()).isEqualTo("LFU");
    }
}
