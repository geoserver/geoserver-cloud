/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin;

import static java.util.Spliterator.DISTINCT;
import static java.util.Spliterator.IMMUTABLE;
import static java.util.Spliterator.NONNULL;
import static java.util.Spliterator.ORDERED;

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
import org.geoserver.catalog.plugin.forwarding.ForwardingCatalog;
import org.geoserver.catalog.plugin.forwarding.ForwardingCatalogFacade;
import org.geoserver.catalog.util.CloseableIterator;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.sort.SortBy;
import org.geotools.util.logging.Logging;

import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Adapts a regular {@link CatalogFacade} to a {@link ExtendedCatalogFacade}
 *
 * <p>Overrides {@link #setCatalog} with a catalog decorator that doesn't publish events, further
 * adapting a legacy {@link CatalogFacade} implementation, that mixes up responsibilities with the
 * catalog itself.
 */
public class CatalogFacadeExtensionAdapter extends ForwardingCatalogFacade
        implements ExtendedCatalogFacade {

    private CatalogInfoTypeRegistry<Consumer<?>> updateToSaveBridge =
            new CatalogInfoTypeRegistry<>();

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

    @Override
    public void setCatalog(Catalog catalog) {
        if (catalog != null) {
            if (!(catalog instanceof CatalogPlugin)) {
                throw new IllegalArgumentException(
                        "Expected %s, got %s"
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
     * Bridges the new {@link ExtendedCatalogFacade#update(CatalogInfo, Patch)} method to the
     * corresponding {@link CatalogFacade#save} method in the decorated old style {@link
     * CatalogFacade}.
     *
     * <p>This would be unnecessary if {@link ExtendedCatalogFacade}'s {@code update()} is
     * incorporated to the official {@link CatalogFacade} interface.
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

    @SuppressWarnings("unchecked")
    private <T extends CatalogInfo> Consumer<T> saving(ClassMappings cm) {
        return (Consumer<T>) updateToSaveBridge.of(cm);
    }

    /** Adapts a {@link ExtendedCatalogFacade#query} call to {@link CatalogFacade#list} */
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
     * Catalog decorator that mutes all calls to fire catalog events, so legacy {@link
     * CatalogFacade}s trying to publish events have no effect, as its now catalog's sole
     * responsibility to do so.
     *
     * <p>Note this class extends {@link CatalogPlugin} and not {@link ForwardingCatalog} because of
     * the astonishing coupling of legacy {@link CatalogFacade} implementations on {@link
     * CatalogImpl}
     */
    @SuppressWarnings({"serial", "rawtypes"})
    public static class SilentCatalog extends CatalogPlugin {
        private static final Logger LOGGER = Logging.getLogger(SilentCatalog.class);
        private CatalogPlugin orig;

        public SilentCatalog(CatalogPlugin orig, CatalogFacadeExtensionAdapter facade) {
            super(facade, orig.isolated);
            this.orig = orig;
            super.resourceLoader = orig.getResourceLoader();
            super.resourcePool = orig.getResourcePool();
            super.removeListeners(CatalogListener.class);
        }

        public CatalogImpl getSubject() {
            return orig;
        }

        @Override
        public void addListener(CatalogListener listener) {
            LOGGER.fine(
                    () -> "Suppressing catalog listener " + listener.getClass().getCanonicalName());
        }

        @Override
        public void setFacade(CatalogFacade facade) {
            super.rawFacade = facade;
            super.facade = facade;
        }

        @Override
        public void fireAdded(CatalogInfo object) {
            LOGGER.fine("Suppressing catalog add event from legacy CatalogFacade");
        }

        @Override
        public void fireModified(
                CatalogInfo object, List propertyNames, List oldValues, List newValues) {
            LOGGER.fine("Suppressing catalog pre-modify event from legacy CatalogFacade");
        }

        @Override
        public void firePostModified(
                CatalogInfo object, List propertyNames, List oldValues, List newValues) {
            LOGGER.fine("Suppressing catalog post-modify event from legacy CatalogFacade");
        }

        @Override
        public void fireRemoved(CatalogInfo object) {
            LOGGER.fine("Suppressing catalog removed event from legacy CatalogFacade");
        }
    }
}
