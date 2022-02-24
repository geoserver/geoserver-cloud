/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.service;

import lombok.NonNull;

import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.platform.resource.ResourceStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;

/** */
@Service
public class ReactiveResourceStoreImpl implements ReactiveResourceStore {

    private @Autowired @Qualifier("resourceStoreImpl") ResourceStore blockingStore;
    private @Autowired Scheduler catalogScheduler;

    public @Override Mono<Resource> get(String path) {
        return Mono.just(path).subscribeOn(catalogScheduler).map(blockingStore::get);
    }

    public @Override Mono<ByteBuffer> getContents(String path) {
        return get(path)
                .map(
                        r -> {
                            try {
                                return r.getContents();
                            } catch (IOException e) {
                                e.printStackTrace();
                                throw new UncheckedIOException(e);
                            }
                        })
                .map(ByteBuffer::wrap);
    }

    public @Override Mono<Resource> setContents(String path, ByteBuffer contents) {
        return get(path)
                .map(
                        resource -> {
                            byte[] byteArray = new byte[contents.remaining()];
                            contents.get(byteArray);
                            try {
                                resource.setContents(byteArray);
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                            return resource;
                        });
    }

    public @Override Mono<Boolean> remove(String path) {
        return Mono.just(path).subscribeOn(catalogScheduler).map(blockingStore::remove);
    }

    /**
     * @return the new resource, or empty if it couldn't be moved
     */
    public @Override Mono<Resource> move(String path, String target) {
        return Mono.just(path)
                .subscribeOn(catalogScheduler)
                .map(source -> blockingStore.move(source, target))
                .flatMap(moved -> moved ? get(target) : Mono.empty());
    }

    @Override
    public Flux<Resource> list(Resource resource) {
        return Mono.just(resource)
                .subscribeOn(catalogScheduler)
                .map(Resource::list)
                .flatMapMany(l -> Flux.fromStream(l.stream()));
    }

    @Override
    public Mono<Resource> create(String path, @NonNull Resource.Type type) {
        return get(path)
                .map(
                        r -> {
                            if (type == Type.DIRECTORY) {
                                r.dir();
                            } else {
                                r.file();
                            }
                            return r;
                        })
                .map(Resource::path)
                .flatMap(this::get);
    }
}
