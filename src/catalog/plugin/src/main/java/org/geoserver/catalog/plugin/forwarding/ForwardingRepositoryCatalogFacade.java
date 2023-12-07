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
        asExtendedFacade().setNamespaceRepository(namespaces);
    }

    @Override
    public void setWorkspaceRepository(WorkspaceRepository workspaces) {
        asExtendedFacade().setWorkspaceRepository(workspaces);
    }

    @Override
    public void setStoreRepository(StoreRepository stores) {
        asExtendedFacade().setStoreRepository(stores);
    }

    @Override
    public void setResourceRepository(ResourceRepository resources) {
        asExtendedFacade().setResourceRepository(resources);
    }

    @Override
    public void setLayerRepository(LayerRepository layers) {
        asExtendedFacade().setLayerRepository(layers);
    }

    @Override
    public void setLayerGroupRepository(LayerGroupRepository layerGroups) {
        asExtendedFacade().setLayerGroupRepository(layerGroups);
    }

    @Override
    public void setStyleRepository(StyleRepository styles) {
        asExtendedFacade().setStyleRepository(styles);
    }

    @Override
    public void setMapRepository(MapRepository maps) {
        asExtendedFacade().setMapRepository(maps);
    }

    @Override
    public NamespaceRepository getNamespaceRepository() {
        return asExtendedFacade().getNamespaceRepository();
    }

    @Override
    public WorkspaceRepository getWorkspaceRepository() {
        return asExtendedFacade().getWorkspaceRepository();
    }

    @Override
    public StoreRepository getStoreRepository() {
        return asExtendedFacade().getStoreRepository();
    }

    @Override
    public ResourceRepository getResourceRepository() {
        return asExtendedFacade().getResourceRepository();
    }

    @Override
    public LayerRepository getLayerRepository() {
        return asExtendedFacade().getLayerRepository();
    }

    @Override
    public LayerGroupRepository getLayerGroupRepository() {
        return asExtendedFacade().getLayerGroupRepository();
    }

    @Override
    public StyleRepository getStyleRepository() {
        return asExtendedFacade().getStyleRepository();
    }

    @Override
    public MapRepository getMapRepository() {
        return asExtendedFacade().getMapRepository();
    }

    @Override
    protected RepositoryCatalogFacade asExtendedFacade() {
        return (RepositoryCatalogFacade) facade;
    }

    @Override
    public <T extends CatalogInfo, R extends CatalogInfoRepository<T>> R repository(Class<T> of) {
        return asExtendedFacade().repository(of);
    }

    @Override
    public <T extends CatalogInfo, R extends CatalogInfoRepository<T>> R repositoryFor(T info) {
        return asExtendedFacade().repositoryFor(info);
    }
}
