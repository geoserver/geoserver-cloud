/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.catalog.plugin.resolving;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import lombok.NonNull;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.impl.LayerGroupStyle;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.catalog.impl.StoreInfoImpl;
import org.geoserver.catalog.impl.StyleInfoImpl;
import org.geoserver.catalog.plugin.forwarding.ResolvingCatalogFacadeDecorator;

/**
 * A {@link UnaryOperator} that sets the {@link Catalog} property on {@link CatalogInfo} objects requiring it.
 *
 * <p>This utility class implements a resolver for use with {@link ResolvingCatalogFacadeDecorator#setOutboundResolver(UnaryOperator)},
 * ensuring that {@link CatalogInfo} objects (e.g., {@link StoreInfo}, {@link ResourceInfo}, {@link StyleInfo})
 * have their catalog reference set before being returned by a facade. It recursively processes nested references
 * (e.g., styles in a {@link LayerInfo}, layers in a {@link LayerGroupInfo}) to maintain catalog consistency.
 * The resolver uses a catalog supplier to provide the current catalog instance, supporting dynamic catalog access.
 *
 * <p>Key features:
 * <ul>
 *   <li><strong>Catalog Assignment:</strong> Sets the catalog on supported {@link CatalogInfo} types.</li>
 *   <li><strong>Recursive Resolution:</strong> Handles nested structures like styles, layers, and group styles.</li>
 *   <li><strong>Null Safety:</strong> Gracefully handles null inputs, returning null without modification.</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>
 * Catalog catalog = ...;
 * ResolvingCatalogFacadeDecorator facade = ...;
 * UnaryOperator<CatalogInfo> resolver = CatalogPropertyResolver.of(catalog);
 * facade.setOutboundResolver(resolver);
 * </pre>
 *
 * @param <T> The type of {@link Info} to resolve (typically {@link CatalogInfo}).
 * @since 1.0
 * @see ResolvingCatalogFacadeDecorator
 * @see ModificationProxy
 */
public class CatalogPropertyResolver<T extends Info> implements UnaryOperator<T> {

    private final Supplier<Catalog> catalog;

    /**
     * Constructs a resolver with a fixed {@link Catalog} instance.
     *
     * @param catalog The {@link Catalog} to set on resolved objects; must not be null.
     * @throws NullPointerException if {@code catalog} is null.
     */
    public CatalogPropertyResolver(Catalog catalog) {
        Objects.requireNonNull(catalog, "Catalog must not be null");
        this.catalog = () -> catalog;
    }

    /**
     * Constructs a resolver with a dynamic {@link Catalog} supplier.
     *
     * @param catalog A supplier providing the {@link Catalog}; must not be null.
     * @throws NullPointerException if {@code catalog} is null.
     */
    public CatalogPropertyResolver(@NonNull Supplier<Catalog> catalog) {
        Objects.requireNonNull(catalog, "Catalog supplier must not be null");
        this.catalog = catalog;
    }

    /**
     * Retrieves the current {@link Catalog} instance from the supplier.
     *
     * @return The {@link Catalog}; never null.
     */
    @NonNull
    protected Catalog catalog() {
        return catalog.get();
    }

    /**
     * Creates a resolver for a fixed {@link Catalog} instance.
     *
     * @param <I>     The type of {@link Info} to resolve.
     * @param catalog The {@link Catalog} to use; must not be null.
     * @return A new {@link CatalogPropertyResolver} instance.
     * @throws NullPointerException if {@code catalog} is null.
     */
    public static <I extends Info> CatalogPropertyResolver<I> of(Catalog catalog) {
        return new CatalogPropertyResolver<>(catalog);
    }

    /**
     * Creates a resolver for a dynamic {@link Catalog} supplier.
     *
     * @param <I>     The type of {@link Info} to resolve.
     * @param catalog A supplier providing the {@link Catalog}; must not be null.
     * @return A new {@link CatalogPropertyResolver} instance.
     * @throws NullPointerException if {@code catalog} is null.
     */
    public static <I extends Info> CatalogPropertyResolver<I> of(Supplier<Catalog> catalog) {
        return new CatalogPropertyResolver<>(catalog);
    }

    /**
     * Applies the resolver to an {@link Info} object, setting its catalog property if applicable.
     *
     * @param i The {@link Info} object to resolve; may be null.
     * @return The resolved object with catalog set, or null if {@code i} is null.
     */
    @Override
    public T apply(T i) {
        return resolve(i);
    }

