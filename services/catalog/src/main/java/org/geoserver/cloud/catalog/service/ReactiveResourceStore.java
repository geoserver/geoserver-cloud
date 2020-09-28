/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.service;

import reactor.core.publisher.Mono;

/** */
public interface ReactiveResourceStore {

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
    Mono<byte[]> get(String path);

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
    Mono<Boolean> remove(String path);

    /**
     * Move resource at indicated path (including individual resources or directories).
     *
     * @param path Path of resource to move (using unix conventions, forward slash as separator)
     * @param target path for moved resource
     * @return true if resource was moved target path
     */
    Mono<Boolean> move(String path, String target);

    /**
     * The Resource Notification Dispatcher
     *
     * @return resource notification dispatcher
     */
    // ResourceNotificationDispatcher getResourceNotificationDispatcher();
}
