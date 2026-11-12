/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.backend.pgconfig.cache;

/**
 * Keeps the JSON as it is.
 *
 * <p>Callers must not modify the arrays: encoding and decoding return their argument without a copy.
 */
public final class NoopCodec implements EntityCodec {

    @Override
    public byte[] encode(byte[] json) {
        return json;
    }

    @Override
    public byte[] decode(byte[] stored) {
        return stored;
    }

    @Override
    public String name() {
        return "none";
    }
}
