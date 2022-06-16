/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.server.api.v1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_STREAM_JSON;
import java.io.IOException;
import java.util.List;
import org.geoserver.catalog.NamespaceInfo;
import org.junit.Test;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;

@AutoConfigureWebTestClient(timeout = "360000")
public class NamespaceControllerTest extends AbstractReactiveCatalogControllerTest<NamespaceInfo> {

    public NamespaceControllerTest() {
        super(NamespaceInfo.class);
    }

    protected @Override void assertPropertriesEqual(NamespaceInfo expected, NamespaceInfo actual) {
        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getPrefix(), actual.getPrefix());
        assertEquals(expected.getURI(), actual.getURI());
    }

    public @Override @Test void testFindAll() {
        super.testFindAll(testData.namespaceA, testData.namespaceB, testData.namespaceC);
    }

    public @Override @Test void testFindAllByType() {
        super.testFindAll(NamespaceInfo.class, testData.namespaceA, testData.namespaceB,
                testData.namespaceC);
    }

    public @Override @Test void testFindById() {
        super.testFindById(testData.namespaceA);
        super.testFindById(testData.namespaceB);
        super.testFindById(testData.namespaceC);
    }

    public @Override @Test void testQueryFilter() {
        NamespaceInfo ns1 = catalog.getNamespace(testData.namespaceA.getId());
        NamespaceInfo ns2 = catalog.getNamespace(testData.namespaceB.getId());
        NamespaceInfo ns3 = catalog.getNamespace(testData.namespaceC.getId());

        super.testQueryFilter(String.format("URI = '%s'", ns2.getURI()), ns2);

        ns3.setIsolated(true);
        ns3.setURI(ns1.getURI());
        catalog.save(ns3);

        super.testQueryFilter(String.format("URI = '%s'", ns1.getURI()), ns1, ns3);
    }

    public @Test void testFindNamespaceById() {
        testFindById(testData.namespaceA);
        testFindById(testData.namespaceB);
        testFindById(testData.namespaceC);
    }

    public @Test void testFindNamespacePrefix() {
        testFindByPrefix(testData.namespaceA);
        testFindByPrefix(testData.namespaceB);
        testFindByPrefix(testData.namespaceC);
    }

    private void testFindByPrefix(NamespaceInfo expected) {
        assertEquals(expected, client().getFirstByName(expected.getPrefix()));
    }

    public @Test void testNamespaceInfo_CRUD() throws IOException {
        NamespaceInfo ns = testData.faker().namespace();
        crudTest(ns, catalog::getNamespace, n -> n.setPrefix("modified-prefix"),
                (old, updated) -> assertEquals("modified-prefix", updated.getPrefix()));
    }

    public @Test void testSetDefaultNamespace() {
        assertEquals(testData.namespaceA, catalog.getDefaultNamespace());

        NamespaceInfo returned =
                client().put("/namespaces/default/{id}", testData.namespaceB.getId()).expectStatus()
                        .isOk().expectHeader().contentType(APPLICATION_JSON)
                        .expectBody(NamespaceInfo.class).returnResult().getResponseBody();

        assertEquals(testData.namespaceB, returned);
    }

    public @Test void testSetDefaultNamespaceNonExistent() {
        assertEquals(testData.namespaceA, catalog.getDefaultNamespace());

        client().put("/namespaces/default/{id}", "non-existent-id").expectStatus().isNoContent()
                .expectHeader().contentType(APPLICATION_JSON);
    }

    public @Test void testGetDefaultNamespace() {
        NamespaceInfo returned = client().getRelative("/namespaces/default").expectStatus().isOk()
                .expectHeader().contentType(APPLICATION_JSON).expectBody(NamespaceInfo.class)
                .returnResult().getResponseBody();

        assertEquals(testData.namespaceA, returned);
        catalog.setDefaultNamespace(testData.namespaceB);

        returned = client().getRelative("/namespaces/default").expectStatus().isOk().expectHeader()
                .contentType(APPLICATION_JSON).expectBody(NamespaceInfo.class).returnResult()
                .getResponseBody();

        assertEquals(testData.namespaceB, returned);
    }

    public @Test void testGetDefaultNamespaceNoDefaultExists() {
        testData.deleteAll();
        client().getRelative("/namespaces/default").expectStatus().isNoContent().expectHeader()
                .contentType(APPLICATION_JSON);
    }

    public @Test void testFindOneNamespaceByURI() {
        NamespaceInfo ns1 = testData.namespaceA;
        NamespaceInfo ns2 =
                testData.faker().namespace("second-ns-with-duplicate-uri", "prefix2", ns1.getURI());
        ns2.setIsolated(true);
        catalog.add(ns2);

        NamespaceInfo found = findByURI(ns1.getURI());
        // catalog.getNamespaceByURI() contract says it returns the first found with that uri...
        assertTrue(found.getId().equals(ns1.getId()) || found.getId().equals(ns2.getId()));
    }

    protected NamespaceInfo findByURI(String uri) {
        NamespaceInfo found = client().getRelative("/namespaces/uri?uri={uri}", uri).expectStatus()
                .isOk().expectHeader().contentType(APPLICATION_JSON).expectBody(NamespaceInfo.class)
                .returnResult().getResponseBody();
        return found;
    }

    public @Test void testFindAllNamespacesByURI() {
        NamespaceInfo ns1 = testData.namespaceA;
        NamespaceInfo ns2 =
                testData.faker().namespace("second-ns-with-duplicate-uri", "prefix2", ns1.getURI());
        ns2.setIsolated(true);
        catalog.add(ns2);

        List<NamespaceInfo> found =
                client().getRelative("/namespaces/uri/all?uri={uri}", ns1.getURI()).expectStatus()
                        .isOk().expectHeader().contentType(APPLICATION_STREAM_JSON)
                        .expectBodyList(NamespaceInfo.class).returnResult().getResponseBody();

        assertTrue(found.contains(ns1));
        assertTrue(found.contains(ns2));
        assertEquals(2, found.size());
    }

    public @Test void testCreateNamespaceDuplicateURI() {
        NamespaceInfo ns1 = testData.namespaceA;
        NamespaceInfo ns2 =
                testData.faker().namespace("second-ns-with-duplicate-uri", "prefix2", ns1.getURI());
        client().create(ns2).expectStatus().isBadRequest();

        ns2.setIsolated(true);
        NamespaceInfo created = client().create(ns2).expectStatus().isCreated().expectHeader()
                .contentType(APPLICATION_JSON).expectBody(NamespaceInfo.class).returnResult()
                .getResponseBody();
        assertNotNull(created.getId());
        assertEquals(ns2.getPrefix(), created.getPrefix());
        assertEquals(ns1.getURI(), created.getURI());
    }
}
