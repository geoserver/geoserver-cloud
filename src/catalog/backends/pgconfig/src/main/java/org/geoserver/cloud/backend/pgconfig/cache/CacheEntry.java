/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.backend.pgconfig.cache;

import java.util.Arrays;
import java.util.Objects;
import lombok.NonNull;
import org.geoserver.cloud.event.info.ConfigInfoType;
import org.jspecify.annotations.Nullable;

/**
 * An entry for the {@link EntityCache}, read from a database row: the JSON of an object under an {@code ENTITY} key,
 * the id of an object under a {@code NAME} key, or the GWC settings of a layer or group under a {@code TILE_LAYER} key.
 *
 * @param key the key of the entry
 * @param json the JSON in UTF-8, for an {@code ENTITY} or {@code TILE_LAYER} key
 * @param id the id of the object, for a {@code NAME} key
 */
public record CacheEntry(@NonNull CacheKey key, byte @Nullable [] json, @Nullable String id) {

    public static CacheEntry entity(ConfigInfoType type, String id, byte[] json) {
        return new CacheEntry(CacheKey.entity(type, id), json, null);
    }

    public static CacheEntry name(ConfigInfoType type, String prefixedName, String id) {
        return new CacheEntry(CacheKey.name(type, prefixedName), null, id);
    }

    /**
     * The GWC settings of a layer or group, see {@link CacheKey#tileLayer(String)}.
     *
     * @param json the JSON of the tile layer in UTF-8, null if the object has none. The entry then holds an empty
     *     array: a lookup finds it, and learns that the object has no tile layer.
     */
    public static CacheEntry tileLayer(String publishedId, byte @Nullable [] json) {
        byte[] stored = null == json ? new byte[0] : json;
        return new CacheEntry(CacheKey.tileLayer(publishedId), stored, null);
    }

    // The equals of a record compares arrays by identity
    @Override
    public boolean equals(Object other) {
        return other instanceof CacheEntry entry
                && key.equals(entry.key())
                && Arrays.equals(json, entry.json())
                && Objects.equals(id, entry.id());
    }

    @Override
    public int hashCode() {
        return 31 * Objects.hash(key, id) + Arrays.hashCode(json);
    }

    // The length only: the JSON of a store can hold a password in clear text
    @Override
    public String toString() {
        String value = null == json ? "id=" + id : json.length + " bytes";
        return "CacheEntry[" + key + ", " + value + "]";
    }
}
