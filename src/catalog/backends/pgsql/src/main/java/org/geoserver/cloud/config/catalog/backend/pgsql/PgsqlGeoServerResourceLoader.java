/*
 * (c) 2023 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.catalog.backend.pgsql;

import lombok.NonNull;

import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.ResourceStore;

import java.io.File;

/**
 * @since 1.4
 */
public class PgsqlGeoServerResourceLoader extends GeoServerResourceLoader {

    /**
     * @param resourceStore
     */
    public PgsqlGeoServerResourceLoader(@NonNull ResourceStore resourceStore) {
        super(resourceStore);
        File baseDirectory = resourceStore.get("").dir();
        setBaseDirectory(baseDirectory);
    }
}
