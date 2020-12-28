/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.api.v1;

import static org.geoserver.catalog.impl.ClassMappings.RESOURCE;
import static org.geoserver.catalog.impl.ClassMappings.STORE;
import static org.springframework.http.MediaType.APPLICATION_STREAM_JSON_VALUE;

import lombok.NonNull;
import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.ClassMappings;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.catalog.plugin.Query;
import org.geoserver.cloud.catalog.service.ProxyResolver;
import org.geoserver.cloud.catalog.service.ReactiveCatalog;
import org.opengis.filter.capability.FunctionName;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** */
@RestController
@RequestMapping(path = ReactiveCatalogController.BASE_URI)
public class ReactiveCatalogController {

    public static final String BASE_URI = "/api/v1/catalog";

    private ReactiveCatalog catalog;

    private ProxyResolver proxyResolver;

    public ReactiveCatalogController(ReactiveCatalog catalog, ProxyResolver proxyResolver) {
        this.catalog = catalog;
        this.proxyResolver = proxyResolver;
    }

    @PostMapping(path = "/{endpoint}")
    @ResponseStatus(HttpStatus.CREATED)
    public <C extends CatalogInfo> Mono<C> create(
            @PathVariable("endpoint") String endpoint, @RequestBody C info) {

        return catalog.create(Mono.just(info).flatMap(proxyResolver::resolve));
    }

    @PatchMapping(path = "/{endpoint}/{id}")
    public Mono<? extends CatalogInfo> update(
            @PathVariable("endpoint") String endpoint,
            @PathVariable("id") String id,
            @RequestBody Patch patch) {

        Mono<Patch> resolvedPatch = proxyResolver.resolve(patch);

        Class<? extends CatalogInfo> type = endpointToClass(endpoint);

        Mono<? extends CatalogInfo> object =
                catalog.getById(id, type)
                        .switchIfEmpty(
                                noContent(
                                        "%s with id '%s' does not exist",
                                        type.getSimpleName(), id));

        try {
            return object.flatMap(c -> catalog.update(c, resolvedPatch));
        } catch (RuntimeException e) {
            e.printStackTrace();
            throw e;
        }
    }

    @DeleteMapping(path = "/{endpoint}")
    public <C extends CatalogInfo> Mono<C> delete(
            @PathVariable("endpoint") String endpoint, @NonNull C value) {

        ClassMappings type = endpointToType(endpoint);
        return catalog.delete(value)
                .switchIfEmpty(
                        noContent(
                                "%s with id '%s' does not exist",
                                type.getInterface().getSimpleName(), value.getId()));
    }

    @DeleteMapping(path = "/{endpoint}/{id}")
    public Mono<? extends CatalogInfo> deleteById(
            @PathVariable("endpoint") String endpoint, @PathVariable("id") String id) {
        Class<? extends CatalogInfo> type = endpointToClass(endpoint, null);
        return catalog.getById(id, type)
                .flatMap(i -> catalog.delete(i))
                .switchIfEmpty(
                        noContent("%s with id '%s' does not exist", type.getSimpleName(), id));
    }

    @GetMapping(path = "/{endpoint}", produces = APPLICATION_STREAM_JSON_VALUE)
    public Flux<? extends CatalogInfo> findAll(
            @PathVariable("endpoint") String endpoint,
            @RequestParam(name = "type", required = false) ClassMappings subType) {

        Class<? extends CatalogInfo> type = endpointToClass(endpoint, subType);
        return catalog.getAll(type);
    }

    @GetMapping(path = {"/{endpoint}/{id}"})
    public Mono<? extends CatalogInfo> findById( //
            @PathVariable("endpoint") String endpoint,
            @PathVariable("id") String id,
            @RequestParam(name = "type", required = false) ClassMappings subType) {

        final @NonNull ClassMappings type = endpointToType(endpoint, subType);
        @SuppressWarnings("unchecked")
        Class<? extends CatalogInfo> targetType =
                (Class<? extends CatalogInfo>) type.getInterface();
        return catalog.getById(id, targetType)
                .switchIfEmpty(
                        noContent(
                                "%s with id '%s' does not exist",
                                type.getInterface().getSimpleName(), id));
    }

    @GetMapping(path = "/{endpoint}/name/{name}/first")
    public Mono<? extends CatalogInfo> findFirstByName( //
            @PathVariable("endpoint") String endpoint,
            @PathVariable(name = "name") String name,
            @RequestParam(name = "type", required = false) ClassMappings subType) {

        Class<? extends CatalogInfo> type = endpointToClass(endpoint, subType);
        return catalog.getFirstByName(name, type)
                .switchIfEmpty(
                        noContent("%s with name '%s' does not exist", type.getSimpleName(), name));
    }

