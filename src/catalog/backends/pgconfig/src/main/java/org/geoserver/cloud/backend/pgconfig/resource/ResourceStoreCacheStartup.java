/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.backend.pgconfig.resource;

import lombok.NonNull;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

/**
 * Lets the resource store index serve once the application is ready. Until then the store reads the database, see
 * {@link ResourceStoreCache#waitForStartup()}.
 *
 * <p>Spring Boot publishes {@link ApplicationReadyEvent} once the context is refreshed and its listeners have run:
 * GeoServer's security manager and GWC load their configuration on the context refresh, under the configuration lock.
 * The index is loaded here, on the thread of the event: Spring Boot reports the application ready for traffic once the
 * listeners of the event return, and the readiness probe waits for the load.
 */
public class ResourceStoreCacheStartup {

    private final ResourceStoreCache cache;

    public ResourceStoreCacheStartup(@NonNull ResourceStoreCache cache) {
        this.cache = cache;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        cache.startupComplete();
    }
}