    /**
     * Resolves an {@link Info} object by setting its catalog property and processing nested references.
     *
     * <p>Unwraps any {@link ModificationProxy} and dispatches to type-specific resolution methods for
     * {@link StoreInfo}, {@link ResourceInfo}, {@link StyleInfo}, {@link PublishedInfo}, and
     * {@link LayerGroupStyle}.
     *
     * @param <I> The type of {@link Info}.
     * @param i   The object to resolve; may be null.
     * @return The resolved object, or null if {@code i} is null.
     */
    @SuppressWarnings("unchecked")
    private <I> I resolve(I i) {
        i = null == i ? null : ModificationProxy.unwrap(i);
        if (i instanceof StoreInfo store) {
            setCatalog(store);
        } else if (i instanceof ResourceInfo resource) {
            setCatalog(resource);
        } else if (i instanceof StyleInfo style) {
            setCatalog(style);
        } else if (i instanceof PublishedInfo published) {
            setCatalog(published);
        } else if (i instanceof LayerGroupStyle lgs) {
            setCatalog(lgs);
        }
        return i;
    }

    /**
     * Resolves a collection by applying resolution to each element.
     *
     * @param list The collection to resolve; may be null (no action taken).
     */
    private void resolve(Collection<?> list) {
        if (null != list) {
            list.forEach(this::resolve);
        }
    }

    /**
     * Sets the catalog on a {@link PublishedInfo} object, dispatching to specific types.
     *
     * @param i The {@link PublishedInfo} to process; must not be null.
     */
    private void setCatalog(@NonNull PublishedInfo i) {
        if (i instanceof LayerInfo li) {
            setCatalog(li);
        } else if (i instanceof LayerGroupInfo lg) {
            setCatalog(lg);
        }
    }

    /**
     * Sets the catalog on a {@link LayerInfo} and resolves its nested references.
     *
     * @param i The {@link LayerInfo} to process; must not be null.
     */
    private void setCatalog(@NonNull LayerInfo i) {
        resolve(i.getResource());
        resolve(i.getDefaultStyle());
        resolve(i.getStyles());
    }

    /**
     * Sets the catalog on a {@link LayerGroupInfo} and resolves its nested references.
     *
     * @param i The {@link LayerGroupInfo} to process; must not be null.
     */
    private void setCatalog(@NonNull LayerGroupInfo i) {
        resolve(i.getRootLayer());
        resolve(i.getRootLayerStyle());
        resolve(i.getLayers());
        resolve(i.getStyles());
        resolve(i.getLayerGroupStyles());
    }

    /**
     * Sets the catalog on a {@link LayerGroupStyle} and resolves its nested references.
     *
     * @param i The {@link LayerGroupStyle} to process; must not be null.
     */
    private void setCatalog(@NonNull LayerGroupStyle i) {
        if (null != i.getLayers()) {
            i.getLayers().forEach(this::setCatalog);
        }
        if (null != i.getStyles()) {
            i.getStyles().forEach(this::setCatalog);
        }
    }

    /**
     * Sets the catalog on a {@link StoreInfo} if it’s a concrete implementation.
     *
     * @param i The {@link StoreInfo} to process; must not be null.
     */
    private void setCatalog(@NonNull StoreInfo i) {
        if (i instanceof StoreInfoImpl store) {
            store.setCatalog(catalog());
        }
    }

    /**
     * Sets the catalog on a {@link ResourceInfo} and resolves its nested references.
     *
     * @param i The {@link ResourceInfo} to process; must not be null.
     */
    private void setCatalog(@NonNull ResourceInfo i) {
        i.setCatalog(catalog());
        resolve(i.getStore());
        if (i instanceof WMSLayerInfo wmsLayer) {
            resolve(wmsLayer.getAllAvailableRemoteStyles());
        }
    }

    /**
     * Sets the catalog on a {@link StyleInfo} if it’s a concrete implementation.
     *
     * @param i The {@link StyleInfo} to process; must not be null.
     */
    private void setCatalog(@NonNull StyleInfo i) {
        if (i instanceof StyleInfoImpl style) {
            /*
             * When the style is remote (null id), StyleInfoImpl.getSLD() will check for a null catalog and return null.
             * Otherwise it'll call ResourcePool.getStyle(StyleInfo) and fail
             */
            boolean isRemoteStyle = null == style.getId();
            if (!isRemoteStyle) {
                style.setCatalog(catalog());
            }
        }
    }
}