    @GetMapping(path = "/{endpoint}/query/cansortby/{propertyName}")
    public Mono<Boolean> canSortBy(
            @PathVariable("endpoint") String endpoint,
            @PathVariable("propertyName") String propertyName) {

        Class<? extends CatalogInfo> type = endpointToClass(endpoint);
        return catalog.canSortBy(type, propertyName);
    }

    @GetMapping(path = "/query/capabilities/functions", produces = APPLICATION_STREAM_JSON_VALUE)
    public Flux<FunctionName> getSupportedFilterFunctionNames() {
        return catalog.getSupportedFunctionNames();
    }

    @PostMapping(path = "/{endpoint}/query", produces = APPLICATION_STREAM_JSON_VALUE)
    public <C extends CatalogInfo> Flux<C> query( //
            @PathVariable("endpoint") String endpoint, @RequestBody Query<C> query) {

        return catalog.query(query);
    }

    @PostMapping(path = "/{endpoint}/query/count")
    public <C extends CatalogInfo> Mono<Long> count(
            @PathVariable("endpoint") String endpoint, @RequestBody Query<C> query) {
        return catalog.count(query.getType(), query.getFilter());
    }

    @PutMapping(path = "/workspaces/default/{workspaceId}")
    public Mono<WorkspaceInfo> setDefaultWorkspace(
            @PathVariable("workspaceId") String workspaceId) {

        return catalog.getById(workspaceId, WorkspaceInfo.class)
                .switchIfEmpty(noContent("WorkspaceInfo with id '%s' does not exist", workspaceId))
                .flatMap(catalog::setDefaultWorkspace);
    }

    @DeleteMapping(path = "workspaces/default")
    public Mono<WorkspaceInfo> unsetDefaultWorkspace() {
        return catalog.unsetDefaultWorkspace();
    }

    @GetMapping(path = "/workspaces/default")
    public Mono<WorkspaceInfo> getDefaultWorkspace() {
        return catalog.getDefaultWorkspace().switchIfEmpty(noContent("No default workspace"));
    }

    @PutMapping(path = "namespaces/default/{namespaceId}")
    public Mono<NamespaceInfo> setDefaultNamespace(
            @PathVariable("namespaceId") String namespaceId) {

        return catalog.getById(namespaceId, NamespaceInfo.class)
                .switchIfEmpty(noContent("Namespace %s does not exist", namespaceId))
                .flatMap(catalog::setDefaultNamespace);
    }

    @DeleteMapping(path = "namespaces/default")
    public Mono<NamespaceInfo> unsetDefaultNamespace() {
        return catalog.unsetDefaultNamespace();
    }

    @GetMapping(path = "namespaces/default")
    public Mono<NamespaceInfo> getDefaultNamespace() {
        return catalog.getDefaultNamespace().switchIfEmpty(noContent("No default namespace"));
    }

    @GetMapping(path = "namespaces/uri")
    public Mono<NamespaceInfo> findOneNamespaceByURI(@RequestParam("uri") String uri) {
        return catalog.getOneNamespaceByURI(uri)
                .switchIfEmpty(noContent("No NamespaceInfo found for uri %s", uri));
    }

    @GetMapping(path = "namespaces/uri/all", produces = APPLICATION_STREAM_JSON_VALUE)
    public Flux<NamespaceInfo> findAllNamespacesByURI(@RequestParam("uri") String uri) {
        return catalog.getAllNamespacesByURI(uri);
    }

    @GetMapping(path = "/stores/defaults", produces = APPLICATION_STREAM_JSON_VALUE)
    public Flux<DataStoreInfo> getDefaultDataStores() {
        return catalog.getDefaultDataStores();
    }

    @PutMapping(path = "/workspaces/{workspaceId}/stores/default/{dataStoreId}")
    public Mono<DataStoreInfo> setDefaultDataStoreByWorkspaceId( //
            @PathVariable("workspaceId") String workspaceId,
            @PathVariable("dataStoreId") String dataStoreId) {

        Mono<WorkspaceInfo> ws =
                catalog.getById(workspaceId, WorkspaceInfo.class)
                        .switchIfEmpty(noContent("workspace not found"));
        Mono<DataStoreInfo> ds =
                catalog.getById(dataStoreId, DataStoreInfo.class)
                        .switchIfEmpty(noContent("data store not found"));

        return ws.zipWith(ds)
                .flatMap(tuple -> catalog.setDefaultDataStore(tuple.getT1(), tuple.getT2()));
    }

