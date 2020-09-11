/*
 * (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.CatalogRepository;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MapInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.StoreInfoImpl;
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

        // workspaces
        if (workspaces == null) {
            workspaces = new WorkspaceInfoLookup();
        }
        for (WorkspaceInfo ws : workspaces.findAll()) {
            resolve(ws);
        }

        // namespaces
        if (namespaces == null) {
            namespaces = new NamespaceInfoLookup();
        }
        for (NamespaceInfo ns : namespaces.findAll()) {
            resolve(ns);
        }

        // stores
        if (stores == null) {
            stores = new StoreInfoLookup();
        }
        for (Object o : stores.findAll()) {
            resolve((StoreInfoImpl) o);
        }

        // styles
        if (styles == null) {
            styles = new StyleInfoLookup();
        }
        for (StyleInfo s : styles.findAll()) {
            resolve(s);
        }

        // layers
        if (layers == null) {
            layers = new LayerInfoLookup();
        }

        // resources
        if (resources == null) {
            resources = new ResourceInfoLookup((LayerInfoLookup) layers);
        }

        for (Object o : resources.findAll()) {
            resolve((ResourceInfo) o);
        }
        for (LayerInfo l : layers.findAll()) {
            resolve(l);
        }

        // layer groups
        if (layerGroups == null) {
            layerGroups = new LayerGroupInfoLookup();
        }
        for (LayerGroupInfo lg : layerGroups.findAll()) {
            resolve(lg);
        }

        // maps
        if (maps == null) {
            maps = new MapInfoLookup();
        }
        for (MapInfo m : maps.findAll()) {
            resolve(m);
        }
    }
}
