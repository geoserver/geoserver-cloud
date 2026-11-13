/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.backend.pgconfig.support;

import org.geoserver.cloud.backend.pgconfig.catalog.repository.RepositoryDecodes;

/**
 * Counts the catalog objects decoded from JSON by the pgconfig repositories and their row mappers, to assert how many
 * decodes an operation runs.
 *
 * <p>The repositories of the JVM share one count. Tests running at the same time add to it.
 */
public class DecodeCounter {

    private long baseline = RepositoryDecodes.total();

    /** Returns the decodes since this counter was created or last reset. */
    public long count() {
        return RepositoryDecodes.total() - baseline;
    }

    public void reset() {
        baseline = RepositoryDecodes.total();
    }
}
