/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.api.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import org.geoserver.catalog.NamespaceInfo;
import org.junit.Test;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;

@AutoConfigureWebTestClient(timeout = "360000")
public class NamespaceControllerTest extends AbstractCatalogInfoControllerTest<NamespaceInfo> {

    public NamespaceControllerTest() {
        super(NamespaceController.BASE_URI, NamespaceInfo.class);
    }

    protected @Override void assertPropertriesEqual(NamespaceInfo expected, NamespaceInfo actual) {
        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getPrefix(), actual.getPrefix());
        assertEquals(expected.getURI(), actual.getURI());
    }

    public @Test void findNamespaceById() {
        testFindById(testData.namespaceA);
        testFindById(testData.namespaceB);
        testFindById(testData.namespaceC);
    }

    public @Test void namespaceCRUD() throws IOException {
        NamespaceInfo ns = testData.createNamespace("namedpaceCRUD", "http://namespace.crud.test");
        crudTest(ns, n -> n.setPrefix("modified-prefix"), catalog::getNamespace);
    }
}
