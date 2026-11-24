/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.geoserver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.OK;

import java.nio.file.Path;
import org.geoserver.cloud.autoconfigure.catalog.event.UpdateSequenceController.UpdateSeq;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Verifies the update-sequence admin endpoint answers at {@code /admin/updatesequence} in the combined application,
 * where {@code geoserver.base-path} is empty on every role. Earlier, per-service applications set
 * {@code geoserver.base-path} to the service name; the endpoint therefore lived at
 * {@code /{service}/admin/updatesequence} instead.
 *
 * <p>The GET operation has no {@code @RolesAllowed} restriction, unlike the POST operation that advances the sequence:
 * this test documents that current, intentional behavior rather than a gap to close.
 */
@AutoConfigureTestRestTemplate
@SpringBootTest(
        classes = GeoServerApplication.class,
        webEnvironment = WebEnvironment.RANDOM_PORT,
        properties = {"geoserver.services=wfs"})
@ActiveProfiles("datadir")
class UpdateSequenceEndpointTest {

    static @TempDir Path datadir;

    @DynamicPropertySource
    static void setUpDataDir(DynamicPropertyRegistry registry) {
        registry.add("geoserver.backend.data-directory.location", datadir::toAbsolutePath);
        registry.add("gwc.cache-directory", () -> datadir.resolve("gwc").toAbsolutePath());
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void getRespondsAnonymouslyAtRootAdminPath(@LocalServerPort int port) {
        String url = "http://localhost:%d/admin/updatesequence".formatted(port);
        ResponseEntity<UpdateSeq> response = restTemplate.getForEntity(url, UpdateSeq.class);

        assertThat(response.getStatusCode()).isEqualTo(OK);
        UpdateSeq body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.observed()).isGreaterThanOrEqualTo(0L);
        assertThat(body.real()).isGreaterThanOrEqualTo(0L);
    }
}
