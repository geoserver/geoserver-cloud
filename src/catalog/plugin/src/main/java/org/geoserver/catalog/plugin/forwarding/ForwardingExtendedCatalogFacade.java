/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin.forwarding;

import java.util.stream.Stream;
import lombok.NonNull;
import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MapInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.plugin.ExtendedCatalogFacade;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.catalog.plugin.Query;

/** Adapts a regular {@link CatalogFacade} to a {@link ExtendedCatalogFacade} */
public class ForwardingExtendedCatalogFacade extends ForwardingCatalogFacade implements ExtendedCatalogFacade {

    public ForwardingExtendedCatalogFacade(ExtendedCatalogFacade facade) {
        super(facade);
    }

    @Override
    public <I extends CatalogInfo> I update(final I info, final Patch patch) {
        return asExtendedFacade().update(info, patch);
    }

    @Override
    public <T extends CatalogInfo> Stream<T> query(Query<T> query) {
        return asExtendedFacade().query(query);
    }

    protected ExtendedCatalogFacade asExtendedFacade() {
        return (ExtendedCatalogFacade) super.facade;
    }

    @Override
    public <T extends CatalogInfo> T add(@NonNull T info) {
        return asExtendedFacade().add(info);
    }

    @Override
    public void remove(@NonNull CatalogInfo info) {
        asExtendedFacade().remove(info);
    }

    @Override
    public WorkspaceInfo add(WorkspaceInfo info) {
        return (WorkspaceInfo) add((CatalogInfo) info);
    }

    @Override
    public void remove(WorkspaceInfo info) {
        remove((CatalogInfo) info);
    }

    @Override
    public NamespaceInfo add(NamespaceInfo info) {
        return (NamespaceInfo) add((CatalogInfo) info);
    }

    @Override
    public void remove(NamespaceInfo info) {
        remove((CatalogInfo) info);
    }

    @Override
    public StoreInfo add(StoreInfo info) {
        return (StoreInfo) add((CatalogInfo) info);
    }

    @Override
    public void remove(StoreInfo info) {
        remove((CatalogInfo) info);
    }

    @Override
    public ResourceInfo add(ResourceInfo info) {
        return (ResourceInfo) add((CatalogInfo) info);
    }

    @Override
    public void remove(ResourceInfo info) {
        remove((CatalogInfo) info);
    }

    @Override
    public LayerInfo add(LayerInfo info) {
        return (LayerInfo) add((CatalogInfo) info);
    }

    @Override
    public void remove(LayerInfo info) {
        remove((CatalogInfo) info);
    }

    @Override
    public LayerGroupInfo add(LayerGroupInfo info) {
        return (LayerGroupInfo) add((CatalogInfo) info);
    }

    @Override
    public void remove(LayerGroupInfo info) {
        remove((CatalogInfo) info);
    }

    @Override
    public StyleInfo add(StyleInfo info) {
        return (StyleInfo) add((CatalogInfo) info);
    }

    @Override
    public void remove(StyleInfo info) {
        remove((CatalogInfo) info);
    }

    @Override
    public MapInfo add(MapInfo info) {
        return (MapInfo) add((CatalogInfo) info);
    }

    @Override
    public void remove(MapInfo info) {
        remove((CatalogInfo) info);
    }
}
