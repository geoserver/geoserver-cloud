/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.catalog.plugin;

import static java.util.Spliterator.DISTINCT;
import static java.util.Spliterator.IMMUTABLE;
import static java.util.Spliterator.NONNULL;
import static java.util.Spliterator.ORDERED;

import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.geoserver.catalog.Catalog;
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
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.catalog.impl.ClassMappings;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.catalog.plugin.forwarding.ForwardingCatalogFacade;
import org.geoserver.catalog.util.CloseableIterator;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.sort.SortBy;
import org.geotools.util.logging.Logging;

/**
 * Adapts a legacy {@link CatalogFacade} implementation to the {@link ExtendedCatalogFacade} interface.
 *
 * <p>This class bridges the gap between traditional {@link CatalogFacade} implementations and the
 * enhanced {@link ExtendedCatalogFacade} API introduced in GeoServer Cloud. It extends
 * {@link ForwardingCatalogFacade} to wrap an existing facade, adding support for modern methods like
 * {@link #update(CatalogInfo, Patch)} and {@link #query(Query)} while suppressing unwanted event
 * publishing from legacy facades. The adapter ensures compatibility with older facade implementations
 * that may intertwine catalog operations with event handling, a responsibility now delegated to the
 * {@link Catalog} itself.
 *
 * <p>Key adaptations:
 * <ul>
 *   <li><strong>Event Suppression:</strong> Wraps the catalog in a {@link SilentCatalog} to mute event
 *       firing from legacy facade operations.</li>
 *   <li><strong>Update Bridging:</strong> Maps {@link #update(CatalogInfo, Patch)} to legacy {@code save}
 *       methods using a type registry.</li>
 *   <li><strong>Query Support:</strong> Adapts {@link #query(Query)} to the legacy {@link #list} method
 *       with stream-based results.</li>
 * </ul>
 *
 * <p>This adapter is intended as a transitional tool until {@link ExtendedCatalogFacade} features are
 * fully integrated into GeoServer’s core {@link CatalogFacade}.
 *
 * @since 1.0
 * @see ExtendedCatalogFacade
 * @see CatalogFacade
 * @see SilentCatalog
 */
public class CatalogFacadeExtensionAdapter extends ForwardingCatalogFacade implements ExtendedCatalogFacade {

    private CatalogInfoTypeRegistry<Consumer<?>> updateToSaveBridge = new CatalogInfoTypeRegistry<>();

    /**
     * Constructs a new adapter wrapping a legacy {@link CatalogFacade}.
     *
     * <p>Initializes the adapter by decorating the provided facade and setting up a bridge from
     * {@link #update(CatalogInfo, Patch)} to the legacy {@code save} methods for each catalog info type.
     * If the facade already implements {@link ExtendedCatalogFacade}, an exception is thrown to avoid
     * unnecessary wrapping.
     *
     * @param facade The legacy {@link CatalogFacade} to adapt; must not be null.
     * @throws NullPointerException if {@code facade} is null.
     * @throws IllegalArgumentException if {@code facade} is already an {@link ExtendedCatalogFacade}.
     */
    public CatalogFacadeExtensionAdapter(CatalogFacade facade) {
        super(facade);
        if (facade instanceof ExtendedCatalogFacade) {
            throw new IllegalArgumentException("facade is already an ExtendedCatalogFacade");
        }

        updateToSaveBridge.consume(WorkspaceInfo.class, facade::save);
        updateToSaveBridge.consume(NamespaceInfo.class, facade::save);
        updateToSaveBridge.consume(StoreInfo.class, facade::save);
        updateToSaveBridge.consume(ResourceInfo.class, facade::save);
        updateToSaveBridge.consume(LayerInfo.class, facade::save);
        updateToSaveBridge.consume(LayerGroupInfo.class, facade::save);
        updateToSaveBridge.consume(StyleInfo.class, facade::save);
        updateToSaveBridge.consume(MapInfo.class, facade::save);

        Catalog currentCatalog = facade.getCatalog();
        if (currentCatalog != null) {
            setCatalog(currentCatalog);
        }
    }

