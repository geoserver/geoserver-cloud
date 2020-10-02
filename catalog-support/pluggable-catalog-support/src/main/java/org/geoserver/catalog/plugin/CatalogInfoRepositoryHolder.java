/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin;

import org.geoserver.catalog.plugin.CatalogInfoRepository.LayerGroupRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.LayerRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.MapRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.NamespaceRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.ResourceRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.StoreRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.StyleRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.WorkspaceRepository;

public interface CatalogInfoRepositoryHolder {

    void setNamespaceRepository(NamespaceRepository namespaces);

    NamespaceRepository getNamespaceRepository();

    void setWorkspaceRepository(WorkspaceRepository workspaces);

    WorkspaceRepository getWorkspaceRepository();

    void setStoreRepository(StoreRepository stores);

    StoreRepository getStoreRepository();

    void setResourceRepository(ResourceRepository resources);

    ResourceRepository getResourceRepository();

    void setLayerRepository(LayerRepository layers);

    LayerRepository getLayerRepository();

    void setLayerGroupRepository(LayerGroupRepository layerGroups);

    LayerGroupRepository getLayerGroupRepository();

    void setStyleRepository(StyleRepository styles);

    StyleRepository getStyleRepository();

    void setMapRepository(MapRepository maps);

    MapRepository getMapRepository();
}
