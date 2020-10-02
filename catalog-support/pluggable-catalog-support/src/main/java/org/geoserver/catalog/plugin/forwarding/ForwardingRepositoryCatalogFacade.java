/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin.forwarding;

import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.plugin.CatalogInfoRepository.LayerGroupRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.LayerRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.MapRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.NamespaceRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.ResourceRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.StoreRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.StyleRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.WorkspaceRepository;
import org.geoserver.catalog.plugin.ExtendedCatalogFacade;
import org.geoserver.catalog.plugin.RepositoryCatalogFacade;

/**
 * {@link CatalogFacade} which forwards all its method calls to another {@code CatalogFacade} aiding
 * in implementing a decorator.
 *
 * <p>Subclasses should override one or more methods to modify the behavior of the backing facade as
 * needed.
 */
public class ForwardingRepositoryCatalogFacade extends ForwardingExtendedCatalogFacade
        implements RepositoryCatalogFacade {

    public ForwardingRepositoryCatalogFacade(ExtendedCatalogFacade facade) {
        super(facade);
    }

    public @Override void setNamespaceRepository(NamespaceRepository namespaces) {
        facade().setNamespaceRepository(namespaces);
    }

    public @Override void setWorkspaceRepository(WorkspaceRepository workspaces) {
        facade().setWorkspaceRepository(workspaces);
    }

    public @Override void setStoreRepository(StoreRepository stores) {
        facade().setStoreRepository(stores);
    }

    public @Override void setResourceRepository(ResourceRepository resources) {
        facade().setResourceRepository(resources);
    }

    public @Override void setLayerRepository(LayerRepository layers) {
        facade().setLayerRepository(layers);
    }

    public @Override void setLayerGroupRepository(LayerGroupRepository layerGroups) {
        facade().setLayerGroupRepository(layerGroups);
    }

    public @Override void setStyleRepository(StyleRepository styles) {
        facade().setStyleRepository(styles);
    }

    public @Override void setMapRepository(MapRepository maps) {
        facade().setMapRepository(maps);
    }

    public @Override NamespaceRepository getNamespaceRepository() {
        return facade().getNamespaceRepository();
    }

    public @Override WorkspaceRepository getWorkspaceRepository() {
        return facade().getWorkspaceRepository();
    }

    public @Override StoreRepository getStoreRepository() {
        return facade().getStoreRepository();
    }

    public @Override ResourceRepository getResourceRepository() {
        return facade().getResourceRepository();
    }

    public @Override LayerRepository getLayerRepository() {
        return facade().getLayerRepository();
    }

    public @Override LayerGroupRepository getLayerGroupRepository() {
        return facade().getLayerGroupRepository();
    }

    public @Override StyleRepository getStyleRepository() {
        return facade().getStyleRepository();
    }

    public @Override MapRepository getMapRepository() {
        return facade().getMapRepository();
    }

    protected RepositoryCatalogFacade facade() {
        return (RepositoryCatalogFacade) facade;
    }
}
