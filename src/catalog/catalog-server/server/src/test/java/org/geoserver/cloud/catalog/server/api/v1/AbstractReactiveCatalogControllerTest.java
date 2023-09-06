/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.server.api.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;

import lombok.NonNull;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.CatalogTestData;
import org.geoserver.catalog.impl.ClassMappings;
import org.geoserver.catalog.plugin.Query;
import org.geoserver.catalog.plugin.resolving.ProxyUtils;
import org.geoserver.cloud.catalog.server.test.CatalogTestClient;
import org.geoserver.cloud.catalog.server.test.TestConfiguration;
import org.geoserver.cloud.catalog.server.test.WebTestClientSupport;
import org.geoserver.cloud.catalog.server.test.WebTestClientSupportConfiguration;
import org.geoserver.config.GeoServer;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opengis.filter.Filter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

@SpringBootTest(classes = {TestConfiguration.class, WebTestClientSupportConfiguration.class})
@ActiveProfiles("test") // see bootstrap-test.yml
public abstract class AbstractReactiveCatalogControllerTest<C extends CatalogInfo> {

    protected @Autowired WebTestClientSupport clientSupport;

    protected @Autowired @Qualifier("catalog") Catalog catalog;
    protected @Autowired @Qualifier("geoServer") GeoServer geoServer;

    protected CatalogTestData testData;

    protected final @NonNull Class<C> infoType;

    protected ProxyUtils proxyResolver;

    protected AbstractReactiveCatalogControllerTest(@NonNull Class<C> infoType) {
        this.infoType = infoType;
    }

    public @BeforeEach void setup() {
        proxyResolver =
                new ProxyUtils(catalog, Optional.of(geoServer)).failOnMissingReference(true);
        testData =
                CatalogTestData.initialized(() -> catalog, () -> null)
                        .initConfig(false)
                        .initialize();
    }

    public @AfterEach void after() {
        if (null != testData) testData.after();
    }

    public abstract @Test void testFindAll();

    public abstract @Test void testFindAllByType();

    public abstract @Test void testFindById();

    public abstract @Test void testQueryFilter();

    protected void testFindAll(@SuppressWarnings("unchecked") C... expected) {
        Set<String> expectedIds =
                Arrays.stream(expected).map(CatalogInfo::getId).collect(Collectors.toSet());
        Set<String> actual =
                this.findAll().stream().map(CatalogInfo::getId).collect(Collectors.toSet());
        assertEquals(expectedIds, actual);
    }

    protected <S extends C> void testFindAll(
            Class<S> type, @SuppressWarnings("unchecked") S... expected) {
        Set<String> expectedIds =
                Arrays.stream(expected).map(CatalogInfo::getId).collect(Collectors.toSet());
        ClassMappings classMappings = ClassMappings.fromInterface(type);
        Set<String> actual =
                this.findAll(classMappings).stream()
                        .map(CatalogInfo::getId)
                        .collect(Collectors.toSet());
        assertEquals(expectedIds, actual);
    }

    protected <S extends C> void testQueryFilter(
            String ecqlFilter, @SuppressWarnings("unchecked") S... expected) {
        testQueryFilter(this.infoType, ecqlFilter, expected);
    }

    protected <S extends C> void testQueryFilter(
            Class<S> type, String ecqlFilter, @SuppressWarnings("unchecked") S... expected) {
        Filter filter;
        try {
            filter = ECQL.toFilter(ecqlFilter);
        } catch (CQLException e) {
            throw new RuntimeException(e);
        }
        this.testQueryFilter(type, filter, expected);
    }

    protected <S extends C> void testQueryFilter(
            @NonNull Class<S> type, Filter filter, @SuppressWarnings("unchecked") S... expected) {
        String endpoint = endpoint();

        Query<S> query = Query.valueOf(type, filter);

        client().doPost(query, "/{endpoint}/query", endpoint)
                .expectStatus()
                .isOk()
                .expectHeader()
                .contentType(MediaType.APPLICATION_STREAM_JSON)
                .expectBodyList(type)
                .consumeWith(
                        response -> {
                            List<S> responseBody = response.getResponseBody();

                            Set<String> expectedIds =
                                    Arrays.stream(expected)
                                            .map(CatalogInfo::getId)
                                            .collect(Collectors.toSet());
                            Set<String> returnedIds =
                                    responseBody.stream()
                                            .map(CatalogInfo::getId)
                                            .collect(Collectors.toSet());
                            assertEquals(expectedIds, returnedIds);
                        });
    }

    protected void testFindById(C expected) {
        C responseBody =
                client().findById(expected)
                        .expectStatus()
                        .isOk()
                        .expectBody(infoType)
                        .returnResult()
                        .getResponseBody();
        C resolved = resolveProxies(responseBody);
        assertCatalogInfoEquals(expected, resolved);
    }

