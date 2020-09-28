/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.reactivefeign;

import java.nio.ByteBuffer;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.platform.resource.ResourceStore;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import reactivefeign.spring.config.ReactiveFeignClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Catalog-service client to support {@link ResourceStore} */
@ReactiveFeignClient( //
    name = "resources-service-client", //
    url = "${geoserver.backend.catalog-service.uri:catalog-service}", //
    path = "/api/v1/resources"
)
public interface ReactiveResourceStoreClient {
    /**
     * Path based resource access.
     *
     * <p>The returned Resource acts as a handle, and may be UNDEFINED. In general Resources are
     * created in a lazy fashion when used for the first time.
     *
     * @param path Path (using unix conventions, forward slash as separator) of requested resource
     * @return Resource at the indicated location (null is never returned although Resource may be
     *     UNDEFINED).
     * @throws IllegalArgumentException If path is invalid
     */
    @GetMapping("")
    Mono<ByteBuffer> get(@RequestParam("path") String path);

    @PutMapping("")
    Mono<Void> put(@RequestParam("path") String path, @RequestBody ByteBuffer contents);

    /**
     * Remove resource at indicated path (including individual resources or directories).
     *
     * <p>Returns <code>true</code> if Resource existed and was successfully removed. For read-only
     * content (or if a security check) prevents the resource from being removed <code>false</code>
     * is returned.
     *
     * @param path Path of resource to remove (using unix conventions, forward slash as separator)
     * @return <code>false</code> if doesn't exist or unable to remove
     */
    @DeleteMapping("")
    Mono<Boolean> delete(@RequestParam("path") String path);

    /**
     * Move resource at indicated path (including individual resources or directories).
     *
     * @param path Path of resource to move (using unix conventions, forward slash as separator)
     * @param target path for moved resource
     * @return true if resource was moved target path
     */
    @PutMapping("")
    Mono<Boolean> move(@RequestParam("path") String path, @RequestParam("target") String target);

    @GetMapping("")
    Mono<Type> getType(@RequestParam("path") String path);

    @GetMapping("")
    Flux<Resource> list(@RequestParam("path") String path);

    @GetMapping("")
    Mono<Long> lastModified(@RequestParam("path") String string);
}
