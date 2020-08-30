/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.api.v1;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.NonNull;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.impl.ClassMappings;
import org.geoserver.cloud.catalog.app.CatalogServiceApplicationConfiguration;
import org.geoserver.cloud.catalog.test.CatalogTestClient;
import org.geoserver.cloud.catalog.test.CatalogTestData;
import org.geoserver.cloud.catalog.test.WebTestClientSupport;
import org.geoserver.cloud.catalog.test.WebTestClientSupportConfiguration;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(
    classes = {
        CatalogServiceApplicationConfiguration.class,
        WebTestClientSupportConfiguration.class
    }
)
@ActiveProfiles("test") // see bootstrap-test.yml
@RunWith(SpringRunner.class)
public abstract class AbstractCatalogInfoControllerTest<C extends CatalogInfo> {

    protected @Autowired WebTestClientSupport clientSupport;

    protected @Autowired @Qualifier("catalog") Catalog catalog;

    public @Rule CatalogTestData testData = CatalogTestData.initialized(() -> catalog);

    protected final String baseUri;

    protected final @NonNull Class<C> infoType;

    protected AbstractCatalogInfoControllerTest(
            @NonNull String baseUri, @NonNull Class<C> infoType) {
        this.infoType = infoType;
        this.baseUri = baseUri;
    }

    protected final void assertCatalogInfoEquals(C expected, C actual) {
        assertEquals(expected.getId(), actual.getId());
        assertPropertriesEqual(expected, actual);
    }

    /**
     * Subclasses should override to provide {@link CatalogInfo} subtype specific assertions. Not
     * doing {@code assertEquals(expected, actual)} because the returned object may differ from the
     * submitted one as the catalog populated default properties
     */
    protected abstract void assertPropertriesEqual(C expected, C actual);

    protected CatalogTestClient<C> client() {
        return this.clientSupport.clientFor(infoType, baseUri);
    }

    protected WebTestClient webtTestClient() {
        return this.clientSupport.get();
    }

    public @Test void findByIdNotFound() throws IOException {
        client().findById("non-existent-ws-id", infoType)
                .expectStatus()
                .isNotFound()
                .expectBody()
                .isEmpty();
    }

    protected void testFindById(C expected) {
        C responseBody =
                client().findById(expected)
                        .expectStatus()
                        .isOk()
                        .expectBody(infoType)
                        .returnResult()
                        .getResponseBody();
        assertCatalogInfoEquals(expected, responseBody);
    }

    public void crudTest(
            final C toCreate, Consumer<C> modifyingConsumer, Function<String, C> catalogLookup) {

        CatalogTestClient<C> testClient = this.client();
        assertNotNull(
                "Argument object expected to have pre-assigned id to ease lookup and comparison",
                toCreate.getId());
        assertNull(
                "Object to be created shall not already exist in catalog",
                catalogLookup.apply(toCreate.getId()));

        final Class<? extends C> expectedType =
                ClassMappings.fromImpl(toCreate.getClass()).getInterface();

        C created =
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
        assertCatalogInfoEquals(toCreate, created);

        testClient
                .findById(created)
                .expectStatus()
                .isOk()
                .expectBody(expectedType)
                .consumeWith(Assert::assertNotNull);

        // try to create it again?
        /// testClient.create(toCreate).expectStatus().is;

        modifyingConsumer.accept(created);
        final C updated =
                testClient
                        .update(created)
                        .expectStatus()
                        .isOk()
                        .expectBody(expectedType)
                        .returnResult()
                        .getResponseBody();
        assertNotSame(created, updated);
        assertCatalogInfoEquals(created, updated);
        assertNotEquals(toCreate, updated);

        C foundAfterUpdate =
                testClient
                        .findById(updated)
                        .expectStatus()
                        .isOk()
                        .expectBody(expectedType)
                        .returnResult()
                        .getResponseBody();
        assertCatalogInfoEquals(updated, foundAfterUpdate);

        C deleted =
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
