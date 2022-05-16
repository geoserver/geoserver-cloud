/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.gwc.tiling.model;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * @since 1.0
 */
public interface TileLayerInfo {

    /**
     * @return
     */
    String getName();

    Set<String> getFormats();

    /**
     * @return
     */
    List<GridSubsetInfo> getGridSubsets();

    /**
     * @return
     */
    int getMetaTilingWidth();

    /**
     * @return
     */
    int getMetaTilingHeight();

    Set<String> gridSubsetNames();

    /**
     * @param gridsetId
     * @return
     */
    Optional<GridSubsetInfo> gridSubset(String gridsetId);
}
