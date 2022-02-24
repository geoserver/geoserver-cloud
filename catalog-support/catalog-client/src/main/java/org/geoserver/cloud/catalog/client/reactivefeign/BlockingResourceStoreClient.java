/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.reactivefeign;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.geoserver.cloud.catalog.client.reactivefeign.ReactiveResourceStoreClient.ResourceDescriptor;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class BlockingResourceStoreClient {

    private final @NonNull ReactiveResourceStoreClient client;

    public <T> T block(Mono<T> command) {
        return blockOptional(command).orElse(null);
    }

    public <T> Optional<T> blockOptional(Mono<T> command) {
        if (Schedulers.isInNonBlockingThread()) {
            return CompletableFuture.supplyAsync(command::blockOptional).join();
        }
        return command.blockOptional(Duration.ofMillis(5000));
    }

    public <T> Stream<T> async(Flux<T> command) {
        return command.publishOn(Schedulers.parallel()).toStream();
    }

    public @NonNull ResourceDescriptor describe(String path) {
        return block(client.describe(path));
    }

    public @NonNull Stream<ResourceDescriptor> list(String path) {
        return async(client.list(path));
    }

    public @NonNull org.springframework.core.io.Resource getFileContent(String path) {
        return block(client.getFileContent(path));
    }

    public @NonNull ResourceDescriptor put(String path, ByteBuffer contents) {
        return block(client.put(path, contents));
    }

    public @NonNull ResourceDescriptor create(String path, ResourceDescriptor resource) {
        return block(client.create(path, resource));
    }

    public boolean delete(String path) {
        return block(client.delete(path));
    }

    public @NonNull Optional<ResourceDescriptor> move(String path, String target) {
        return blockOptional(client.move(path, target));
    }
}
