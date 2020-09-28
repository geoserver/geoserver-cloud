/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.impl;

import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.geoserver.cloud.catalog.client.reactivefeign.ReactiveResourceStoreClient;
import org.geoserver.platform.resource.LockProvider;
import org.geoserver.platform.resource.NullLockProvider;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.ResourceListener;
import org.geoserver.platform.resource.ResourceNotification;
import org.geoserver.platform.resource.ResourceNotificationDispatcher;
import org.geoserver.platform.resource.ResourceStore;

/** */
public class CatalogServiceResourceStore implements ResourceStore {

    /** LockProvider used to secure resources for exclusive access */
    private @Getter @Setter @NonNull LockProvider lockProvider = new NullLockProvider();

    private ReactiveResourceStoreClient client;

    /**
     * No-op notification dispatcher, resources are live-through-the-wire. May end up needing a
     * notification mechanism, but until proved differently, there seems to be no need for it.
     */
    private ResourceNotificationDispatcher resourceNotificationDispatcher =
            new NullResourceNotificationDispatcher();

    public @Override Resource get(String path) {
        Path normalized = Paths.get(path).normalize();
        return new CatalogServiceResource(client, normalized, lockProvider);
    }

    public @Override boolean remove(String path) {
        return client.delete(path).block();
    }

    public @Override boolean move(String path, String target) {
        return client.move(path, target).block();
    }

    public @Override ResourceNotificationDispatcher getResourceNotificationDispatcher() {
        return resourceNotificationDispatcher;
    }

    private static class NullResourceNotificationDispatcher
            implements ResourceNotificationDispatcher {
        public @Override void addListener(String resource, ResourceListener listener) {}

        public @Override boolean removeListener(String resource, ResourceListener listener) {
            return true;
        }

        public @Override void changed(ResourceNotification notification) {}
    }
}
