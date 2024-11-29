/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.gwc.backend.pgconfig;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.dao.DataAccessException;

/**
 * {@code TileLayerInfoRepository} defines CRUD operations on {@link TileLayerInfo}.
 *
 * @see PgconfigTileLayerCatalog
 */
public interface TileLayerInfoRepository {

    void add(TileLayerInfo pgInfo) throws DataAccessException;

    boolean save(TileLayerInfo pgInfo) throws DataAccessException;

    boolean delete(String workspaceName, String localName) throws DataAccessException;

    Stream<TileLayerInfo> findAll() throws DataAccessException;

    Optional<TileLayerInfo> find(String workspaceName, String localName) throws DataAccessException;

    int count() throws DataAccessException;

    Set<String> findAllNames() throws DataAccessException;

    boolean exists(String workspaceName, String localName) throws DataAccessException;
}