    /**
     * Sets the catalog for this facade, wrapping it to suppress event publishing.
     *
     * <p>Overrides the parent method to ensure the catalog is a {@link CatalogPlugin} instance and
     * decorates it with {@link SilentCatalog} if it isn’t already silent. This prevents legacy facade
     * implementations from firing catalog events, aligning with GeoServer Cloud’s design where event
     * handling is the catalog’s responsibility.
     *
     * @param catalog The catalog to set; may be null to unset.
     * @throws IllegalArgumentException if {@code catalog} is non-null and not a {@link CatalogPlugin}.
     * @example Setting a catalog:
     *          <pre>
     *          CatalogPlugin catalog = new CatalogPlugin(facade);
     *          adapter.setCatalog(catalog);
     *          </pre>
     */
    @Override
    public void setCatalog(Catalog catalog) {
        if (catalog != null) {
            if (!(catalog instanceof CatalogPlugin)) {
                throw new IllegalArgumentException("Expected %s, got %s"
                        .formatted(
                                CatalogPlugin.class.getName(),
                                catalog.getClass().getName()));
            }
            if (!(catalog instanceof SilentCatalog)) {
                catalog = new SilentCatalog((CatalogPlugin) catalog, this);
            }
        }
        super.setCatalog(catalog);
    }

    /**
     * Updates a catalog object with a patch, bridging to legacy {@code save} methods.
     *
     * <p>This method adapts the modern {@link ExtendedCatalogFacade#update(CatalogInfo, Patch)} API to
     * the legacy {@link CatalogFacade#save} methods of the wrapped facade. It creates a proxy of the
     * original object, applies the patch, and delegates to the appropriate {@code save} method via the
     * {@code updateToSaveBridge} registry. This bridge is temporary and would be unnecessary if
     * {@code update} is integrated into {@link CatalogFacade}.
     *
     * @param <I>   The type of {@link CatalogInfo} to update.
     * @param info  The catalog object to update; must not be null.
     * @param patch The patch containing changes to apply; must not be null.
     * @return The updated {@link CatalogInfo} object after applying the patch.
     * @throws NullPointerException if {@code info} or {@code patch} is null.
     * @throws IllegalArgumentException if the object type is not supported by the bridge.
     * @example Updating a layer’s title:
     *          <pre>
     *          LayerInfo layer = ...; // fetched from catalog
     *          Patch patch = new Patch().with("title", "New Title");
     *          LayerInfo updated = adapter.update(layer, patch);
     *          </pre>
     */
    @Override
    public <I extends CatalogInfo> I update(final I info, final Patch patch) {
        final I orig = ModificationProxy.unwrap(info);
        ClassMappings cm = CatalogInfoTypeRegistry.determineKey(orig.getClass());
        @SuppressWarnings("unchecked")
        I proxied = (I) ModificationProxy.create(orig, cm.getInterface());
        patch.applyTo(proxied, cm.getInterface());
        saving(cm).accept(proxied);
        return proxied;
    }

    /**
     * Retrieves a type-specific {@code save} consumer from the update bridge registry.
     *
     * @param <T>  The type of {@link CatalogInfo}.
     * @param cm   The {@link ClassMappings} key identifying the type.
     * @return A {@link Consumer} that invokes the legacy {@code save} method for the type.
     */
    @SuppressWarnings("unchecked")
    private <T extends CatalogInfo> Consumer<T> saving(ClassMappings cm) {
        return (Consumer<T>) updateToSaveBridge.of(cm);
    }

    /**
     * Queries the catalog by adapting to the legacy {@link CatalogFacade#list} method.
     *
     * <p>This method converts a {@link Query} into parameters for the underlying facade’s {@code list}
     * method, returning a {@link Stream} of results. The stream is configured with characteristics
     * ({@code ORDERED}, {@code DISTINCT}, {@code IMMUTABLE}, {@code NONNULL}) and ensures proper resource
     * closure via {@link CloseableIterator}.
     *
     * @param <T>   The type of {@link CatalogInfo} to query.
     * @param query The query defining type, filter, sorting, and pagination; must not be null.
     * @return A {@link Stream} of matching catalog objects; never null.
     * @throws NullPointerException if {@code query} is null.
     * @example Querying layers:
     *          <pre>
     *          Query<LayerInfo> query = Query.valueOf(LayerInfo.class, Filter.INCLUDE);
     *          try (Stream<LayerInfo> layers = adapter.query(query)) {
     *              layers.forEach(l -> System.out.println(l.getName()));
     *          }
     *          </pre>
     */
    @Override
    public <T extends CatalogInfo> Stream<T> query(Query<T> query) {
        Class<T> of = query.getType();
        Filter filter = query.getFilter();
        Integer offset = query.getOffset();
        Integer count = query.getCount();
        SortBy sortOrder = query.getSortBy().stream().findFirst().orElse(null);

        CloseableIterator<T> iterator;
        if (sortOrder == null) {
            iterator = facade.list(of, filter, offset, count);
        } else {
            iterator = facade.list(of, filter, offset, count, sortOrder);
        }

        int characteristics = ORDERED | DISTINCT | IMMUTABLE | NONNULL;
        Spliterator<T> spliterator = Spliterators.spliteratorUnknownSize(iterator, characteristics);
        Stream<T> stream = StreamSupport.stream(spliterator, false);
        stream.onClose(iterator::close);
        return stream;
    }

