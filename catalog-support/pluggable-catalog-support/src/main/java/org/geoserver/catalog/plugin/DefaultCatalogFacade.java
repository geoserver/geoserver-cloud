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
public class DefaultCatalogFacade extends AbstractCatalogFacade implements CatalogFacade {

    public DefaultCatalogFacade() {
        this(null);
    }

    public DefaultCatalogFacade(Catalog catalog) {
        super(catalog);
        setNamespaces(new NamespaceInfoLookup());
        setWorkspaces(new WorkspaceInfoLookup());
        setStores(new StoreInfoLookup());
        setLayers(new LayerInfoLookup());
        setResources(new ResourceInfoLookup((LayerInfoLookup) layers));
        setLayerGroups(new LayerGroupInfoLookup());
        setMaps(new MapInfoLookup());
        setStyles(new StyleInfoLookup());
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

        workspaces.findAll().forEach(this::resolve);
        namespaces.findAll().forEach(this::resolve);
        stores.findAll().forEach(this::resolve);
        styles.findAll().forEach(this::resolve);
        resources.findAll().forEach(this::resolve);
        layers.findAll().forEach(this::resolve);
        layerGroups.findAll().forEach(this::resolve);
        maps.findAll().forEach(this::resolve);
    }

    private <I extends CatalogInfo, R extends CatalogInfoRepository<I>> R resolve(
            R current, Supplier<R> factory) {
        return current == null ? factory.get() : current;
    }
}
