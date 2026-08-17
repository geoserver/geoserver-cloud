/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.wfs.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.cloud.autoconfigure.extensions.test.ConditionalTestAutoConfiguration;
import org.geotools.feature.NameImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.xmlunit.assertj3.XmlAssert;

@AutoConfigureTestRestTemplate
@SpringBootTest(classes = WfsApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
abstract class WfsApplicationTest {

    protected TestRestTemplate restTemplate = new TestRestTemplate("admin", "geoserver");

    @Autowired
    protected ApplicationContext context;

    @Autowired
    @Qualifier("rawCatalog")
    private Catalog catalog;

    @Test
    void owsGetCapabilitiesSmokeTest(@LocalServerPort int servicePort) {
        String url = "http://localhost:%d/ows?SERVICE=WFS&REQUEST=GETCAPABILITIES&VERSION=1.1.0".formatted(servicePort);
        String caps = restTemplate.getForObject(url, String.class);
        Map<String, String> nscontext = Map.of("wfs", "http://www.opengis.net/wfs");
        XmlAssert.assertThat(caps).withNamespaceContext(nscontext).hasXPath("/wfs:WFS_Capabilities");
    }

    @Test
    void wfsGetCapabilitiesSmokeTest(@LocalServerPort int servicePort) {
        String url = "http://localhost:%d/wfs?SERVICE=WFS&REQUEST=GETCAPABILITIES&VERSION=1.1.0".formatted(servicePort);
        String caps = restTemplate.getForObject(url, String.class);
        Map<String, String> nscontext = Map.of("wfs", "http://www.opengis.net/wfs");
        XmlAssert.assertThat(caps).withNamespaceContext(nscontext).hasXPath("/wfs:WFS_Capabilities");
    }

    @Test
    void wfsGetCapabilitiesOutputFormatsTest(@LocalServerPort int servicePort) {
        String url = "http://localhost:%d/wfs?SERVICE=WFS&REQUEST=GETCAPABILITIES&VERSION=1.1.0".formatted(servicePort);
        String caps = restTemplate.getForObject(url, String.class);

        Map<String, String> nscontext =
                Map.of("wfs", "http://www.opengis.net/wfs", "ows", "http://www.opengis.net/ows");
        XmlAssert xmlAssert = XmlAssert.assertThat(caps).withNamespaceContext(nscontext);
        xmlAssert.hasXPath(
                "//ows:Operation[@name='GetFeature']/ows:Parameter[@name='outputFormat']/ows:Value[text()='DXF']");
        xmlAssert.hasXPath(
                "//ows:Operation[@name='GetFeature']/ows:Parameter[@name='outputFormat']/ows:Value[text()='DXF-ZIP']");
        xmlAssert.hasXPath(
                "//ows:Operation[@name='GetFeature']/ows:Parameter[@name='outputFormat']/ows:Value[text()='application/flatgeobuf']");
    }

    /**
     * Tests the service-specific conditional annotations.
     *
     * <p>Verifies that only the WFS conditional bean is activated in this service, based on the
     * geoserver.service.wfs.enabled=true property set in bootstrap.yml. This test relies on the
     * ConditionalTestAutoConfiguration class from the extensions-core test-jar, which contains beans conditionally
     * activated based on each GeoServer service type.
     */
    @Test
    void testServiceConditionalAnnotations() {
        // This should exist in WFS service
        assertThat(context.containsBean("wfsConditionalBean")).isTrue();

        ConditionalTestAutoConfiguration.ConditionalTestBean bean =
                context.getBean("wfsConditionalBean", ConditionalTestAutoConfiguration.ConditionalTestBean.class);
        assertThat(bean.getServiceName()).isEqualTo("WFS");

        // These should not exist in WFS service
        assertThat(context.containsBean("wmsConditionalBean")).isFalse();
        assertThat(context.containsBean("wcsConditionalBean")).isFalse();
        assertThat(context.containsBean("wpsConditionalBean")).isFalse();
        assertThat(context.containsBean("restConditionalBean")).isFalse();
        assertThat(context.containsBean("webUiConditionalBean")).isFalse();
    }

    @Test
    void workspaceServiceGetFeatureWithUnqualifiedTypeName(@LocalServerPort int port) throws Exception {
        createTestWorkspaces();
        String wsa = getFeatureAsJson(port, "wsa");
        assertThat(wsa)
                .as("the wsa virtual service should not crash on the unqualified typeName")
                .doesNotContain("ClassCastException", "cannot be cast")
                .as("the wsa virtual service should return wsa's own 'roads' feature")
                .contains("FeatureCollection")
                .contains("wsa road")
                .doesNotContain("wsb road");

        String wsb = getFeatureAsJson(port, "wsb");
        assertThat(wsb)
                .as("the wsb virtual service should resolve the same unqualified name to its own feature")
                .contains("FeatureCollection")
                .contains("wsb road")
                .doesNotContain("wsa road");
    }

    private void createTestWorkspaces() throws Exception {
        if (catalog.getWorkspaceByName("base") != null
                && catalog.getWorkspaceByName("wsa") != null
                && catalog.getWorkspaceByName("wsb") != null) {
            return;
        }
        // "base" is added first and so becomes the default workspace; it does NOT publish "roads",
        // so the unqualified lookup is not short-circuited by the default namespace and must fall
        // back to ANY_NAMESPACE.
        addWorkspace("base");
        addWorkspace("wsa");
        addWorkspace("wsb");
        publishRoads("wsa", "wsa road");
        publishRoads("wsb", "wsb road");
    }

    private String getFeatureAsJson(int port, String workspace) {
        String url =
                "http://localhost:%d/%s/wfs?SERVICE=WFS&VERSION=2.0.0&REQUEST=GetFeature&TYPENAMES=roads&outputFormat=application/json"
                        .formatted(port, workspace);
        return restTemplate.getForObject(url, String.class);
    }

    private void addWorkspace(String name) {
        WorkspaceInfo ws = catalog.getFactory().createWorkspace();
        ws.setName(name);
        catalog.add(ws);
        NamespaceInfo ns = catalog.getFactory().createNamespace();
        ns.setPrefix(name);
        ns.setURI("http://%s.example".formatted(name));
        catalog.add(ns);
    }

    private void publishRoads(String workspaceName, String featureLabel) throws Exception {
        WorkspaceInfo ws = catalog.getWorkspaceByName(workspaceName);
        NamespaceInfo ns = catalog.getNamespaceByPrefix(workspaceName);

        Path dir = Files.createTempDirectory("propstore-" + workspaceName + "-");
        String properties =
                """
                _=id:Integer,name:String,geom:Point:srid=4326
                roads.1=1|%s|POINT(1 1)
                """
                        .formatted(featureLabel);
        Files.writeString(dir.resolve("roads.properties"), properties);

        DataStoreInfo store = catalog.getFactory().createDataStore();
        store.setName(workspaceName + "-roads-store");
        store.setWorkspace(ws);
        store.setType("Properties");
        store.getConnectionParameters().put("directory", dir.toFile().getAbsolutePath());
        store.getConnectionParameters().put("namespace", ns.getURI());
        store.setEnabled(true);
        catalog.add(store);

        CatalogBuilder cb = new CatalogBuilder(catalog);
        cb.setStore(store);
        FeatureTypeInfo ft = cb.buildFeatureType(new NameImpl(ns.getURI(), "roads"));
        cb.setupBounds(ft);
        catalog.add(ft);
        catalog.add(cb.buildLayer(ft));
    }
}