    /**
     * A catalog decorator that suppresses event publishing from legacy {@link CatalogFacade} implementations.
     *
     * <p>This nested class extends {@link CatalogPlugin} to wrap an existing catalog, muting all event firing
     * methods (e.g., {@link #fireAdded}) to prevent legacy facades from publishing events. Event handling is
     * now the sole responsibility of the catalog, addressing the tight coupling in legacy {@link CatalogImpl}
     * designs. It logs suppression actions for debugging purposes.
     *
     * @since 1.0
     * @see CatalogPlugin
     * @see CatalogImpl
     */
    @SuppressWarnings({"serial", "rawtypes"})
    public static class SilentCatalog extends CatalogPlugin {
        private static final Logger LOGGER = Logging.getLogger(SilentCatalog.class);
        private CatalogPlugin orig;

        /**
         * Constructs a silent catalog wrapper around an existing {@link CatalogPlugin}.
         *
         * <p>Initializes the wrapper with the provided facade and original catalog, copying resource-related
         * fields and removing listeners to mute event propagation.
         *
         * @param orig   The original {@link CatalogPlugin} to wrap; must not be null.
         * @param facade The facade associated with this catalog; must not be null.
         * @throws NullPointerException if {@code orig} or {@code facade} is null.
         */
        public SilentCatalog(CatalogPlugin orig, CatalogFacadeExtensionAdapter facade) {
            super(facade, orig.isolated);
            this.orig = orig;
            super.resourceLoader = orig.getResourceLoader();
            super.resourcePool = orig.getResourcePool();
            super.removeListeners(CatalogListener.class);
        }

        /**
         * Returns the underlying {@link CatalogImpl} subject being wrapped.
         *
         * @return The original {@link CatalogImpl} instance.
         */
        public CatalogImpl getSubject() {
            return orig;
        }

        /**
         * Suppresses adding a catalog listener, logging the action.
         *
         * @param listener The listener to suppress; may be null (ignored).
         */
        @Override
        public void addListener(CatalogListener listener) {
            LOGGER.fine(() -> "Suppressing catalog listener "
                    + (listener != null ? listener.getClass().getCanonicalName() : "null"));
        }

        /**
         * Overrides facade setting to ensure proper delegation.
         *
         * @param facade The facade to set; must not be null.
         */
        @Override
        public void setFacade(CatalogFacade facade) {
            super.rawFacade = facade;
            super.facade = facade;
        }

        /**
         * Suppresses firing an add event, logging the suppression.
         *
         * @param object The object added (ignored).
         */
        @Override
        public void fireAdded(CatalogInfo object) {
            LOGGER.fine("Suppressing catalog add event from legacy CatalogFacade");
        }

        /**
         * Suppresses firing a pre-modify event, logging the suppression.
         *
         * @param object        The modified object (ignored).
         * @param propertyNames The property names (ignored).
         * @param oldValues     The old values (ignored).
         * @param newValues     The new values (ignored).
         */
        @Override
        public void fireModified(CatalogInfo object, List propertyNames, List oldValues, List newValues) {
            LOGGER.fine("Suppressing catalog pre-modify event from legacy CatalogFacade");
        }

        /**
         * Suppresses firing a post-modify event, logging the suppression.
         *
         * @param object        The modified object (ignored).
         * @param propertyNames The property names (ignored).
         * @param oldValues     The old values (ignored).
         * @param newValues     The new values (ignored).
         */
        @Override
        public void firePostModified(CatalogInfo object, List propertyNames, List oldValues, List newValues) {
            LOGGER.fine("Suppressing catalog post-modify event from legacy CatalogFacade");
        }

        /**
         * Suppresses firing a remove event, logging the suppression.
         *
         * @param object The removed object (ignored).
         */
        @Override
        public void fireRemoved(CatalogInfo object) {
            LOGGER.fine("Suppressing catalog removed event from legacy CatalogFacade");
        }
    }
}
