/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin.forwarding;

import java.util.stream.Stream;
import lombok.NonNull;
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

/**
 * A decorator that implements the forwarding pattern for an {@link ExtendedCatalogFacade}.
 *
 * <p>This class extends {@link ForwardingCatalogFacade} to wrap an {@link ExtendedCatalogFacade} subject,
 * delegating all operations to it while providing the additional functionality of `ExtendedCatalogFacade`
 * (e.g., stream-based queries, patch updates). Subclasses can override methods to customize behavior,
 * such as adding logging, validation, or caching, without altering the core facade implementation.
 *
 * <p>Example usage:
 * <pre>
 * ExtendedCatalogFacade baseFacade = ...;
 * ForwardingExtendedCatalogFacade decorator = new ForwardingExtendedCatalogFacade(baseFacade) {
 *     &#64;Override
 *     public &lt;T extends CatalogInfo&gt; T add(@NonNull T info) {
 *         // Custom logic before forwarding
 *         return super.add(info);
 *     }
 * };
 * </pre>
 *
 * @since 1.0
 * @see ExtendedCatalogFacade
 * @see ForwardingCatalogFacade
 */
public class ForwardingExtendedCatalogFacade extends ForwardingCatalogFacade implements ExtendedCatalogFacade {

    /**
     * Constructs a forwarding facade wrapping an {@link ExtendedCatalogFacade} subject.
     *
     * @param facade The underlying {@link ExtendedCatalogFacade} to forward calls to; may be null (behavior depends on subclass).
     */
    public ForwardingExtendedCatalogFacade(ExtendedCatalogFacade facade) {
        super(facade);
    }

    /** {@inheritDoc} */
    @Override
    public <I extends CatalogInfo> I update(final I info, final Patch patch) {
        return asExtendedFacade().update(info, patch);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CatalogInfo> Stream<T> query(Query<T> query) {
        return asExtendedFacade().query(query);
    }

    /**
     * Casts the subject facade to {@link ExtendedCatalogFacade} for accessing extended methods.
     *
     * @return The subject as an {@link ExtendedCatalogFacade}; may be null if not set.
     */
    protected ExtendedCatalogFacade asExtendedFacade() {
        return (ExtendedCatalogFacade) super.facade;
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CatalogInfo> T add(@NonNull T info) {
        return asExtendedFacade().add(info);
    }

    /** {@inheritDoc} */
    @Override
    public void remove(@NonNull CatalogInfo info) {
        asExtendedFacade().remove(info);
    }

    /** {@inheritDoc} */
    @Override
    public WorkspaceInfo add(WorkspaceInfo info) {
        return (WorkspaceInfo) add((CatalogInfo) info);
    }

    /** {@inheritDoc} */
    @Override
    public void remove(WorkspaceInfo info) {
        remove((CatalogInfo) info);
    }

    /** {@inheritDoc} */
    @Override
    public NamespaceInfo add(NamespaceInfo info) {
        return (NamespaceInfo) add((CatalogInfo) info);
    }

    /** {@inheritDoc} */
    @Override
    public void remove(NamespaceInfo info) {
        remove((CatalogInfo) info);
    }

    /** {@inheritDoc} */
    @Override
    public StoreInfo add(StoreInfo info) {
        return (StoreInfo) add((CatalogInfo) info);
    }

    /** {@inheritDoc} */
    @Override
    public void remove(StoreInfo info) {
        remove((CatalogInfo) info);
    }

    /** {@inheritDoc} */
    @Override
    public ResourceInfo add(ResourceInfo info) {
        return (ResourceInfo) add((CatalogInfo) info);
    }

    /** {@inheritDoc} */
    @Override
    public void remove(ResourceInfo info) {
        remove((CatalogInfo) info);
    }

    /** {@inheritDoc} */
    @Override
    public LayerInfo add(LayerInfo info) {
        return (LayerInfo) add((CatalogInfo) info);
    }

    /** {@inheritDoc} */
    @Override
    public void remove(LayerInfo info) {
        remove((CatalogInfo) info);
    }

    /** {@inheritDoc} */
    @Override
    public LayerGroupInfo add(LayerGroupInfo info) {
        return (LayerGroupInfo) add((CatalogInfo) info);
    }

    /** {@inheritDoc} */
    @Override
    public void remove(LayerGroupInfo info) {
        remove((CatalogInfo) info);
    }

    /** {@inheritDoc} */
    @Override
    public StyleInfo add(StyleInfo info) {
        return (StyleInfo) add((CatalogInfo) info);
    }

    /** {@inheritDoc} */
    @Override
    public void remove(StyleInfo info) {
        remove((CatalogInfo) info);
    }

    /** {@inheritDoc} */
    @Override
    public MapInfo add(MapInfo info) {
        return (MapInfo) add((CatalogInfo) info);
    }

    /** {@inheritDoc} */
    @Override
    public void remove(MapInfo info) {
        remove((CatalogInfo) info);
    }
}
