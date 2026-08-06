/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.restconfig;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import org.geoserver.cloud.backend.pgconfig.support.PgConfigTestContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

/**
 * Integration test for the parquetry GeoParquet extension in the rest-service app: GeoParquet files on S3-emulated
 * object storage (s3proxy), the pgconfig catalog backend, and the REST workflow the user guide documents in
 * {@code docs/src/user-guide/geoparquet-rest-api.md}: create the workspace and the datastore, discover the available
 * feature types, publish them, and read back the computed metadata.
 *
 * <p>The published feature types' CRS and native bounds are the integration proof: they exist only if parquetry
 * actually read the files through the s3proxy endpoint.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles({"test", "pgconfigjndi"})
@Testcontainers(disabledWithoutDocker = true)
class ParquetryRestApiIT {

    private static final String WORKSPACE = "naturalearth";
    private static final String STORE = "ne";
    private static final String BUCKET = "naturalearth";

    @Container
    static PgConfigTestContainer pgconfig = new PgConfigTestContainer();

    @Container
    @SuppressWarnings("resource")
    static GenericContainer<?> s3proxy = new GenericContainer<>("andrewgaul/s3proxy")
            .withEnv("S3PROXY_AUTHORIZATION", "none")
            .withEnv("S3PROXY_ENDPOINT", "http://0.0.0.0:80")
            .withEnv("JCLOUDS_PROVIDER", "filesystem")
            .withEnv("JCLOUDS_FILESYSTEM_BASEDIR", "/data")
            .withExposedPorts(80)
            .withCopyFileToContainer(
                    MountableFile.forClasspathResource("geoparquet/disputed_areas.parquet"),
                    "/data/" + BUCKET + "/disputed_areas.parquet")
            .withCopyFileToContainer(
                    MountableFile.forClasspathResource("geoparquet/populated_places.parquet"),
                    "/data/" + BUCKET + "/populated_places.parquet")
            .waitingFor(Wait.forListeningPort());

    @DynamicPropertySource
    static void setUpPgConfig(DynamicPropertyRegistry registry) {
        pgconfig.setupDynamicPropertySource(registry);
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void geoParquetStoreOverS3RestWorkflow() {
        TestRestTemplate admin = restTemplate.withBasicAuth("admin", "geoserver");

        createWorkspace(admin);
        createGeoParquetDataStore(admin);

        List<String> available = listAvailableFeatureTypes(admin);
        assertThat(available).containsExactlyInAnyOrder("disputed_areas", "populated_places");

        publishFeatureType(admin, "disputed_areas");
        publishFeatureType(admin, "populated_places");

        assertPublishedFeatureType(admin, "disputed_areas");
        assertPublishedFeatureType(admin, "populated_places");

        recalculateBounds(admin, "disputed_areas");
        assertPublishedFeatureType(admin, "disputed_areas");
    }

    /**
     * Forces a live re-read of the parquet file: recalculating bounds re-opens the datastore from the connection
     * parameters persisted in the catalog backend, proving the store survives the round-trip and the file is reachable
     * through the s3proxy endpoint after initial creation.
     */
    private void recalculateBounds(TestRestTemplate admin, String typeName) {
        String uri = "/rest/workspaces/%s/datastores/%s/featuretypes/%s?recalculate=nativebbox,latlonbbox"
                .formatted(WORKSPACE, STORE, typeName);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_XML);
        String body = "<featureType><name>%s</name></featureType>".formatted(typeName);
        ResponseEntity<String> response =
                admin.exchange(uri, HttpMethod.PUT, new HttpEntity<>(body, headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private void createWorkspace(TestRestTemplate admin) {
        String body = "<workspace><name>%s</name></workspace>".formatted(WORKSPACE);
        ResponseEntity<String> response = postXml(admin, "/rest/workspaces", body);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    private void createGeoParquetDataStore(TestRestTemplate admin) {
        String endpoint = "http://%s:%d".formatted(s3proxy.getHost(), s3proxy.getMappedPort(80));
        String body =
                """
                <dataStore>
                  <name>%s</name>
                  <type>Parquet</type>
                  <connectionParameters>
                    <entry key="geoparquet">s3://%s/</entry>
                    <entry key="layer-grouping">file</entry>
                    <entry key="storage.provider">s3</entry>
                    <entry key="storage.s3.endpoint">%s</entry>
                    <entry key="storage.s3.region">us-east-1</entry>
                    <entry key="storage.s3.anonymous">true</entry>
                  </connectionParameters>
                </dataStore>
                """
                        .formatted(STORE, BUCKET, endpoint);
        ResponseEntity<String> response = postXml(admin, "/rest/workspaces/%s/datastores".formatted(WORKSPACE), body);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    private List<String> listAvailableFeatureTypes(TestRestTemplate admin) {
        String uri = "/rest/workspaces/%s/datastores/%s/featuretypes.json?list=available".formatted(WORKSPACE, STORE);
        ResponseEntity<String> response = admin.getForEntity(uri, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        DocumentContext json = JsonPath.parse(response.getBody());
        return json.read("$.list.string");
    }

    private void publishFeatureType(TestRestTemplate admin, String typeName) {
        String body = "<featureType><name>%s</name></featureType>".formatted(typeName);
        String uri = "/rest/workspaces/%s/datastores/%s/featuretypes".formatted(WORKSPACE, STORE);
        ResponseEntity<String> response = postXml(admin, uri, body);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    /**
     * Reads back a published feature type and verifies the metadata parquetry computed from the file: the declared CRS
     * and a native bounding box within world bounds.
     */
    private void assertPublishedFeatureType(TestRestTemplate admin, String typeName) {
        String uri = "/rest/workspaces/%s/datastores/%s/featuretypes/%s.json".formatted(WORKSPACE, STORE, typeName);
        ResponseEntity<String> response = admin.getForEntity(uri, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        DocumentContext json = JsonPath.parse(response.getBody());
        String srs = json.read("$.featureType.srs");
        assertThat(srs).isEqualTo("EPSG:4326");

        double minx = ((Number) json.read("$.featureType.nativeBoundingBox.minx")).doubleValue();
        double maxx = ((Number) json.read("$.featureType.nativeBoundingBox.maxx")).doubleValue();
        double miny = ((Number) json.read("$.featureType.nativeBoundingBox.miny")).doubleValue();
        double maxy = ((Number) json.read("$.featureType.nativeBoundingBox.maxy")).doubleValue();
        assertThat(minx).isLessThan(maxx).isGreaterThanOrEqualTo(-180);
        assertThat(maxx).isLessThanOrEqualTo(180);
        assertThat(miny).isLessThan(maxy).isGreaterThanOrEqualTo(-90);
        assertThat(maxy).isLessThanOrEqualTo(90);
    }

    private ResponseEntity<String> postXml(TestRestTemplate admin, String uri, String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_XML);
        return admin.postForEntity(uri, new HttpEntity<>(body, headers), String.class);
    }
}
