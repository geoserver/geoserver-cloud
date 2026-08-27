/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.restconfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_XML;
import static org.springframework.http.MediaType.TEXT_HTML;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogFactory;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.SLDHandler;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.cloud.autoconfigure.extensions.test.ConditionalTestAutoConfiguration;
import org.geoserver.cloud.gwc.config.core.GwcRequestPathInfoFilter;
import org.geoserver.gwc.GWC;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
abstract class RestConfigApplicationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    protected ConfigurableApplicationContext context;

    @Autowired
    protected Catalog catalog;

    @BeforeEach
    void before() {
        restTemplate = restTemplate.withBasicAuth("admin", "geoserver");
    }

    @Test
    void gwcCoreIntegrationPresent() {
        assertThat(context.containsBean("gwcFacade"))
                .as("the GWC mediator bean must exist for tile layers to follow catalog changes (issue #519)")
                .isTrue();
        assertThat(context.containsBean("gwcCatalogConfiguration")).isTrue();
    }

    @Test
    void tileLayerFollowsCatalogLifecycle(@TempDir Path storeDirectory) throws IOException {
        GWC gwc = context.getBean("gwcFacade", GWC.class);
        assertThat(gwc.getConfig().isCacheLayersByDefault()).isTrue();

        try {
            LayerInfo layer = createVectorLayer(storeDirectory, "gwclifecycle", "gwclifecyclestore", "roads");

            assertThat(gwc.tileLayerExists("gwclifecycle:roads"))
                    .as("adding a layer must create its tile layer")
                    .isTrue();

            FeatureTypeInfo toRename = catalog.getFeatureTypeByName("gwclifecycle", "roads");
            toRename.setName("streets");
            catalog.save(toRename);

            assertThat(gwc.tileLayerExists("gwclifecycle:streets"))
                    .as("renaming the resource must rename the tile layer")
                    .isTrue();
            assertThat(gwc.tileLayerExists("gwclifecycle:roads")).isFalse();

            catalog.remove(catalog.getLayer(layer.getId()));
            catalog.remove(catalog.getFeatureTypeByName("gwclifecycle", "streets"));

            assertThat(gwc.tileLayerExists("gwclifecycle:streets"))
                    .as("removing the layer must remove its tile layer")
                    .isFalse();
        } finally {
            dropVectorLayerTree("gwclifecycle", "gwclifecyclestore", "streets", "roads");
        }
    }

    @Test
    void tileLayerFollowsWorkspaceRename(@TempDir Path storeDirectory) throws IOException {
        GWC gwc = context.getBean("gwcFacade", GWC.class);

        try {
            createVectorLayer(storeDirectory, "gwcwsrename", "gwcwsrenamestore", "roads");
            assertThat(gwc.tileLayerExists("gwcwsrename:roads")).isTrue();

            WorkspaceInfo ws = catalog.getWorkspaceByName("gwcwsrename");
            ws.setName("gwcwsrenamed");
            catalog.save(ws);
            NamespaceInfo ns = catalog.getNamespaceByPrefix("gwcwsrename");
            if (ns != null) {
                ns.setPrefix("gwcwsrenamed");
                catalog.save(ns);
            }

            assertThat(gwc.tileLayerExists("gwcwsrenamed:roads"))
                    .as("renaming the workspace must rename its tile layers")
                    .isTrue();
            assertThat(gwc.tileLayerExists("gwcwsrename:roads")).isFalse();
        } finally {
            dropVectorLayerTree("gwcwsrenamed", "gwcwsrenamestore", "roads");
            dropVectorLayerTree("gwcwsrename", "gwcwsrenamestore", "roads");
        }
    }

    @Test
    void tileLayerFollowsStoreMoveToAnotherWorkspace(@TempDir Path storeDirectory) throws IOException {
        GWC gwc = context.getBean("gwcFacade", GWC.class);

        try {
            createVectorLayer(storeDirectory, "gwcmovesource", "gwcmovestore", "roads");
            assertThat(gwc.tileLayerExists("gwcmovesource:roads")).isTrue();

            WorkspaceInfo targetWs = catalog.getFactory().createWorkspace();
            targetWs.setName("gwcmovetarget");
            NamespaceInfo targetNs = catalog.getFactory().createNamespace();
            targetNs.setPrefix("gwcmovetarget");
            targetNs.setURI("http://gwcmovetarget.test");
            catalog.add(targetWs);
            catalog.add(targetNs);

            DataStoreInfo store = catalog.getDataStoreByName("gwcmovesource", "gwcmovestore");
            store.setWorkspace(catalog.getWorkspaceByName("gwcmovetarget"));
            catalog.save(store);

            assertThat(gwc.tileLayerExists("gwcmovetarget:roads"))
                    .as("relocating the store must rename the tile layers of its layers")
                    .isTrue();
            assertThat(gwc.tileLayerExists("gwcmovesource:roads")).isFalse();
        } finally {
            dropVectorLayerTree("gwcmovetarget", "gwcmovestore", "roads");
            dropVectorLayerTree("gwcmovesource", "gwcmovestore", "roads");
        }
    }

    /**
     * Creates a workspace, namespace, property-file datastore, feature type, and layer, returning the layer. The
     * datastore is backed by a real property file to make the layer loadable, as required for automatic tile layer
     * creation.
     */
    private LayerInfo createVectorLayer(Path storeDirectory, String wsName, String storeName, String ftName)
            throws IOException {
        Files.writeString(
                storeDirectory.resolve(ftName + ".properties"),
                """
                _=geom:Point:srid=4326,name:String
                %s.1=POINT(1 1)|first street
                """
                        .formatted(ftName));

        CatalogFactory factory = catalog.getFactory();
        WorkspaceInfo ws = factory.createWorkspace();
        ws.setName(wsName);
        NamespaceInfo ns = factory.createNamespace();
        ns.setPrefix(wsName);
        ns.setURI("http://%s.test".formatted(wsName));
        catalog.add(ws);
        catalog.add(ns);

        DataStoreInfo store = factory.createDataStore();
        store.setWorkspace(catalog.getWorkspace(ws.getId()));
        store.setName(storeName);
        store.setEnabled(true);
        store.getConnectionParameters().put("directory", storeDirectory.toFile().getAbsolutePath());
        store.getConnectionParameters().put("namespace", ns.getURI());
        catalog.add(store);

        FeatureTypeInfo ft = factory.createFeatureType();
        ft.setStore(catalog.getDataStore(store.getId()));
        ft.setNamespace(catalog.getNamespace(ns.getId()));
        ft.setName(ftName);
        ft.setNativeName(ftName);
        ft.setSRS("EPSG:4326");
        ReferencedEnvelope world = new ReferencedEnvelope(-180, 180, -90, 90, DefaultGeographicCRS.WGS84);
        ft.setNativeBoundingBox(world);
        ft.setLatLonBoundingBox(world);
        ft.setEnabled(true);
        catalog.add(ft);

        LayerInfo layer = factory.createLayer();
        layer.setResource(catalog.getFeatureType(ft.getId()));
        catalog.add(layer);
        return catalog.getLayer(layer.getId());
    }

    /** Best-effort removal of the whole tree created by {@link #createVectorLayer}, never masking test outcomes. */
    private void dropVectorLayerTree(String wsName, String storeName, String... ftNames) {
        for (String ftName : ftNames) {
            removeQuietly(() -> catalog.getLayerByName(wsName + ":" + ftName), catalog::remove);
            removeQuietly(() -> catalog.getFeatureTypeByName(wsName, ftName), catalog::remove);
        }
        removeQuietly(() -> catalog.getDataStoreByName(wsName, storeName), catalog::remove);
        removeQuietly(() -> catalog.getNamespaceByPrefix(wsName), catalog::remove);
        removeQuietly(() -> catalog.getWorkspaceByName(wsName), catalog::remove);
    }

    /** Best-effort cleanup that never masks the test outcome. */
    private <T> void removeQuietly(Supplier<T> finder, Consumer<T> remover) {
        try {
            T found = finder.get();
            if (found != null) {
                remover.accept(found);
            }
        } catch (RuntimeException e) {
            // ignore, the assertion failure is the interesting outcome
        }
    }

    @Test
    void testAnnonymousForbidden() {
        restTemplate = restTemplate.withBasicAuth(null, null);
        ResponseEntity<String> response = restTemplate.getForEntity("/rest", String.class);
        assertThat(response.getStatusCode()).isEqualTo(FORBIDDEN);
    }

    @Test
    void testGatewaySharedAuthenticationForbidden() {
        restTemplate = restTemplate.withBasicAuth(null, null);

        HttpHeaders headers = new HttpHeaders();
        headers.set("x-gsc-username", "gabe");
        headers.set("x-gsc-roles", "ROLE_USER");

        ResponseEntity<String> response;

        response = restTemplate.exchange("/rest", GET, new HttpEntity<>(headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(FORBIDDEN);
    }

    @Test
    void testGatewaySharedAuthenticationAdmin() {
        restTemplate = restTemplate.withBasicAuth(null, null);

        HttpHeaders headers = new HttpHeaders();

        headers.set("x-gsc-username", "gabe");
        headers.set("x-gsc-roles", "ADMIN");
        ResponseEntity<String> response = restTemplate.exchange("/rest", GET, new HttpEntity<>(headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(OK);
    }

    @Test
    void testBasicAdminAccess() {
        testPathExtensionContentType("/rest", TEXT_HTML);
        testPathExtensionContentType("/rest/", TEXT_HTML);
        testPathExtensionContentType("/rest/index", TEXT_HTML);
    }

    @Test
    void testDefaultContentType() {
        testPathExtensionContentType("/rest/workspaces", APPLICATION_JSON);
        testPathExtensionContentType("/rest/layers", APPLICATION_JSON);
    }

    @Test
    void testPathExtensionContentNegotiation() {
        testPathExtensionContentType("/rest/styles/line.json", APPLICATION_JSON);
        testPathExtensionContentType("/rest/styles/line.xml", APPLICATION_XML);
        testPathExtensionContentType("/rest/styles/line.html", TEXT_HTML);
        testPathExtensionContentType("/rest/styles/line.sld", MediaType.valueOf(SLDHandler.MIMETYPE_10));

        testPathExtensionContentType("/rest/workspaces.html", TEXT_HTML);
        testPathExtensionContentType("/rest/workspaces.xml", APPLICATION_XML);
        testPathExtensionContentType("/rest/workspaces.json", APPLICATION_JSON);
    }

    /**
     * The REST and GWC path info filters must both be registered: they used to share the {@code
     * setRequestPathInfoFilter} bean name, and with bean definition overriding enabled the GWC one replaced the REST
     * one, breaking every {@code /rest/resource/**} request (issue #913).
     */
    @Test
    void restAndGwcPathInfoFiltersBothRegistered() {
        assertThat(context.getBean("restRequestPathInfoFilter"))
                .isInstanceOf(RestConfigApplicationConfiguration.SetRequestPathInfoFilter.class);
        assertThat(context.getBean("setRequestPathInfoFilter")).isInstanceOf(GwcRequestPathInfoFilter.class);
    }

    /**
     * Any {@code /rest/resource/**} path containing the {@code /gwc} character sequence failed, because the servlet
     * filters that rebuild {@code getPathInfo()} mistook such URIs for GeoWebCache requests. See issue #913.
     */
    @Test
    void testResourceEndpointPathContainingGwc() {
        ResponseEntity<String> response = restTemplate.getForEntity("/rest/resource/gwc-gs.xml", String.class);
        assertThat(response.getStatusCode()).isEqualTo(OK);
        assertThat(response.getBody()).contains("GeoServerGWCConfig");

        response = restTemplate.getForEntity("/rest/resource/gwcfoo", String.class);
        assertThat(response.getStatusCode()).isEqualTo(NOT_FOUND);
    }

    /** Directory variant of issue #913: listing a directory whose name starts with {@code gwc} */
    @Test
    void testResourceEndpointDirectoryNameContainingGwc() {
        try {
            putTextResource("/rest/resource/gwc913dir/child.txt", "issue #913 directory probe");

            ResponseEntity<String> listing =
                    restTemplate.getForEntity("/rest/resource/gwc913dir?format=json", String.class);
            assertThat(listing.getStatusCode()).isEqualTo(OK);
            assertThat(listing.getBody()).contains("child.txt");
        } finally {
            deleteResourceQuietly("/rest/resource/gwc913dir");
        }
    }

    /** Round trip through the resource REST API on a file path containing {@code /gwc}, see issue #913 */
    @Test
    void testResourceEndpointRoundTripOnGwcPrefixedFileName() {
        String file = "/rest/resource/styles/gwc-913-probe.txt";
        String contents = "issue #913 round trip probe";
        try {
            putTextResource(file, contents);

            ResponseEntity<String> get = restTemplate.getForEntity(file, String.class);
            assertThat(get.getStatusCode()).isEqualTo(OK);
            assertThat(get.getBody()).isEqualTo(contents);

            ResponseEntity<Void> delete = restTemplate.exchange(file, DELETE, null, Void.class);
            assertThat(delete.getStatusCode()).isEqualTo(OK);

            ResponseEntity<String> afterDelete = restTemplate.getForEntity(file, String.class);
            assertThat(afterDelete.getStatusCode()).isEqualTo(NOT_FOUND);
        } finally {
            deleteResourceQuietly(file);
        }
    }

    /**
     * The {@code format} parameter must decide the metadata representation even when the resource name has a
     * well-known file extension: the path-extension negotiation strategy used to win, asking for a
     * {@code text/plain} response no message converter can produce for the REST wrapper.
     */
    @Test
    void testResourceEndpointMetadataHonorsFormatParameter() {
        String file = "/rest/resource/resource_api_meta/probe.txt";
        try {
            putTextResource(file, "metadata probe");

            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", "*/*");
            ResponseEntity<String> metadata = restTemplate.exchange(
                    file + "?operation=metadata&format=json", GET, new HttpEntity<>(headers), String.class);
            assertThat(metadata.getStatusCode()).isEqualTo(OK);
            assertThat(metadata.getHeaders().getContentType()).isEqualTo(APPLICATION_JSON);
            assertThat(metadata.getBody()).contains("probe.txt");
        } finally {
            deleteResourceQuietly("/rest/resource/resource_api_meta");
        }
    }

    /**
     * Directory listings default to the html representation for any Accept header: without the {@code format}
     * parameter the Accept header used to decide, and clients preferring a type without a converter for the REST
     * wrapper, such as this test's {@code text/plain}, got a 500.
     */
    @Test
    void testResourceEndpointDirectoryHtmlListing() {
        try {
            putTextResource("/rest/resource/resource_api_html/child.txt", "html listing probe");

            ResponseEntity<String> listing =
                    restTemplate.getForEntity("/rest/resource/resource_api_html", String.class);
            assertThat(listing.getStatusCode()).isEqualTo(OK);
            assertThat(listing.getHeaders().getContentType()).asString().contains("text/html");
            assertThat(listing.getBody()).contains("child.txt");
        } finally {
            deleteResourceQuietly("/rest/resource/resource_api_html");
        }
    }

    private void putTextResource(String path, String contents) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        ResponseEntity<Void> put = restTemplate.exchange(path, PUT, new HttpEntity<>(contents, headers), Void.class);
        assertThat(put.getStatusCode()).isIn(OK, CREATED);
    }

    private void deleteResourceQuietly(String path) {
        try {
            restTemplate.exchange(path, DELETE, null, Void.class);
        } catch (RuntimeException e) {
            // ignore, the assertion failure is the interesting outcome
        }
    }

    protected void testPathExtensionContentType(String uri, MediaType expected) {
        ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
        assertThat(response.getStatusCode()).isEqualTo(OK);
        assertThat(response.getHeaders().getContentType()).isEqualTo(expected);
    }

    /**
     * Tests the service-specific conditional annotations.
     *
     * <p>
     * Verifies that only the REST conditional bean is activated in this service,
     * based on the geoserver.service.restconfig.enabled=true property set in bootstrap.yml.
     * This test relies on the ConditionalTestAutoConfiguration class from the
     * extensions-core test-jar, which contains beans conditionally activated
     * based on each GeoServer service type.
     */
    @Test
    void testServiceConditionalAnnotations() {
        // This should exist in REST service
        assertThat(context.containsBean("restConditionalBean")).isTrue();
        if (context.containsBean("restConditionalBean")) {
            ConditionalTestAutoConfiguration.ConditionalTestBean bean =
                    context.getBean("restConditionalBean", ConditionalTestAutoConfiguration.ConditionalTestBean.class);
            assertThat(bean.getServiceName()).isEqualTo("REST");
        }

        // These should not exist in REST service
        assertThat(context.containsBean("wfsConditionalBean")).isFalse();
        assertThat(context.containsBean("wcsConditionalBean")).isFalse();
        assertThat(context.containsBean("wmsConditionalBean")).isFalse();
        assertThat(context.containsBean("wpsConditionalBean")).isFalse();
        assertThat(context.containsBean("webUiConditionalBean")).isFalse();
    }
}
