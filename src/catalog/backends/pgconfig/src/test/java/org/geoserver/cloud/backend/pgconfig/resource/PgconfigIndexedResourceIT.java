/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.backend.pgconfig.resource;

import org.springframework.jdbc.core.JdbcTemplate;

/** Runs the tests of {@link PgconfigResourceIT} on a store reading its rows from a {@link ResourceStoreCache}. */
class PgconfigIndexedResourceIT extends PgconfigResourceIT {

    private ResourceStoreCache index;

    @Override
    protected ResourceStoreCache newIndex(JdbcTemplate template) {
        index = new ResourceStoreCache("gs-resources", PgconfigResourceStore.indexLoader(template), 1024 * 1024);
        return index;
    }

    // a row changed behind the store reaches the index at its next load
    @Override
    protected void rowsChangedBehindStore() {
        index.clear();
    }
}