    protected <I extends CatalogInfo> I resolveProxies(I info) {
        return proxyResolver.resolve(info);
    }

    protected final void assertCatalogInfoEquals(C expected, C actual) {
        assertPropertriesEqual(expected, actual);
    }

    /**
     * Subclasses should override to provide {@link CatalogInfo} subtype specific assertions. Not
     * doing {@code assertEquals(expected, actual)} because the returned object may differ from the
     * submitted one as the catalog populated default properties
     */
    protected abstract void assertPropertriesEqual(C expected, C actual);

    protected CatalogTestClient<C> client() {
        return this.clientSupport.clientFor(infoType);
    }

    protected WebTestClient webtTestClient() {
        return this.clientSupport.get();
    }

    public @Test void testFindByIdNotFound() throws IOException {
        client().findById("non-existent-ws-id", infoType).expectStatus().isNoContent();
    }

    protected String endpoint() {
        return toEndpoint(ClassMappings.fromInterface(this.infoType));
    }

    protected String toEndpoint(ClassMappings cm) {
        return cm.name().toLowerCase() + "s";
    }

    protected List<C> findAll() {
        return findAll(null);
    }

    protected List<C> findAll(@Nullable ClassMappings subType) {
        String endpoint = endpoint();
        return client().getRelative("/{endpoint}?type={type}", endpoint, subType)
                .expectStatus()
                .isOk()
                .expectHeader()
                .contentType(MediaType.APPLICATION_STREAM_JSON)
                .expectBodyList(infoType)
                .returnResult()
                .getResponseBody();
    }

    protected void crudTest(
            final C toCreate,
            Function<String, C> catalogLookup,
            Consumer<C> modifyingConsumer,
            BiConsumer<C, C> updateVerifier) {

        C created = testCreate(toCreate, catalogLookup);

        C updated = testUpdate(created, modifyingConsumer, updateVerifier);

        testDelete(updated, catalogLookup);
    }

    protected C testUpdate(
            C created, Consumer<C> modifyingConsumer, BiConsumer<C, C> updateVerifier) {
        final Class<? extends C> expectedType = expectedType(created);
        C updated =
                client().update(created, modifyingConsumer)
                        .expectStatus()
                        .isOk()
                        .expectBody(expectedType)
                        .returnResult()
                        .getResponseBody();
        assertNotSame(created, updated);
        updated = resolveProxies(updated);
        updateVerifier.accept(created, updated);

        C foundAfterUpdate =
                client().findById(updated)
                        .expectStatus()
                        .isOk()
                        .expectBody(expectedType)
                        .returnResult()
                        .getResponseBody();
        foundAfterUpdate = resolveProxies(foundAfterUpdate);
        updateVerifier.accept(created, foundAfterUpdate);

        return updated;
    }

    protected C testCreate(C toCreate, Function<String, C> catalogLookup) {
        final @Nullable String providedId = toCreate.getId();

        CatalogTestClient<C> testClient = this.client();

        assertNull(
                catalogLookup.apply(toCreate.getId()),
                "Object to be created shall not already exist in catalog");

        final Class<? extends C> expectedType = expectedType(toCreate);

        C created =
                testClient
                        .create(toCreate)
                        .expectStatus()
                        .isCreated()
                        .expectBody(expectedType)
                        .returnResult()
                        .getResponseBody();
        created = resolveProxies(created);
        assertNotNull(
                catalogLookup.apply(created.getId()),
                "created object not found in service backend catalog");

        assertNotSame(toCreate, created);
        if (providedId == null) assertNotNull(created.getId());
        else assertEquals(providedId, created.getId());

        assertCatalogInfoEquals(toCreate, created);

        testClient
                .findById(created)
                .expectStatus()
                .isOk()
                .expectBody(expectedType)
                .consumeWith(Assertions::assertNotNull);

        // try to create it again?
        /// testClient.create(toCreate).expectStatus().is;
        return created;
    }

    protected C testDelete(final C toDelete, Function<String, C> catalogLookup) {
        final Class<? extends C> expectedType = expectedType(toDelete);

        client().findById(toDelete).expectStatus().isOk();

        C deleted =
                client().delete(toDelete)
                        .expectStatus()
                        .isOk()
                        .expectBody(expectedType)
                        .returnResult()
                        .getResponseBody();
        assertNull(
                catalogLookup.apply(toDelete.getId()), "object not deleted from backend catalog");

        client().findById(deleted).expectStatus().isNoContent();

        client().delete(toDelete).expectStatus().isNoContent();

        return deleted;
    }

    private Class<? extends C> expectedType(C info) {
        @SuppressWarnings("unchecked")
        final Class<? extends C> expectedType =
                (Class<? extends C>) ClassMappings.fromImpl(info.getClass()).getInterface();
        return expectedType;
    }
}
