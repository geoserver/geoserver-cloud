/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.backend.pgconfig.cache;

/**
 * Converts the JSON of a catalog or configuration object to the bytes kept by the entity cache, and back.
 *
 * <p>Implementations are thread-safe.
 */
public interface EntityCodec {

    byte[] encode(byte[] json);

    /**
     * Returns the JSON encoded by {@link #encode(byte[])}.
     *
     * @throws IllegalArgumentException if this codec cannot decode the bytes, for example bytes encoded with another
     *     dictionary
     */
    byte[] decode(byte[] stored);

    /** The name of the codec, as given to the {@code compression} property: {@code zstd} or {@code none}. */
    String name();
}
