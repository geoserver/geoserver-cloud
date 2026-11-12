/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.backend.pgconfig.catalog.repository;

import java.util.concurrent.atomic.LongAdder;

/**
 * Counts the catalog objects decoded from JSON, for tests. {@link CatalogInfoRowMapper} counts its decodes here, and
 * {@link PgconfigCatalogInfoRepository#decodeCount()} reads the total. The count has a class of its own to keep the row
 * mapper independent of the repositories.
 */
final class CatalogInfoDecodes {

    private static final LongAdder DECODES = new LongAdder();

    private CatalogInfoDecodes() {
        // static methods only
    }

    static void increment() {
        DECODES.increment();
    }

    static long total() {
        return DECODES.sum();
    }
}
