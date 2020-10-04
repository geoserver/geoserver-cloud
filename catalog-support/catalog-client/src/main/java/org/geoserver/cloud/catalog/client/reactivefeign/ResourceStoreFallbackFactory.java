/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.reactivefeign;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.ResourceStore;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** */
@Component
public class ResourceStoreFallbackFactory
        implements reactivefeign.FallbackFactory<ReactiveResourceStoreClient> {

    private @Setter ResourceStore fallback;

    public @Override ReactiveResourceStoreClient apply(Throwable t) {
        if (fallback == null) {
            if (t instanceof RuntimeException) throw (RuntimeException) t;
            throw new RuntimeException(t);
        }
        return new ResourceStoreFallback(fallback);
    }

    @RequiredArgsConstructor
    private static class ResourceStoreFallback implements ReactiveResourceStoreClient {

        private final ResourceStore fallback;

        public @Override Flux<ResourceDescriptor> list(String path) {
            return Flux.fromStream(
                    fallback.get(path).list().stream().map(ResourceDescriptor::valueOf));
        }

        public @Override Mono<ResourceDescriptor> describe(String path) {
            return Mono.just(ResourceDescriptor.valueOf(fallback.get(path)));
        }

        public @Override Mono<org.springframework.core.io.Resource> getFileContent(String path) {
            return Mono.just(new InputStreamResource(fallback.get(path).in()));
        }

        public @Override Mono<ResourceDescriptor> put(String path, ByteBuffer contents) {
            byte[] byteArray = new byte[contents.remaining()];
            contents.get(byteArray);
            try {
                fallback.get(path).setContents(byteArray);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            return describe(path);
        }

        public @Override Mono<ResourceDescriptor> create(String path, ResourceDescriptor resource) {
            Resource r = fallback.get(path);
            switch (resource.getType()) {
                case DIRECTORY:
                    r.dir();
                    break;
                case RESOURCE:
                    r.file();
                    break;
                case UNDEFINED:
                default:
                    throw new IllegalArgumentException("Resource Type not specified");
            }
            return describe(path);
        }

        // public @Override Mono<ResourceDescriptor> update(String path, ResourceDescriptor
        // resource) {
        // throw new UnsupportedOperationException("what is this supposed to do?");
        // }

        public @Override Mono<Boolean> delete(String path) {
            return Mono.just(fallback.get(path).delete());
        }

        public @Override Mono<ResourceDescriptor> move(String path, String target) {
            boolean renamed = fallback.get(path).renameTo(fallback.get(target));
            Resource result = renamed ? fallback.get(target) : fallback.get(path);
            return Mono.just(ResourceDescriptor.valueOf(result));
        }
    }
}
