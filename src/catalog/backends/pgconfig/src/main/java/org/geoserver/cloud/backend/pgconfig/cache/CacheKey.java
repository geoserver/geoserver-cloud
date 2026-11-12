/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.backend.pgconfig.cache;

import lombok.NonNull;
import org.geoserver.cloud.event.info.ConfigInfoType;

/**
 * The key of an entry of the entity cache.
 *
 * <p>An {@link Kind#ENTITY ENTITY} entry holds the JSON of an object. {@link Kind#NAME NAME} and {@link Kind#LOOKUP
 * LOOKUP} entries hold the id of an object, to find its {@code ENTITY} entry. A {@link Kind#TILE_LAYER TILE_LAYER}
 * entry holds the GWC settings of a layer or group: the JSON of its tile layer, or an empty value if it has none.
 *
 * @param type the type of the object
 * @param kind what the key holds
 * @param key the id of the object, its prefixed name, or the name of a lookup
 */
public record CacheKey(@NonNull ConfigInfoType type, @NonNull Kind kind, @NonNull String key) {

    /** What a key holds. */
    public enum Kind {
        /** An object id. */
        ENTITY,
        /** The prefixed name of an object: {@code prefix:name}, or the bare name of an object without a prefix. */
        NAME,
        /** A query with a single answer, such as the default workspace or the settings of a workspace. */
        LOOKUP,
        /** The id of a layer or group, for its GWC settings. */
        TILE_LAYER
    }

    public static CacheKey entity(ConfigInfoType type, String id) {
        return new CacheKey(type, Kind.ENTITY, id);
    }

    public static CacheKey name(ConfigInfoType type, String prefixedName) {
        return new CacheKey(type, Kind.NAME, prefixedName);
    }

    public static CacheKey lookup(ConfigInfoType type, String discriminator) {
        return new CacheKey(type, Kind.LOOKUP, discriminator);
    }

    /**
     * The key of the GWC settings of a layer or group. Its type is {@code PUBLISHED} for both: GWC looks the settings
     * up by id, without knowing whether the id is a layer or a group.
     */
    public static CacheKey tileLayer(String publishedId) {
        return new CacheKey(ConfigInfoType.PUBLISHED, Kind.TILE_LAYER, publishedId);
    }

    /**
     * Whether the key holds JSON, as an {@code ENTITY} or {@code TILE_LAYER} key does. The cache keeps JSON through its
     * codec. The other keys hold an id.
     */
    public boolean holdsJson() {
        return kind == Kind.ENTITY || kind == Kind.TILE_LAYER;
    }
}
