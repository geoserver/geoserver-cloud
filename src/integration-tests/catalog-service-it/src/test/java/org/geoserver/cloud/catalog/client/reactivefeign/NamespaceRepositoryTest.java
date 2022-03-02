/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.reactivefeign;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import lombok.Getter;
import lombok.experimental.Accessors;

import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.plugin.CatalogInfoRepository.NamespaceRepository;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@EnableAutoConfiguration
@Accessors(fluent = true)
public class NamespaceRepositoryTest
        extends AbstractCatalogServiceClientRepositoryTest<NamespaceInfo, NamespaceRepository> {

    private @Autowired @Getter NamespaceRepository repository;

    public NamespaceRepositoryTest() {
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
        super.testFindAllIncludeFilter(
                NamespaceInfo.class, testData.namespaceA, testData.namespaceB, testData.namespaceC);
    }

    public @Override @Test void testFindById() {
        super.testFindById(testData.namespaceA);
        super.testFindById(testData.namespaceB);
        super.testFindById(testData.namespaceC);
    }

    public @Override @Test void testQueryFilter() {
        NamespaceInfo ns1 = serverCatalog.getNamespace(testData.namespaceA.getId());
        NamespaceInfo ns2 = serverCatalog.getNamespace(testData.namespaceB.getId());
        NamespaceInfo ns3 = serverCatalog.getNamespace(testData.namespaceC.getId());

        super.testQueryFilter(String.format("URI = '%s'", ns2.getURI()), ns2);

        ns3.setIsolated(true);
        ns3.setURI(ns1.getURI());
        serverCatalog.save(ns3);

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
        assertEquals(
                expected,
                repository.findFirstByName(expected.getPrefix(), NamespaceInfo.class).get());
    }

    public @Test void testNamespaceInfo_CRUD() throws IOException {
        NamespaceInfo ns = testData.createNamespace("namedpaceCRUD", "http://namespace.crud.test");
        crudTest(
                ns,
                serverCatalog::getNamespace,
                n -> n.setPrefix("modified-prefix"),
                (old, updated) -> assertEquals("modified-prefix", updated.getPrefix()));
    }

    public @Test void testGetDefaultNamespace() {
        assertEquals(testData.namespaceA, repository.getDefaultNamespace().get());
        serverCatalog.setDefaultNamespace(testData.namespaceB);
        assertEquals(testData.namespaceB, repository.getDefaultNamespace().get());
    }

    public @Test void testGetDefaultNamespaceNoDefaultExists() {
        testData.deleteAll();
        assertNull(serverCatalog.getDefaultNamespace());
        assertTrue(repository.getDefaultNamespace().isEmpty());
    }

    public @Test void testSetDefaultNamespace() {
        assertEquals(testData.namespaceA, serverCatalog.getDefaultNamespace());

        repository.setDefaultNamespace(testData.namespaceB);
        NamespaceInfo returned = repository.getDefaultNamespace().get();
        assertEquals(testData.namespaceB, returned);
        assertEquals(testData.namespaceB, serverCatalog.getDefaultNamespace());
    }

    public @Test void testSetDefaultNamespaceNonExistent() {
        assertEquals(testData.namespaceA, serverCatalog.getDefaultNamespace());

        NamespaceInfo nonExistent = testData.createNamespace("invalid", "nonExistentURI");
        repository.setDefaultNamespace(nonExistent);
        assertEquals(testData.namespaceA, repository.getDefaultNamespace().get());
        assertEquals(testData.namespaceA, serverCatalog.getDefaultNamespace());
    }

    public @Test void testUnsetDefaultNamespace() {
        NamespaceInfo ns = testData.namespaceA;
        // preflight check
        serverCatalog.setDefaultNamespace(null);
        assertNull(serverCatalog.getDefaultNamespace());
        serverCatalog.setDefaultNamespace(ns);
        assertEquals(ns, serverCatalog.getDefaultNamespace());

        NamespaceInfo current = serverCatalog.getDefaultNamespace();
        assertNotNull(current);
        assertEquals(ns.getId(), current.getId());

        repository.unsetDefaultNamespace();

        assertNull(serverCatalog.getDefaultNamespace());
        assertTrue(repository.getDefaultNamespace().isEmpty());

        // check idempotency
        repository.unsetDefaultNamespace();

        assertNull(serverCatalog.getDefaultNamespace());
        assertTrue(repository.getDefaultNamespace().isEmpty());
    }

    public @Test void testFindOneNamespaceByURI() {
        NamespaceInfo ns1 = testData.namespaceA;
        NamespaceInfo ns2 =
                testData.createNamespace("second-ns-with-duplicate-uri", "prefix2", ns1.getURI());
        ns2.setIsolated(true);
        serverCatalog.add(ns2);

        NamespaceInfo found = repository.findOneByURI(ns1.getURI()).get();
        // serverCatalog.getNamespaceByURI() contract says it returns the first found with that
        // uri...
        assertTrue(found.getId().equals(ns1.getId()) || found.getId().equals(ns2.getId()));
    }

    public @Test void testFindAllNamespacesByURI() {
        NamespaceInfo ns1 = testData.namespaceA;
        NamespaceInfo ns2 =
                testData.createNamespace("second-ns-with-duplicate-uri", "prefix2", ns1.getURI());
        ns2.setIsolated(true);
        serverCatalog.add(ns2);

        List<NamespaceInfo> found =
                repository.findAllByURI(ns1.getURI()).collect(Collectors.toList());
        assertTrue(found.contains(ns1));
        assertTrue(found.contains(ns2));
        assertEquals(2, found.size());
    }

    public @Test void testCreateNamespaceDuplicateURI() {
        NamespaceInfo ns1 = testData.namespaceA;
        NamespaceInfo ns2 =
                testData.createNamespace("second-ns-with-duplicate-uri", "prefix2", ns1.getURI());
        try {
            repository.add(ns2);
            fail("expected exception");
        } catch (RuntimeException expected) {
            expected.printStackTrace();
        }

        ns2.setIsolated(true);
        repository.add(ns2);
        assertEquals(ns2, repository.findById(ns2.getId(), NamespaceInfo.class).get());
    }
}
