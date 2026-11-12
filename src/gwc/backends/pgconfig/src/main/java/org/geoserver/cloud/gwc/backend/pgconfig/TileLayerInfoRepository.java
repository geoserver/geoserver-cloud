/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.gwc.backend.pgconfig;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.dao.DataAccessException;

/**
 * Reads and writes the GWC settings of tile layers: the {@link TileLayerInfo} of a layer or group, by the id of the
 * layer or group. The lookups and listings of tile layers take the layer or group from the catalog.
 *
 * @see PgconfigTileLayerCatalog
 */
public interface TileLayerInfoRepository {

    /**
     * Returns the GWC settings of a layer or group. Empty if it has no tile layer, or if no layer or group has the id.
     *
     * <p>Callers must not modify the returned settings: a cache may return the same instance to the next callers.
     */
    Optional<TileLayerInfo> findSettings(String publishedId) throws DataAccessException;

    /**
     * Returns the GWC settings of several layers and groups, by id. An id is absent from the map if its layer or group
     * has no tile layer, or if no layer or group has the id.
     *
     * <p>Callers must not modify the returned settings, as for {@link #findSettings(String)}.
     */
    Map<String, TileLayerInfo> findAllSettings(Collection<String> publishedIds) throws DataAccessException;

    /** Stores the GWC settings of a layer or group. Returns false if no layer or group has the id. */
    boolean save(String publishedId, TileLayerInfo settings) throws DataAccessException;

    /** Removes the tile layer of a layer or group. Returns false if it had none. */
    boolean delete(String publishedId) throws DataAccessException;

    /** Returns the ids of the layers and groups with a tile layer. The listings of tile layers follow their order. */
    List<String> findAllIds() throws DataAccessException;

    int count() throws DataAccessException;

    Set<String> findAllNames() throws DataAccessException;
}
