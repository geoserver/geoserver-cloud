/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.gwc.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.METHOD_NOT_ALLOWED;
import static org.springframework.http.HttpStatus.OK;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * End-to-end integration test for the GeoWebCache {@code DiskQuotaController} on the pgconfig backend with disk-quota
 * enabled.
 *
 * <p>Boots the GWC microservice with the pgconfig backend pointing at a Postgres testcontainer and
 * {@code gwc.disk-quota.enabled=true}, then drives the {@code /gwc/rest/diskquota} REST API to verify the full GET/PUT
 * round-trip works. Exercises:
 *
 * <ol>
 *   <li>Flyway runs the {@code V3_1_0__DiskQuota_Tables.sql} migration in the pgconfig schema.
 *   <li>{@code PgconfigDiskQuotaAutoConfiguration} wires a {@code JDBCQuotaStore} on the pgconfig DataSource and
 *       supplies the {@code DiskQuotaStoreProvider} bean.
 *   <li>{@code DiskQuotaMonitor} starts up (the BFPP gate leaves {@code GWC_DISKQUOTA_DISABLED} cleared) and the REST
 *       controller's PUT applies a new {@code DiskQuotaConfig} that subsequent GETs see.
 * </ol>
 *
 * <p>The upstream {@code DiskQuotaController} chooses XML vs JSON via {@code request.getPathInfo().contains("json")}.
 * Spring Boot 4's path handling strips the {@code .json} suffix before the request reaches the controller, so the
 * format check always picks XML; this test exercises the controller via the (working) XML representation.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureTestRestTemplate
@Testcontainers(disabledWithoutDocker = true)
class DiskQuotaControllerPgconfigIT {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:15");

    static @TempDir Path datadir;

    @Autowired
    private TestRestTemplate restTemplate;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) throws IOException {
        Path gwcdir = datadir.resolve("gwc");
        if (!Files.exists(gwcdir)) {
            Files.createDirectory(gwcdir);
        }
        registry.add("gwc.cache-directory", gwcdir::toAbsolutePath);
        registry.add("gwc.disk-quota.enabled", () -> "true");
        registry.add("geoserver.backend.pgconfig.enabled", () -> "true");
        registry.add("geoserver.backend.pgconfig.jndi-name", () -> "pgconfig");
        registry.add("pgconfig.host", postgres::getHost);
        registry.add("pgconfig.port", () -> postgres.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT));
        registry.add("pgconfig.database", postgres::getDatabaseName);
        registry.add("pgconfig.schema", () -> "pgconfigdiskquota");
        registry.add("pgconfig.username", postgres::getUsername);
        registry.add("pgconfig.password", postgres::getPassword);
    }

    @Test
    void getDefaultDiskQuotaConfig() {
        TestRestTemplate authed = restTemplate.withBasicAuth("admin", "geoserver");
        ResponseEntity<String> resp = authed.getForEntity("/gwc/rest/diskquota", String.class);
        assertThat(resp.getStatusCode()).as("body: %s", resp.getBody()).isEqualTo(OK);
        String body = resp.getBody();
        assertThat(body)
                .contains("<org.geowebcache.diskquota.DiskQuotaConfig>")
                .contains("<enabled>")
                .contains("<globalQuota>");
    }

    /**
     * Note the GET/PUT root-element asymmetry, which is upstream GeoWebCache behavior (reproducible on a vanilla
     * GeoServer): the PUT body uses the documented {@code gwcQuotaConfiguration} root (the controller parses it with
     * {@code ConfigLoader}'s aliased XStream), but the GET response serializes the bean by its class name
     * ({@code org.geowebcache.diskquota.DiskQuotaConfig}) because the GET path doesn't apply that alias.
     */
    @Test
    void putThenGetRoundTripsCustomConfig() {
        TestRestTemplate authed = restTemplate.withBasicAuth("admin", "geoserver");

        String payload =
                """
                <gwcQuotaConfiguration>
                  <enabled>true</enabled>
                  <cacheCleanUpFrequency>5</cacheCleanUpFrequency>
                  <cacheCleanUpUnits>SECONDS</cacheCleanUpUnits>
                  <maxConcurrentCleanUps>4</maxConcurrentCleanUps>
                  <globalExpirationPolicyName>LFU</globalExpirationPolicyName>
                </gwcQuotaConfiguration>
                """;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_XML);
        ResponseEntity<String> putResp = authed.exchange(
                "/gwc/rest/diskquota", HttpMethod.PUT, new HttpEntity<>(payload, headers), String.class);
        assertThat(putResp.getStatusCode().is2xxSuccessful())
                .as("PUT status: %s, body: %s", putResp.getStatusCode(), putResp.getBody())
                .isTrue();

        ResponseEntity<String> getResp = authed.getForEntity("/gwc/rest/diskquota", String.class);
        assertThat(getResp.getStatusCode()).isEqualTo(OK);
        String body = getResp.getBody();
        assertThat(body)
                .contains("<enabled>true</enabled>")
                .contains("<cacheCleanUpFrequency>5</cacheCleanUpFrequency>")
                .contains("<cacheCleanUpUnits>SECONDS</cacheCleanUpUnits>")
                .contains("<maxConcurrentCleanUps>4</maxConcurrentCleanUps>")
                .contains("<globalExpirationPolicyName>LFU</globalExpirationPolicyName>");
    }

    @Test
    void putGlobalQuotaRoundTrips() {
        TestRestTemplate authed = restTemplate.withBasicAuth("admin", "geoserver");

        // The PUT parser reads the Quota as <value>/<units> via the Quota converter; the GET serializes the Quota by
        // reflection (an <id>/<bytes> pair, no converter), so 750 MiB is read back as 786432000 bytes.
        String payload =
                """
                <gwcQuotaConfiguration>
                  <globalQuota>
                    <value>750</value>
                    <units>MiB</units>
                  </globalQuota>
                </gwcQuotaConfiguration>
                """;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_XML);
        ResponseEntity<String> putResp = authed.exchange(
                "/gwc/rest/diskquota", HttpMethod.PUT, new HttpEntity<>(payload, headers), String.class);
        assertThat(putResp.getStatusCode().is2xxSuccessful())
                .as("PUT status: %s, body: %s", putResp.getStatusCode(), putResp.getBody())
                .isTrue();

        ResponseEntity<String> getResp = authed.getForEntity("/gwc/rest/diskquota", String.class);
        assertThat(getResp.getStatusCode()).isEqualTo(OK);
        assertThat(getResp.getBody())
                .as("750 MiB should read back as 786432000 bytes")
                .contains("<bytes>" + (750L * 1024 * 1024) + "</bytes>");
    }

    @Test
    void postIsMethodNotAllowed() {
        TestRestTemplate authed = restTemplate.withBasicAuth("admin", "geoserver");
        ResponseEntity<String> resp = authed.postForEntity("/gwc/rest/diskquota", null, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(METHOD_NOT_ALLOWED);
    }

    @Test
    void deleteIsMethodNotAllowed() {
        TestRestTemplate authed = restTemplate.withBasicAuth("admin", "geoserver");
        ResponseEntity<String> resp = authed.exchange("/gwc/rest/diskquota", HttpMethod.DELETE, null, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(METHOD_NOT_ALLOWED);
    }

    @Test
    void getWithoutCredentialsIsRejected() {
        ResponseEntity<String> resp = restTemplate.getForEntity("/gwc/rest/diskquota", String.class);
        assertThat(resp.getStatusCode().value())
                .as("unauthenticated status: %s", resp.getStatusCode())
                .isIn(401, 403);
    }
}
