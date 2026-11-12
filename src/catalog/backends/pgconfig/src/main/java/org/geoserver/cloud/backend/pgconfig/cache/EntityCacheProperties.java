/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.backend.pgconfig.cache;

import java.time.Duration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration of the caches of the pgconfig backend: the entity cache, shared by the requests of a service instance,
 * and the request cache, private to one request.
 */
@Data
@ConfigurationProperties("geoserver.backend.pgconfig.cache")
public class EntityCacheProperties {

    /** Whether catalog and configuration objects read from the database are kept in memory and shared by requests. */
    private boolean enabled = true;

    /** Maximum size of the cached catalog objects, in percent of the maximum heap. */
    private int maxHeapPercent = 5;

    /** How cached objects are compressed: zstd or none. */
    private Compression compression = Compression.ZSTD;

    /** How long a lookup with no result is remembered. */
    private Duration missTtl = Duration.ofSeconds(30);

    /**
     * Whether objects read by catalog listings are added to the cache. Off by default: a listing larger than the cache
     * would push out the objects read most often.
     */
    private boolean listingWriteThrough;

    /** The cache of decoded objects, private to one request. */
    private Request request = new Request();

    public enum Compression {
        /** zstd with a dictionary trained on catalog objects. */
        ZSTD,
        /** No compression. */
        NONE
    }

    /** Configuration of the cache private to one request. */
    @Data
    public static class Request {

        /** Whether a request keeps its decoded objects until it ends. */
        private boolean enabled = true;

        /**
         * Maximum number of objects kept by the cache of one request. The request keeps the objects it looked up and
         * the references it resolved, not the objects it listed.
         */
        private int maxEntries = 8_000;

        /**
         * Maximum number of objects kept by the caches of the requests in flight together, in one service instance.
         * Past it, the requests look objects up without keeping them, as past the maximum of one request.
         */
        private int maxTotalEntries = 100_000;
    }
}
