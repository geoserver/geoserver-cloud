/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.impl;

import java.util.function.BooleanSupplier;

/**
 * A step run by {@link GsCloudLayerGroupContainmentCache} before it lists the layer groups. A backend uses it to load
 * the groups and their members in bulk, instead of one lookup per group during the listing.
 */
@FunctionalInterface
public interface LayerGroupWarmUp {

    /** A warm-up that does nothing, for backends with no bulk loading. */
    LayerGroupWarmUp NONE = cancelled -> {};

    /**
     * Loads the groups and their members into the cache of the backend, on the calling thread, until cancelled.
     *
     * @param cancelled answers true when the build asking for the warm-up is cancelled
     */
    void warmUp(BooleanSupplier cancelled);
}
