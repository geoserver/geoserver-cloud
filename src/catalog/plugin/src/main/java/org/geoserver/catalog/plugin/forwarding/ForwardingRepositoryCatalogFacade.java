/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin.forwarding;

import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.plugin.CatalogInfoRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.LayerGroupRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.LayerRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.MapRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.NamespaceRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.ResourceRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.StoreRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.StyleRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.WorkspaceRepository;
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

    public ForwardingRepositoryCatalogFacade(RepositoryCatalogFacade facade) {
        super(facade);
    }

    @Override
    public void setNamespaceRepository(NamespaceRepository namespaces) {
        facade().setNamespaceRepository(namespaces);
    }

    @Override
    public void setWorkspaceRepository(WorkspaceRepository workspaces) {
        facade().setWorkspaceRepository(workspaces);
    }

    @Override
    public void setStoreRepository(StoreRepository stores) {
        facade().setStoreRepository(stores);
    }

    @Override
    public void setResourceRepository(ResourceRepository resources) {
        facade().setResourceRepository(resources);
    }

    @Override
    public void setLayerRepository(LayerRepository layers) {
        facade().setLayerRepository(layers);
    }

    @Override
    public void setLayerGroupRepository(LayerGroupRepository layerGroups) {
        facade().setLayerGroupRepository(layerGroups);
    }

    @Override
    public void setStyleRepository(StyleRepository styles) {
        facade().setStyleRepository(styles);
    }

    @Override
    public void setMapRepository(MapRepository maps) {
        facade().setMapRepository(maps);
    }

    @Override
    public NamespaceRepository getNamespaceRepository() {
        return facade().getNamespaceRepository();
    }

    @Override
    public WorkspaceRepository getWorkspaceRepository() {
        return facade().getWorkspaceRepository();
    }

    @Override
    public StoreRepository getStoreRepository() {
        return facade().getStoreRepository();
    }

    @Override
    public ResourceRepository getResourceRepository() {
        return facade().getResourceRepository();
    }

    @Override
    public LayerRepository getLayerRepository() {
        return facade().getLayerRepository();
    }

    @Override
    public LayerGroupRepository getLayerGroupRepository() {
        return facade().getLayerGroupRepository();
    }

    @Override
    public StyleRepository getStyleRepository() {
        return facade().getStyleRepository();
    }

    @Override
    public MapRepository getMapRepository() {
        return facade().getMapRepository();
    }

    @Override
    protected RepositoryCatalogFacade facade() {
        return (RepositoryCatalogFacade) facade;
    }

    @Override
    public <T extends CatalogInfo, R extends CatalogInfoRepository<T>> R repository(Class<T> of) {
        return facade().repository(of);
    }

    @Override
    public <T extends CatalogInfo, R extends CatalogInfoRepository<T>> R repositoryFor(T info) {
        return facade().repositoryFor(info);
    }
}