    @DeleteMapping(path = "/workspaces/{workspaceId}/stores/default")
    public Mono<DataStoreInfo> unsetDefaultDataStore(
            @PathVariable("workspaceId") String workspaceId) {

        return catalog.getById(workspaceId, WorkspaceInfo.class)
                .flatMap(w -> catalog.unsetDefaultDataStore(w))
                .switchIfEmpty(noContent("Workspace does not exist: %s", workspaceId));
    }

    @GetMapping(path = "/workspaces/{workspaceId}/stores/default")
    public Mono<DataStoreInfo> findDefaultDataStoreByWorkspaceId( //
            @PathVariable("workspaceId") String workspaceId) {

        return catalog.getById(workspaceId, WorkspaceInfo.class)
                .flatMap(catalog::getDefaultDataStore)
                .switchIfEmpty(noContent("Workspace not found: %s", workspaceId));
    }

    @GetMapping(path = "/workspaces/{workspaceId}/stores", produces = APPLICATION_STREAM_JSON_VALUE)
    public Flux<? extends StoreInfo> findStoresByWorkspaceId( //
            @PathVariable("workspaceId") String workspaceId,
            @RequestParam(name = "type", required = false) ClassMappings subType) {

        @SuppressWarnings("unchecked")
        final Class<? extends StoreInfo> type =
                (Class<? extends StoreInfo>) (subType == null ? STORE : subType).getInterface();

        return catalog.getById(workspaceId, WorkspaceInfo.class)
                .flatMapMany(w -> catalog.getStoresByWorkspace(w, type));
    }

    @GetMapping(path = "/workspaces/{workspaceId}/stores/name/{name}")
    public Mono<? extends StoreInfo> findStoreByWorkspaceIdAndName( //
            @PathVariable("workspaceId") String workspaceId,
            @PathVariable("name") String name,
            @RequestParam(name = "type", required = false) ClassMappings subType) {

        @SuppressWarnings("unchecked")
        final Class<? extends StoreInfo> type =
                (Class<? extends StoreInfo>) (subType == null ? STORE : subType).getInterface();
        return catalog.getById(workspaceId, WorkspaceInfo.class)
                .flatMap(w -> catalog.getStoreByName(w, name, type))
                .switchIfEmpty(noContent("Workspace does not exist: %s", workspaceId));
    }

    @GetMapping(path = "/namespaces/{namespaceId}/resources/name/{name}")
    public Mono<? extends ResourceInfo> findResourceByNamespaceIdAndName(
            @PathVariable("namespaceId") String namespaceId,
            @PathVariable("name") String name,
            @RequestParam(name = "type", required = false) ClassMappings subType) {

        @SuppressWarnings("unchecked")
        final Class<? extends ResourceInfo> type =
                (Class<? extends ResourceInfo>)
                        (subType == null ? RESOURCE : subType).getInterface();

        return catalog.getById(namespaceId, NamespaceInfo.class)
                .flatMap(n -> catalog.getResourceByName(n, name, type))
                .switchIfEmpty(noContent("Namesapce does not exist: %s", namespaceId));
    }

    @GetMapping(path = "/layers/style/{styleId}", produces = APPLICATION_STREAM_JSON_VALUE)
    public Flux<LayerInfo> findLayersWithStyle(@PathVariable("styleId") String styleId) {
        return catalog.getById(styleId, StyleInfo.class)
                .switchIfEmpty(noContent("Style does not exist: %s", styleId))
                .flatMapMany(s -> catalog.getLayersWithStyle(s));
    }

    @GetMapping(path = "/layers/resource/{resourceId}", produces = APPLICATION_STREAM_JSON_VALUE)
    public Flux<LayerInfo> findLayersByResourceId(@PathVariable("resourceId") String resourceId) {

        return catalog.getById(resourceId, ResourceInfo.class)
                .switchIfEmpty(noContent("ResourceInfo does not exist: %s", resourceId))
                .flatMapMany(r -> catalog.getLayersByResource(r));
    }

    @GetMapping(path = "/layergroups/noworkspace", produces = APPLICATION_STREAM_JSON_VALUE)
    public Flux<LayerGroupInfo> findLayerGroupsByNullWoskspace() {
        return catalog.getLayerGroupsWithNoWoskspace();
    }

    @GetMapping(
        path = "/workspaces/{workspaceId}/layergroups",
        produces = APPLICATION_STREAM_JSON_VALUE
    )
    public Flux<LayerGroupInfo> findLayerGroupsByWoskspaceId(
            @PathVariable("workspaceId") String workspaceId) {

        return catalog.getById(workspaceId, WorkspaceInfo.class)
                .switchIfEmpty(noContent("Workspace does not exist: %s", workspaceId))
                .flatMapMany(catalog::getLayerGroupsByWoskspace);
    }

