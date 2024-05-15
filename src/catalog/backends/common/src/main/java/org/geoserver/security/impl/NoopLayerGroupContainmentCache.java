/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.security.impl;

import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.impl.CatalogImpl;

/**
 * A no-op {@link LayerGroupContainmentCache}, since some services like WFS do not deal with {@link
 * LayerGroupInfo layer groups} at all, then avoid the starup overhead.
 *
 * @since 1.8.2
 */
public class NoopLayerGroupContainmentCache extends LayerGroupContainmentCache {

    /**
     * Since {@link LayerGroupContainmentCache} is a class and the initialization methods are
     * private, we give it an empty in-memory catalog and call it a day
     */
    public NoopLayerGroupContainmentCache() {
        super(new CatalogImpl());
    }
}
