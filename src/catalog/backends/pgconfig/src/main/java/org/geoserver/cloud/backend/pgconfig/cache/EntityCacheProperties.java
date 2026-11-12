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
 * the request cache, private to one request, the cache of the resource store, and the cache of built GWC tile layers.
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

    /**
     * The number of built GWC tile layers kept by a service instance. 0 keeps none: lookups build their tile layers.
     *
     * <p>The bound counts tile layers, not bytes. A kept tile layer holds a decoded copy of its layer or group, with
     * the members and their references. Measured on a lab catalog, a tile layer takes about 15 KiB for a layer and
     * about 100 KiB for a group of 30 layers. A thousand kept groups of that size take about 100 MiB.
     */
    private int tileLayers = 1_000;

    /** The cache of decoded objects, private to one request. */
    private Request request = new Request();

    /** The index of the resource store and the content read from it. */
    private Resources resources = new Resources();

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

    /**
     * Configuration of the cache of the resource store. The index is loaded on first use and after a clear of the
     * caches. A content read stays until a change or the memory bound removes it.
     */
    @Data
    public static class Resources {

        /**
         * Whether the paths of the resource store are kept in memory, loaded in one query, with the content of the
         * files read.
         */
        private boolean enabled = true;

        /** Maximum size of the content kept, in percent of the maximum heap, and 64 MiB at most. */
        private int maxHeapPercent = 1;
    }
}
