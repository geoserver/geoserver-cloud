/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.api.v1;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

import javax.annotation.Nullable;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.impl.ClassMappings;
import org.geoserver.cloud.catalog.service.ReactiveCatalogService;
import org.geotools.feature.NameImpl;
import org.opengis.feature.type.Name;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

public abstract class AbstractCatalogInfoController<T extends CatalogInfo> {

    public static final String BASE_API_URI = "/api/v1/catalog";

    private @Autowired ReactiveCatalogService service;

    protected abstract Class<T> getInfoType();

    @SuppressWarnings("unchecked")
    protected <S extends T> Class<S> getInfoType(@Nullable ClassMappings subType) {
        if (subType == null) {
            return (Class<S>) getInfoType();
        }
        Class<S> interf = subType.getInterface();
        if (!getInfoType().isAssignableFrom(interf)) {
            throw new ResponseStatusException(
                    BAD_REQUEST,
                    String.format(
                            "Invalid subtype of %s: %s", getInfoType().getSimpleName(), subType));
        }
        return interf;
    }

    private Mono<T> error(HttpStatus status, String messageFormat, Object... messageArgs) {
        return Mono.error(
                () ->
                        new ResponseStatusException(
                                status, String.format(messageFormat, messageArgs)));
    }

    private Mono<T> notFound(String messageFormat, Object... messageArgs) {
        return error(NOT_FOUND, messageFormat, messageArgs);
    }

    /**
     * Creates the given {@link CatalogInfo}; note if the {@link CatalogInfo#getId() identifier} is
     * provided it'll be respected, otherwise new one will be assigned.
     *
     * @return the created object, may it differ from the provided one (e.g. some properties
     *     assigned default values)
     */
    @PostMapping(path = "", consumes = "application/xml")
    @ResponseStatus(CREATED)
    public Mono<T> create(@RequestBody(required = true) T info) {
        return service.create(info, getInfoType())
                .switchIfEmpty(
                        notFound(
                                "%s '%s' does not exist",
                                getInfoType().getSimpleName(), info.getId()));
    }

    @PutMapping(path = "", consumes = "application/xml", produces = "application/xml")
    public Mono<T> update(@RequestBody(required = true) T info) {
        return service.update(info, getInfoType())
                .switchIfEmpty(
                        notFound(
                                "%s '%s' does not exist",
                                getInfoType().getSimpleName(), info.getId()));
    }

    @DeleteMapping(path = "/{id}", produces = "application/xml")
    @ResponseStatus(OK)
    public Mono<T> delete(@PathVariable(name = "id", required = true) String id) {
        return service.delete(id, getInfoType())
                .switchIfEmpty(
                        notFound("%s '%s' does not exist", getInfoType().getSimpleName(), id));
    }

    @GetMapping(path = "/{id}")
    public Mono<T> findById(
            @PathVariable("id") String id,
            @RequestParam(name = "subtype", required = false) ClassMappings subType) {

        return service.findById(id, getInfoType(subType))
                .switchIfEmpty(
                        notFound("%s '%s' does not exist", getInfoType().getSimpleName(), id));
    }

    @GetMapping(path = "/name/{name}")
    public Mono<T> findByName(
            @PathVariable(name = "name", required = true) String name,
            @RequestParam(name = "namespace", required = false) String namespace,
            @RequestParam(name = "subtype", required = false) ClassMappings subType) {

        Name qualifiedName = new NameImpl(namespace, name);
        return service.findByName(qualifiedName, getInfoType(subType))
                .switchIfEmpty(
                        notFound(
                                "%s '%s:%s' does not exist",
                                getInfoType(subType), namespace, name));
    }
    //
    // @PostMapping(path = "/filter", consumes = XML)
    // <U extends CI> List<U> query(
    // @RequestParam(name = "subtype", required = false) ClassMappings subType,
    // @RequestBody Filter filter);
}
