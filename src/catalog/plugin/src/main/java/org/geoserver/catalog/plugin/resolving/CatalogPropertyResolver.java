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
 * @since 1.0
 * @see ResolvingCatalogFacadeDecorator
 * @see ModificationProxy
 */
public class CatalogPropertyResolver {

    private final Supplier<Catalog> catalog;

    /**
     * Constructs a resolver with a fixed {@link Catalog} instance.
     *
     * @param catalog The {@link Catalog} to set on resolved objects; must not be null.
     * @throws NullPointerException if {@code catalog} is null.
     */
    private CatalogPropertyResolver(Catalog catalog) {
        Objects.requireNonNull(catalog, "Catalog must not be null");
        this.catalog = () -> catalog;
    }

    /**
     * Constructs a resolver with a dynamic {@link Catalog} supplier.
     *
     * @param catalog A supplier providing the {@link Catalog}; must not be null.
     * @throws NullPointerException if {@code catalog} is null.
     */
    private CatalogPropertyResolver(@NonNull Supplier<Catalog> catalog) {
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
    public static <I extends Info> UnaryOperator<I> of(Catalog catalog) {
        return new CatalogPropertyResolver(catalog)::apply;
    }

    /**
     * Creates a resolver for a dynamic {@link Catalog} supplier.
     *
     * @param <I>     The type of {@link Info} to resolve.
     * @param catalog A supplier providing the {@link Catalog}; must not be null.
     * @return A new {@link CatalogPropertyResolver} instance.
     * @throws NullPointerException if {@code catalog} is null.
     */
    public static <I extends Info> UnaryOperator<I> of(Supplier<Catalog> catalog) {
        return new CatalogPropertyResolver(catalog)::apply;
    }

    /**
     * Applies the resolver to an {@link Info} object, setting its catalog property if applicable.
     *
     * @param info The {@link Info} object to resolve; may be null.
     * @return The resolved object with catalog set, or null if {@code i} is null.
     */
    public <T> T apply(T info) {
        if (info == null) {
            return null;
        }
        return resolve(info);
    }

    /**
     * Resolves an {@link Info} object by setting its catalog property and processing nested references.
     *
     * <p>Unwraps any {@link ModificationProxy} and dispatches to type-specific resolution methods for
     * {@link StoreInfo}, {@link ResourceInfo}, {@link StyleInfo}, {@link PublishedInfo}, and
     * {@link LayerGroupStyle}.
     *
     * @param <I> The type of {@link Info}.
     * @param info   The object to resolve; may be null.
     * @return The resolved object, or null if {@code i} is null.
     */
    private <I> I resolve(@NonNull I orig) {
        I info = ModificationProxy.unwrap(orig);
        if (info instanceof StoreInfo store) {
            setCatalog(store);
        } else if (info instanceof ResourceInfo resource) {
            setCatalog(resource);
        } else if (info instanceof StyleInfo style) {
            setCatalog(style);
        } else if (info instanceof PublishedInfo published) {
            setCatalog(published);
        } else if (info instanceof LayerGroupStyle lgs) {
            setCatalog(lgs);
        }
        return orig;
    }

    /**
     * Resolves a collection by applying resolution to each element.
     *
     * @param list The collection to resolve; may be null (no action taken).
     */
    private void apply(Collection<?> list) {
        if (null != list) {
            list.forEach(this::apply);
        }
    }

    /**
     * Sets the catalog on a {@link PublishedInfo} object, dispatching to specific types.
     *
     * @param published The {@link PublishedInfo} to process; must not be null.
     */
    private void setCatalog(@NonNull PublishedInfo published) {
        if (published instanceof LayerInfo li) {
            setCatalog(li);
        } else if (published instanceof LayerGroupInfo lg) {
            setCatalog(lg);
        }
    }

    /**
     * Sets the catalog on a {@link LayerInfo} and resolves its nested references.
     *
     * @param layer The {@link LayerInfo} to process; must not be null.
     */
    private void setCatalog(@NonNull LayerInfo layer) {
        apply(layer.getResource());
        apply(layer.getDefaultStyle());
        apply(layer.getStyles());
    }

    /**
     * Sets the catalog on a {@link LayerGroupInfo} and resolves its nested references.
     *
     * @param layerGroup The {@link LayerGroupInfo} to process; must not be null.
     */
    private void setCatalog(@NonNull LayerGroupInfo layerGroup) {
        apply(layerGroup.getRootLayer());
        apply(layerGroup.getRootLayerStyle());
        apply(layerGroup.getLayers());
        apply(layerGroup.getStyles());
        apply(layerGroup.getLayerGroupStyles());
    }

    /**
     * Sets the catalog on a {@link LayerGroupStyle} and resolves its nested references.
     *
     * @param lgStyle The {@link LayerGroupStyle} to process; must not be null.
     */
    private void setCatalog(@NonNull LayerGroupStyle lgStyle) {
        if (null != lgStyle.getLayers()) {
            lgStyle.getLayers().forEach(this::apply);
        }
        if (null != lgStyle.getStyles()) {
            lgStyle.getStyles().forEach(this::apply);
        }
    }

    /**
     * Sets the catalog on a {@link StoreInfo} if it’s a concrete implementation.
     *
     * @param store The {@link StoreInfo} to process; must not be null.
     */
    private void setCatalog(@NonNull StoreInfo store) {
        if (store instanceof StoreInfoImpl storeImpl) {
            storeImpl.setCatalog(catalog());
        }
    }

    /**
     * Sets the catalog on a {@link ResourceInfo} and resolves its nested references.
     *
     * @param resource The {@link ResourceInfo} to process; must not be null.
     */
    private void setCatalog(@NonNull ResourceInfo resource) {
        resource.setCatalog(catalog());
        apply(resource.getStore());
        if (resource instanceof WMSLayerInfo wmsLayer) {
            apply(wmsLayer.getAllAvailableRemoteStyles());
        }
    }

    /**
     * Sets the catalog on a {@link StyleInfo} if it’s a concrete implementation.
     *
     * @param style The {@link StyleInfo} to process; must not be null.
     */
    private void setCatalog(@NonNull StyleInfo style) {
        if (style instanceof StyleInfoImpl styleImpl) {
            /*
             * When the style is remote (null id), StyleInfoImpl.getSLD() will check for a null catalog and return null.
             * Otherwise it'll call ResourcePool.getStyle(StyleInfo) and fail
             */
            boolean isRemoteStyle = null == styleImpl.getId();
            if (!isRemoteStyle) {
                styleImpl.setCatalog(catalog());
            }
        }
    }
}
