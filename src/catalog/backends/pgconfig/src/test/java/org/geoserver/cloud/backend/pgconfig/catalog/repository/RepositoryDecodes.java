/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.backend.pgconfig.catalog.repository;

import org.geoserver.cloud.backend.pgconfig.support.DecodeCounter;

/**
 * Reads the decode count of the repositories for {@link DecodeCounter}. The count is package-private, and this class
 * shares the package of the repositories.
 */
public final class RepositoryDecodes {

    private RepositoryDecodes() {
        // static methods only
    }

    public static long total() {
        return PgconfigCatalogInfoRepository.decodeCount();
    }
}
