/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.restconfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.list;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
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
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogFactory;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ProjectionPolicy;
import org.geoserver.catalog.SLDHandler;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.cloud.autoconfigure.extensions.test.ConditionalTestAutoConfiguration;
import org.geoserver.cloud.gwc.config.core.GwcRequestPathInfoFilter;
import org.geoserver.config.GeoServer;
import org.geoserver.config.util.XStreamPersisterInitializer;
import org.geoserver.gwc.GWC;
import org.geoserver.inspire.InspireXStreamPersisterInitializer;
import org.geoserver.ogcapi.LinkInfo;
import org.geoserver.ogcapi.OGCAPIXStreamPersisterInitializer;
import org.geoserver.ogcapi.impl.LinkInfoImpl;
import org.geoserver.ogcapi.v1.features.FeatureConformance;
import org.geoserver.ogcapi.v1.features.FeatureServiceXStreamPersisterInitializer;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.WFSXStreamPersisterInitializer;
import org.geotools.data.wfs.internal.v2_0.storedquery.StoredQueryConfiguration;
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

    @Autowired
    protected GeoServer geoServer;

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
        ft.setProjectionPolicy(ProjectionPolicy.NONE);
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

    /**
     * Serializing a stored configuration object depends on an {@link XStreamPersisterInitializer} that knows its type.
     * Those beans used to be registered only by the service that consumes them, leaving this one to rewrite complex
     * metadata map values as their {@code toString()}. See issue #872.
     */
    @Test
    void configSerializationInitializersPresent() {
        assertThat(context.getBeansOfType(XStreamPersisterInitializer.class).values())
                .as("every service must be able to serialize stored configuration objects (issue #872)")
                .hasAtLeastOneElementOfType(WFSXStreamPersisterInitializer.class)
                .hasAtLeastOneElementOfType(OGCAPIXStreamPersisterInitializer.class)
                .hasAtLeastOneElementOfType(FeatureServiceXStreamPersisterInitializer.class)
                .hasAtLeastOneElementOfType(InspireXStreamPersisterInitializer.class);
    }

    /**
     * Reading the WFS settings and writing the same document back used to replace the OGC API Features conformance
     * object with the string returned by its {@code toString()}, breaking the WFS service with a
     * {@code ClassCastException}. See issue #872.
     */
    @Test
    void wfsSettingsRoundTripPreservesOgcApiFeaturesConformance() {
        try {
            storeFeatureConformance();

            String settings = getWfsSettings();
            putWfsSettings(settings);

            WFSInfo wfs = geoServer.getService(WFSInfo.class);
            assertThat(wfs.getMetadata().get(FeatureConformance.METADATA_KEY))
                    .as("a REST settings round trip must preserve the conformance object type (issue #872)")
                    .isInstanceOf(FeatureConformance.class);

            FeatureConformance conformance = FeatureConformance.configuration(wfs);
            assertThat(conformance.isCore()).isTrue();
            assertThat(conformance.isPropertySelection()).isTrue();
        } finally {
            removeFeatureConformance();
        }
    }

    /**
     * The {@code ogcApiLinks} metadata entry of a resource used to come back from a REST round trip as the string
     * returned by {@code ArrayList.toString()}. See issue #872.
     */
    @Test
    void featureTypeRoundTripPreservesOgcApiLinks(@TempDir Path storeDirectory) throws IOException {
        try {
            createVectorLayer(storeDirectory, "ogcapilinks", "ogcapilinksstore", "roads");
            storeOgcApiLinks("ogcapilinks", "roads");

            roundTripFeatureType("ogcapilinks", "ogcapilinksstore", "roads");

            Object links = catalog.getFeatureTypeByName("ogcapilinks", "roads")
                    .getMetadata()
                    .get(LinkInfo.LINKS_METADATA_KEY);
            assertThat(links)
                    .as("a REST round trip must preserve the OGC API links (issue #872)")
                    .asInstanceOf(list(LinkInfo.class))
                    .singleElement()
                    .extracting(LinkInfo::getHref)
                    .isEqualTo("http://example.com/roads.gpkg");
        } finally {
            dropVectorLayerTree("ogcapilinks", "ogcapilinksstore", "roads");
        }
    }

    /**
     * The {@code storedQueryConfiguration} metadata entry of a cascaded WFS feature type used to come back from a REST
     * round trip as a string. See issue #872.
     */
    @Test
    void featureTypeRoundTripPreservesStoredQueryConfiguration(@TempDir Path storeDirectory) throws IOException {
        try {
            createVectorLayer(storeDirectory, "storedquery", "storedquerystore", "roads");
            storeStoredQueryConfiguration("storedquery", "roads");

            roundTripFeatureType("storedquery", "storedquerystore", "roads");

            Object configuration = catalog.getFeatureTypeByName("storedquery", "roads")
                    .getMetadata()
                    .get(FeatureTypeInfo.STORED_QUERY_CONFIGURATION);
            assertThat(configuration)
                    .as("a REST round trip must preserve the cascaded stored query configuration (issue #872)")
                    .asInstanceOf(type(StoredQueryConfiguration.class))
                    .extracting(StoredQueryConfiguration::getStoredQueryId)
                    .isEqualTo("urn:ogc:def:query:OGC-WFS::GetFeatureById");
        } finally {
            dropVectorLayerTree("storedquery", "storedquerystore", "roads");
        }
    }

    private void storeOgcApiLinks(String workspace, String featureType) {
        ArrayList<LinkInfo> links = new ArrayList<>();
        links.add(new LinkInfoImpl("enclosure", "application/geopackage+sqlite3", "http://example.com/roads.gpkg"));

        FeatureTypeInfo info = catalog.getFeatureTypeByName(workspace, featureType);
        info.getMetadata().put(LinkInfo.LINKS_METADATA_KEY, links);
        catalog.save(info);
    }

    private void storeStoredQueryConfiguration(String workspace, String featureType) {
        StoredQueryConfiguration configuration = new StoredQueryConfiguration();
        configuration.setStoredQueryId("urn:ogc:def:query:OGC-WFS::GetFeatureById");

        FeatureTypeInfo info = catalog.getFeatureTypeByName(workspace, featureType);
        info.getMetadata().put(FeatureTypeInfo.STORED_QUERY_CONFIGURATION, configuration);
        catalog.save(info);
    }

    private void roundTripFeatureType(String workspace, String store, String featureType) {
        String uri = "/rest/workspaces/%s/datastores/%s/featuretypes/%s.json".formatted(workspace, store, featureType);

        ResponseEntity<String> get = restTemplate.getForEntity(uri, String.class);
        assertThat(get.getStatusCode()).isEqualTo(OK);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(APPLICATION_JSON);
        ResponseEntity<String> put =
                restTemplate.exchange(uri, PUT, new HttpEntity<>(get.getBody(), headers), String.class);
        assertThat(put.getStatusCode()).isEqualTo(OK);
    }

    private void storeFeatureConformance() {
        FeatureConformance conformance = new FeatureConformance();
        conformance.setCore(Boolean.TRUE);
        conformance.setPropertySelection(Boolean.TRUE);

        WFSInfo wfs = geoServer.getService(WFSInfo.class);
        wfs.getMetadata().put(FeatureConformance.METADATA_KEY, conformance);
        geoServer.save(wfs);
    }

    private void removeFeatureConformance() {
        WFSInfo wfs = geoServer.getService(WFSInfo.class);
        wfs.getMetadata().remove(FeatureConformance.METADATA_KEY);
        geoServer.save(wfs);
    }

    private String getWfsSettings() {
        ResponseEntity<String> response = restTemplate.getForEntity("/rest/services/wfs/settings.json", String.class);
        assertThat(response.getStatusCode()).isEqualTo(OK);
        return response.getBody();
    }

    private void putWfsSettings(String settings) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(APPLICATION_JSON);
        ResponseEntity<String> response = restTemplate.exchange(
                "/rest/services/wfs/settings", PUT, new HttpEntity<>(settings, headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(OK);
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
