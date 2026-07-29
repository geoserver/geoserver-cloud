/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.config.catalog.backend.pgconfig;

import java.io.File;
import lombok.NonNull;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.ResourceStore;

/**
 * @since 1.4
 */
public class PgconfigGeoServerResourceLoader extends GeoServerResourceLoader {

    /**
     * @param resourceStore
     */
    public PgconfigGeoServerResourceLoader(@NonNull ResourceStore resourceStore) {
        super(resourceStore);
        File baseDirectory = resourceStore.get("").dir();
        setBaseDirectory(baseDirectory);
        createSecurityDirectory(resourceStore);
    }

    /**
     * Creates the {@code security/} directory on a fresh database, before any {@code AbstractAccessRuleDAO} bean
     * constructs.
     *
     * <p>The access rule DAOs ({@code rest.properties}, {@code layers.properties}, etc.) load their rules at bean
     * construction time and permanently keep an empty rule set when the security directory does not exist at that
     * point: their re-load check depends on a property-file watcher that is only created when the directory exists.
     * GeoServer's security config migration creates the directory otherwise, but it runs after all beans are
     * constructed. On a fresh database that ordering left the REST API with empty access rules, denying every request
     * with a 403 status code.
     */
    private void createSecurityDirectory(ResourceStore store) {
        store.get("security").dir();
    }
}
