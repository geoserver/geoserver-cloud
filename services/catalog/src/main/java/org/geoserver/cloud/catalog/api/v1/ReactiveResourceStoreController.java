/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.api.v1;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE;
import static org.springframework.http.MediaType.APPLICATION_STREAM_JSON_VALUE;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.nio.ByteBuffer;
import java.util.Objects;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.geoserver.cloud.catalog.service.ReactiveResourceStore;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** */
@RestController
@RequestMapping(path = ReactiveResourceStoreController.BASE_URI)
public class ReactiveResourceStoreController {

    public static final String BASE_URI = "/api/v1/resources";

    private @Autowired ReactiveResourceStore store;

    @Data
    @NoArgsConstructor
    public static class WebResource {

        private @JsonIgnore Resource resource;
        private @JsonIgnore String incomingPath;
        private @JsonIgnore Resource.Type incomingType;
        private @JsonIgnore long incomingLastModified;

        public WebResource(@NonNull Resource resource) {
            this.resource = resource;
        }

        public @JsonProperty @NonNull String getPath() {
            return resource == null ? incomingPath : resource.path();
        }

        public @JsonProperty @NonNull Resource.Type getType() {
            return resource == null ? incomingType : resource.getType();
        }

        public @JsonProperty long getLastModified() {
            return resource == null ? incomingLastModified : resource.lastmodified();
        }

        public void setPath(@NonNull String path) {
            this.incomingPath = path;
        }

        public void setLastModified(long lastModified) {
            this.incomingLastModified = lastModified;
        }

        public void setType(@NonNull Type type) {
            this.incomingType = type;
        }
    }

    private WebResource toWebResource(Resource r) {
        return new WebResource(r);
    }

    @GetMapping(
        path = "/{*path}",
        produces = {APPLICATION_STREAM_JSON_VALUE}
    )
    public Flux<WebResource> list(@PathVariable("path") String path) {
        return store.get(path).flatMapMany(store::list).map(this::toWebResource);
    }

    @GetMapping(path = "/{*path}", produces = APPLICATION_JSON_VALUE)
    public Mono<WebResource> describe(@PathVariable("path") String path) {
        return store.get(path).map(this::toWebResource);
    }

    @PutMapping(
        path = "/{*path}",
        consumes = APPLICATION_OCTET_STREAM_VALUE,
        produces = APPLICATION_JSON_VALUE
    )
    public Mono<WebResource> put(
            @PathVariable("path") String path, @RequestBody ByteBuffer contents) {

        return this.store.setContents(path, contents).map(this::toWebResource);
    }

    @GetMapping(
        path = "/{*path}",
        produces = {APPLICATION_OCTET_STREAM_VALUE}
    )
    public Mono<org.springframework.core.io.Resource> getFileContent(
            @PathVariable("path") String path) {
        return store.get(path).map(this::toSpringResource);
    }

    // can't use instanceof, FileSystemResourceStore.FileSystemResource is package private
    private Class<?> zeroCopyResourceType;

    private org.springframework.core.io.Resource toSpringResource(Resource gsResource) {
        final Type type = gsResource.getType();
        if (type == Type.DIRECTORY)
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, gsResource.path() + " is a directory, not a file");
        if (type == Type.UNDEFINED)
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, gsResource.path() + " does not exist");

        // can't use instanceof, FileSystemResourceStore.FileSystemResource is package private
        Class<? extends Resource> resourceType = gsResource.getClass();
        if (zeroCopyResourceType != null && resourceType == zeroCopyResourceType) {
            // leverage zero-copy transfer
            return new FileSystemResource(gsResource.file());
        }
        final String resourceClassName = resourceType.getCanonicalName();
        if ("org.geoserver.platform.resource.FileSystemResourceStore.FileSystemResource"
                .equals(resourceClassName)) {
            zeroCopyResourceType = resourceType;
            // leverage zero-copy transfer
            return new FileSystemResource(gsResource.file());
        }
        return new InputStreamResource(gsResource.in());
    }

    @PostMapping("/{*path}")
    public Mono<WebResource> create(
            @PathVariable("path") String path, @RequestBody WebResource resource) {

        return store.create(path, resource.getType()).map(this::toWebResource);
    }

    @PutMapping("/{*path}")
    public Mono<WebResource> update(
            @PathVariable("path") String path, @RequestBody WebResource resource) {

        Objects.requireNonNull(resource.getPath());

        Mono<Resource> updated =
                store.get(path)
                        .filter(r -> r.getType() != Type.UNDEFINED)
                        .switchIfEmpty(
                                Mono.error(
                                        () -> new ResponseStatusException(HttpStatus.NO_CONTENT)))
                        .flatMap(
                                r -> {
                                    if (!Objects.equals(r.path(), resource.getPath())) {
                                        return store.move(r.path(), resource.getPath());
                                    }
                                    return Mono.just(r);
                                });
        return updated.map(this::toWebResource);
    }

    @DeleteMapping("/{*path}")
    public Mono<Boolean> delete(@PathVariable("path") String path) {
        return store.remove(path);
    }
}
