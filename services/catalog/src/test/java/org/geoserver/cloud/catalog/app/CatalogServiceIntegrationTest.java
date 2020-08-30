/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.app;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Function;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.ClassMappings;
import org.geoserver.cloud.catalog.test.CatalogTestClient;
import org.geoserver.cloud.catalog.test.CatalogTestData;
import org.geoserver.cloud.catalog.test.WebTestClientSupport;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerFacade;
import org.geoserver.config.GeoServerLoader;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.web.reactive.server.WebTestClient;

public abstract class CatalogServiceIntegrationTest {

    protected @Autowired WebTestClientSupport clientSupport;

    protected @Autowired @Qualifier("catalog") Catalog catalog;
    protected @Autowired CatalogFacade rawCatalogFacade;

    protected @Autowired GeoServer geoServer;
    protected @Autowired GeoServerFacade geoServerFacadeImpl;

    protected @Autowired GeoServerLoader geoServerLoader;
    public @Rule CatalogTestData testData = CatalogTestData.empty(() -> catalog);

    protected WebTestClient client() {
        return clientSupport.get();
    }

    public @Test void workspaceCRUD() throws IOException {
        WorkspaceInfo ws = testData.createWorkspace("ws_CRUD_Test");
        crudTest(
                ws,
                clientSupport.workspaces(),
                w -> w.setName("workspaceCRUD_modified"),
                catalog::getWorkspace);
    }

    public @Test void namespaceCRUD() throws IOException {
        NamespaceInfo ns = testData.createNamespace("namespaceCRUD", "http://namespace.crud.test");
        crudTest(
                ns,
                clientSupport.namespaces(),
                n -> n.setURI("http://ns.crud.updated"),
                catalog::getNamespace);
    }

    public <T extends CatalogInfo> void crudTest(
            final T toCreate,
            CatalogTestClient<T> testClient,
            Consumer<T> modifyingConsumer,
            Function<String, T> catalogLookup)
            throws IOException {

        assertNotNull(
                "Argument object expected to have pre-assigned id to ease lookup and comparison",
                toCreate.getId());
        assertNull(
                "Object to be created shall not already exist in catalog",
                catalogLookup.apply(toCreate.getId()));

        final Class<? extends T> expectedType =
                ClassMappings.fromImpl(toCreate.getClass()).getInterface();

        T created =
                testClient
                        .create(toCreate)
                        .expectStatus()
                        .isCreated()
                        .expectBody(expectedType)
                        .returnResult()
                        .getResponseBody();
        assertNotNull(
                "created object not found in service backend catalog",
                catalogLookup.apply(created.getId()));

        assertNotSame(toCreate, created);
        assertEquals(toCreate.getId(), created.getId());

        testClient
                .findById(created)
                .expectStatus()
                .isOk()
                .expectBody(expectedType)
                .consumeWith(Assert::assertNotNull);

        // try to create it again?
        /// testClient.create(toCreate).expectStatus().is;

        modifyingConsumer.accept(created);
        final T updated =
                testClient
                        .update(created)
                        .expectStatus()
                        .isOk()
                        .expectBody(expectedType)
                        .returnResult()
                        .getResponseBody();
        assertNotSame(created, updated);
        assertEquals(created, updated);
        assertNotEquals(toCreate, updated);

        T foundAfterUpdate =
                testClient
                        .findById(updated)
                        .expectStatus()
                        .isOk()
                        .expectBody(expectedType)
                        .returnResult()
                        .getResponseBody();
        assertEquals(updated, foundAfterUpdate);

        T deleted =
                testClient
                        .delete(updated)
                        .expectStatus()
                        .isOk()
                        .expectBody(expectedType)
                        .returnResult()
                        .getResponseBody();
        assertNull("object not deleted from backend catalog", catalogLookup.apply(created.getId()));

        testClient.findById(deleted).expectStatus().isNotFound();
    }
}
