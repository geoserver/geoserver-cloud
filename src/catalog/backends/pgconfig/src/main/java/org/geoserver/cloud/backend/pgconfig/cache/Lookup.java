/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.backend.pgconfig.cache;

import java.util.Arrays;
import lombok.NonNull;

/** The answer of the {@link EntityCache} to a lookup by key. */
public sealed interface Lookup {

    /**
     * The key holds a value. Callers must not modify the array: it may be the one held by the cache.
     *
     * @param json the JSON of the object for an {@code ENTITY} key, or the id of the object in UTF-8 for a {@code NAME}
     *     or {@code LOOKUP} key. For a {@code TILE_LAYER} key, the GWC settings of the layer or group, or empty if it
     *     has no tile layer.
     */
    record Hit(@NonNull byte[] json) implements Lookup {

        // The equals of a record compares arrays by identity
        @Override
        public boolean equals(Object other) {
            return other instanceof Hit hit && Arrays.equals(json, hit.json());
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(json);
        }

        // The length only: the JSON of a store can hold a password in clear text
        @Override
        public String toString() {
            return "Hit[" + json.length + " bytes]";
        }
    }

    /** The key holds a miss: the last query for the key found nothing. */
    record Miss() implements Lookup {}

    /** The key is not cached, or the cache is suspended: the caller must query the database. */
    record Absent() implements Lookup {}
}