    @GetMapping(path = "/layergroups/noworkspace/{name}")
    public Mono<LayerGroupInfo> findLayerGroupByNameAndNullWorkspace(
            @PathVariable("name") String name) {

        // gotta use NO_WORKSPACE to bypass all the magic in catalog, better to have well defined
        // contracts
        return catalog.getLayerGroupByName(CatalogFacade.NO_WORKSPACE, name)
                .switchIfEmpty(noContent("LayerGroup named '%s' does not exist", name));
    }

    @GetMapping(path = "/workspaces/{workspaceId}/layergroups/{name}")
    public Mono<LayerGroupInfo> findLayerGroupByWorkspaceIdAndName(
            @PathVariable(required = false, name = "workspaceId") String workspaceId,
            @PathVariable("name") String name) {

        return catalog.getById(workspaceId, WorkspaceInfo.class)
                .flatMap(w -> catalog.getLayerGroupByName(w, name))
                .switchIfEmpty(noContent("Workspace does not exist: %s", workspaceId));
    }

    @GetMapping(path = "/styles/noworkspace", produces = APPLICATION_STREAM_JSON_VALUE)
    public Flux<StyleInfo> findStylesByNullWorkspace() {
        return catalog.getStylesWithNoWorkspace();
    }

    @GetMapping(path = "/workspaces/{workspaceId}/styles", produces = APPLICATION_STREAM_JSON_VALUE)
    public Flux<StyleInfo> findStylesByWorkspaceId(
            @PathVariable(name = "workspaceId") String workspaceId) {

        return catalog.getById(workspaceId, WorkspaceInfo.class)
                .switchIfEmpty(noContent("Workspace does not exist: %s", workspaceId))
                .flatMapMany(catalog::getStylesByWorkspace);
    }

    @GetMapping(path = "/workspaces/{workspaceId}/styles/{name}")
    public Mono<StyleInfo> findStyleByWorkspaceIdAndName(
            @PathVariable(name = "workspaceId") String workspaceId,
            @PathVariable("name") String name) {

        return catalog.getById(workspaceId, WorkspaceInfo.class)
                .flatMap(w -> catalog.getStyleByName(w, name))
                .switchIfEmpty(noContent("Workspace does not exist: %s", workspaceId));
    }

    @GetMapping(path = "/styles/noworkspace/{name}")
    public Mono<StyleInfo> findStyleByNameAndNullWorkspace(@PathVariable("name") String name) {

        return catalog.getStyleByName(name)
                .switchIfEmpty(noContent("Style named '%s' does not exist", name));
    }

    @SuppressWarnings("unchecked")
    private @NonNull Class<? extends CatalogInfo> endpointToClass(@NonNull String endpoint) {
        return (@NonNull Class<? extends CatalogInfo>) endpointToType(endpoint).getInterface();
    }

    private @NonNull ClassMappings endpointToType(@NonNull String endpoint) {
        // e.g. "workspaces" -> "WORKSPACE"
        String enumKey = endpoint.toUpperCase().substring(0, endpoint.length() - 1);
        ClassMappings type = ClassMappings.valueOf(enumKey);
        if (type == null) {
            throw new IllegalArgumentException("Invalid end point: " + endpoint);
        }
        return type;
    }

    @SuppressWarnings("unchecked")
    private @NonNull Class<? extends CatalogInfo> endpointToClass(
            @NonNull String endpoint, ClassMappings subType) {
        return (@NonNull Class<? extends CatalogInfo>)
                endpointToType(endpoint, subType).getInterface();
    }

    private @NonNull ClassMappings endpointToType(@NonNull String endpoint, ClassMappings subType) {
        ClassMappings type = endpointToType(endpoint);
        if (subType != null) {
            if (!type.getInterface().isAssignableFrom(subType.getInterface())) {
                throw new IllegalArgumentException(
                        String.format("%s is not a subtype of %s", subType, type));
            }
            return subType;
        }
        return type;
    }

    protected <T> Mono<T> error(HttpStatus status, String messageFormat, Object... messageArgs) {
        return Mono.error(
                () ->
                        new ResponseStatusException(
                                status, String.format(messageFormat, messageArgs)));
    }

    /**
     * We use response code 204 (No Content) to mean something was not found, to differentiate from
     * the actual meaning of 404 - Not found, that the url itself is not found.
     */
    protected <T> Mono<T> noContent(String messageFormat, Object... messageArgs) {
        // revisit whether and now to return a reason message as header for debugging purposes
        // ex.getResponseHeaders().add("x-debug-reason", reason);
        return Mono.defer(
                () -> Mono.error(() -> new ResponseStatusException(HttpStatus.NO_CONTENT)));
    }
}
