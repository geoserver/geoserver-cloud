/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.server.test;

import static org.springframework.http.MediaType.APPLICATION_JSON;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.impl.ClassMappings;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.catalog.plugin.PropertyDiff;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient.RequestBodySpec;
import org.springframework.test.web.reactive.server.WebTestClient.ResponseSpec;

import java.lang.reflect.Proxy;
import java.util.function.Consumer;

import javax.annotation.Nullable;

@RequiredArgsConstructor
public class CatalogTestClient<C extends CatalogInfo> {

    private final @NonNull WebTestClient client;

    protected final @NonNull Class<C> infoType;

    protected final @NonNull String baseUri;

    public ResponseSpec create(C info) {
        return doPost(info, "/{endpoint}", endpoint());
    }

    /** Calls update with {@link Patch} on the server, does not modify {@code info} */
    public ResponseSpec update(C info, Consumer<C> modifyingConsumer) {
        C real = ModificationProxy.unwrap(info);
        Class<? extends CatalogInfo> clazz = real.getClass();
        ClassMappings classMappings = ClassMappings.fromImpl(clazz);
        @SuppressWarnings("unchecked")
        C proxied = (C) ModificationProxy.create(info, classMappings.getInterface());

        modifyingConsumer.accept(proxied);
        ModificationProxy proxy = (ModificationProxy) Proxy.getInvocationHandler(proxied);
        Patch patch = PropertyDiff.valueOf(proxy).toPatch();

        return patchAbsoluteURI(patch, baseUri + "/{endpoint}/{id}", endpoint(), info.getId());
    }

    public ResponseSpec delete(C info) {
        return doDelete(info, baseUri + "/{endpoint}/{id}", endpoint(), info.getId());
    }

    public ResponseSpec findById(@NonNull C expected) {
        @SuppressWarnings("unchecked")
        Class<? extends C> type =
                (Class<? extends C>) ClassMappings.fromImpl(expected.getClass()).getInterface();
        return findById(expected.getId(), type);
    }

    public ResponseSpec findById(String id) {
        return findById(id, infoType);
    }

    /**
     * Calls {@link #findById(String, ClassMappings)} on the controller under test and for the
     * requested subtype, if provided.
     */
    public ResponseSpec findById(String id, @NonNull Class<? extends C> requestedType) {
        String endpoint = endpoint();
        String uri = baseUri + "/{endpoint}/{id}?type={subtype}";
        ClassMappings subType = null;
        if (!infoType.equals(requestedType)) {
            subType = ClassMappings.fromInterface(requestedType);
        }
        return getWithAbsolutePath(uri, endpoint, id, subType);
    }

    private String endpoint() {
        String endpoint = ClassMappings.fromInterface(infoType).toString().toLowerCase() + "s";
        return endpoint;
    }

    public C getFirstByName(String name) {
        return findFirstByName(name, infoType)
                .expectStatus()
                .isOk()
                .expectBody(infoType)
                .returnResult()
                .getResponseBody();
    }

    public ResponseSpec findFirstByName( //
            String localName, //
            @NonNull Class<? extends C> requestedType) {

        String uri = baseUri + "/{endpoint}/name/{name}/first?type={type}";
        ClassMappings subType = ClassMappings.fromInterface(requestedType);
        return getWithAbsolutePath(uri, endpoint(), localName, subType);
    }

    public <T extends CatalogInfo> ResponseSpec getRelative(String uri, Object... uriVariables) {
        uri = baseUri + uri;
        return client.get()
                .uri(uri, uriVariables)
                .accept(MediaType.APPLICATION_JSON, MediaType.APPLICATION_STREAM_JSON)
                .exchange();
    }

    public <T extends CatalogInfo> ResponseSpec getWithAbsolutePath(
            String uri, Object... uriVariables) {
        return client.get().uri(uri, uriVariables).exchange();
    }

    public ResponseSpec doPost(
            @NonNull Object requestBody, //
            @NonNull String uri,
            Object... uriVariables) {

        uri = baseUri + uri;
        return client.post()
                .uri(uri, uriVariables)
                .contentType(APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange();
    }

    public ResponseSpec put(@NonNull String uri, Object... uriVariables) {
        return putAbsoluteURI(null, baseUri + uri, uriVariables);
    }

    public ResponseSpec putWithBody(
            @NonNull Object requestBody, @NonNull String uri, Object... uriVariables) {
        return putAbsoluteURI(requestBody, baseUri + uri, uriVariables);
    }

    public ResponseSpec patchWithBody(
            @NonNull Object requestBody, @NonNull String uri, Object... uriVariables) {
        return patchAbsoluteURI(requestBody, baseUri + uri, uriVariables);
    }

    public ResponseSpec putAbsoluteURI(
            @Nullable Object requestBody, //
            @NonNull String uri,
            Object... uriVariables) {

        RequestBodySpec bodySpec =
                client.put().uri(uri, uriVariables).contentType(APPLICATION_JSON);
        if (requestBody != null) return bodySpec.bodyValue(requestBody).exchange();

        return bodySpec.exchange();
    }

    public ResponseSpec patchAbsoluteURI(
            @Nullable Object requestBody, //
            @NonNull String uri,
            Object... uriVariables) {

        RequestBodySpec bodySpec =
                client.patch().uri(uri, uriVariables).contentType(APPLICATION_JSON);
        if (requestBody != null) return bodySpec.bodyValue(requestBody).exchange();

        return bodySpec.exchange();
    }

    public ResponseSpec doDelete(
            @NonNull Object requestBody, //
            @NonNull String uri,
            Object... uriVariables) {

        return client.delete().uri(uri, uriVariables).accept(APPLICATION_JSON).exchange();
    }
}
