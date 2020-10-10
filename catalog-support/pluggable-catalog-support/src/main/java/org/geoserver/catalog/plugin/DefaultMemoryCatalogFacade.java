/*
 * (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin;

import java.util.function.Supplier;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.CatalogRepository;
import org.geoserver.catalog.plugin.CatalogInfoLookup.LayerGroupInfoLookup;
import org.geoserver.catalog.plugin.CatalogInfoLookup.LayerInfoLookup;
import org.geoserver.catalog.plugin.CatalogInfoLookup.MapInfoLookup;
import org.geoserver.catalog.plugin.CatalogInfoLookup.NamespaceInfoLookup;
import org.geoserver.catalog.plugin.CatalogInfoLookup.ResourceInfoLookup;
import org.geoserver.catalog.plugin.CatalogInfoLookup.StoreInfoLookup;
import org.geoserver.catalog.plugin.CatalogInfoLookup.StyleInfoLookup;
import org.geoserver.catalog.plugin.CatalogInfoLookup.WorkspaceInfoLookup;

/**
 * Default catalog facade implementation using in-memory {@link CatalogRepository repositories} to
 * store the {@link CatalogInfo}
 */
public class DefaultMemoryCatalogFacade extends RepositoryCatalogFacadeImpl
        implements CatalogFacade {

    public DefaultMemoryCatalogFacade() {
        this(null);
    }

    public DefaultMemoryCatalogFacade(Catalog catalog) {
        super(catalog);
        setNamespaceRepository(new NamespaceInfoLookup());
        setWorkspaceRepository(new WorkspaceInfoLookup());
        setStoreRepository(new StoreInfoLookup());
        setLayerRepository(new LayerInfoLookup());
        setResourceRepository(new ResourceInfoLookup((LayerInfoLookup) layers));
        setLayerGroupRepository(new LayerGroupInfoLookup());
        setMapRepository(new MapInfoLookup());
        setStyleRepository(new StyleInfoLookup());
    }

    public @Override void resolve() {
        // JD creation checks are done here b/c when xstream depersists
        // some members may be left null
        workspaces = resolve(workspaces, WorkspaceInfoLookup::new);
        namespaces = resolve(namespaces, NamespaceInfoLookup::new);
        stores = resolve(stores, StoreInfoLookup::new);
        styles = resolve(styles, StyleInfoLookup::new);
        layers = resolve(layers, LayerInfoLookup::new);
        resources = resolve(resources, () -> new ResourceInfoLookup((LayerInfoLookup) layers));
        layerGroups = resolve(layerGroups, LayerGroupInfoLookup::new);
        maps = resolve(maps, MapInfoLookup::new);
    }

    private <I extends CatalogInfo, R extends CatalogInfoRepository<I>> R resolve(
            R current, Supplier<R> factory) {
        return current == null ? factory.get() : current;
    }
}
