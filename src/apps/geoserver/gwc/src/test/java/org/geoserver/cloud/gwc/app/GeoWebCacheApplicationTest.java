/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.gwc.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_XML;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.net.URI;
import java.util.concurrent.atomic.AtomicReference;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.NamespaceInfoImpl;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geoserver.ows.AbstractDispatcherCallback;
import org.geoserver.ows.LocalWorkspace;
import org.geoserver.ows.Request;
import org.geoserver.platform.Service;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureTestRestTemplate
@Import(GeoWebCacheApplicationTest.LocalWorkspaceProbe.class)
abstract class GeoWebCacheApplicationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    @Qualifier("rawCatalog")
    private Catalog catalog;

    @Autowired
    private LocalWorkspaceProbe localWorkspace;

    @BeforeEach
    void before() {
        restTemplate = restTemplate.withBasicAuth("admin", "geoserver");
        String rootUri = restTemplate.getRootUri();
        assertThat(rootUri).isNotEmpty();
    }

    @Test
    void testRESTDefaultContentType() {
        ResponseEntity<String> response = testGetRequestContentType("/gwc/rest/layers", APPLICATION_JSON);
        JsonElement parsed = JsonParser.parseString(response.getBody());
        assertThat(parsed.isJsonArray()).isTrue();
    }

    @Test
    void testRESTPathExtensionContentNegotiationJson() {
        ResponseEntity<String> response = testGetRequestContentType("/gwc/rest/layers.json", APPLICATION_JSON);
        JsonElement parsed = JsonParser.parseString(response.getBody());
        assertThat(parsed.isJsonArray()).isTrue();

        testGetRequestContentType("/gwc/rest/layers.xml", APPLICATION_XML);
    }

    @Test
    void testRESTPathExtensionContentNegotiationXml() {
        testGetRequestContentType("/gwc/rest/layers.xml", APPLICATION_XML);
    }

    // the tile layers listed by the capabilities of a workspace depend on the local workspace
    @Test
    void workspaceServiceSetsWorkspace() {
        addWorkspace("wmtsws");
        String uri = "/wmtsws/gwc/service/wmts?service=WMTS&version=1.0.0&request=GetCapabilities";

        ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(localWorkspace.lastWorkspaceName()).isEqualTo("wmtsws");
    }

    private void addWorkspace(String name) {
        if (null == catalog.getWorkspaceByName(name)) {
            WorkspaceInfo workspace = new WorkspaceInfoImpl();
            workspace.setName(name);
            catalog.add(workspace);
            NamespaceInfo namespace = new NamespaceInfoImpl();
            namespace.setPrefix(name);
            namespace.setURI("http://" + name);
            catalog.add(namespace);
        }
    }

    protected ResponseEntity<String> testGetRequestContentType(String uri, MediaType expected) {
        ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @Nullable MediaType contentType = response.getHeaders().getContentType();
        assertThat(contentType).isNotNull();
        assertThat(contentType.isCompatibleWith(expected))
                .as("expected %s got %s".formatted(expected, contentType))
                .isTrue();
        return response;
    }

    @Test
    void testPostSeedDoesNotThrowAmbiguousHandlerMapping() {
        String payload =
                """
                <seedRequest>
                  <name>workspace:layer</name>
                  <srs><number>3857</number></srs>
                  <zoomStart>0</zoomStart>
                  <zoomStop>8</zoomStop>
                  <format>image/png</format>
                  <type>reseed</type>
                  <threadCount>2</threadCount>
                </seedRequest>
                """;
        String uri = "/gwc/rest/seed/workspace:layer.xml";

        ResponseEntity<String> response = restTemplate.postForEntity(URI.create(uri), payload, String.class);
        HttpStatusCode statusCode = response.getStatusCode();
        String body = response.getBody();

        // SeedService will throw a 500 error when the layer is not found
        // it's ok we just need to check it doesn't result in an "ambiguous handler mapping" error
        // see org.geoserver.cloud.gwc.config.services.SeedController
        assertThat(statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(body).contains("Unknown layer workspace:layer");
    }

    /** Records the local workspace of the GWC service requests handled by GeoServer's dispatcher. */
    static class LocalWorkspaceProbe extends AbstractDispatcherCallback {

        private final AtomicReference<String> lastWorkspaceName = new AtomicReference<>();

        @Override
        public Service serviceDispatched(Request request, Service service) {
            WorkspaceInfo workspace = LocalWorkspace.get();
            lastWorkspaceName.set(null == workspace ? null : workspace.getName());
            return service;
        }

        @Nullable
        String lastWorkspaceName() {
            return lastWorkspaceName.get();
        }
    }
}
