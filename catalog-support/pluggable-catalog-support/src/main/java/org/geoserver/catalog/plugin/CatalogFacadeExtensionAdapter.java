/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin;

import static java.util.Spliterator.DISTINCT;
import static java.util.Spliterator.IMMUTABLE;
import static java.util.Spliterator.NONNULL;
import static java.util.Spliterator.ORDERED;

import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
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
import org.geoserver.catalog.impl.ClassMappings;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.catalog.plugin.forwarding.ForwardingCatalogFacade;
import org.geoserver.catalog.util.CloseableIterator;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;

/**
 * Adapts a regular {@link CatalogFacade} to a {@link ExtendedCatalogFacade}
 *
 * <p>Overrides {@link #setCatalog} with a catalog decorator that doesn't publish events, further
 * adapting a legacy {@link CatalogFacade} implementation, that mixes up responsibilities with the
 * catalog itself.
 */
public class CatalogFacadeExtensionAdapter extends ForwardingCatalogFacade
        implements ExtendedCatalogFacade {

    private final boolean adapt;

    private CatalogInfoTypeRegistry<?, Consumer<?>> updateToSaveBridge =
            new CatalogInfoTypeRegistry<>();

    public CatalogFacadeExtensionAdapter(CatalogFacade facade) {
        super(facade);
        adapt = !(facade instanceof ExtendedCatalogFacade);
        if (adapt) {
            updateToSaveBridge.consume(WorkspaceInfo.class, facade::save);
            updateToSaveBridge.consume(NamespaceInfo.class, facade::save);
            updateToSaveBridge.consume(StoreInfo.class, facade::save);
            updateToSaveBridge.consume(ResourceInfo.class, facade::save);
            updateToSaveBridge.consume(LayerInfo.class, facade::save);
            updateToSaveBridge.consume(LayerGroupInfo.class, facade::save);
            updateToSaveBridge.consume(StyleInfo.class, facade::save);
            updateToSaveBridge.consume(MapInfo.class, facade::save);
        }
    }

    /**
     * Bridges the new {@link ExtendedCatalogFacade#update(CatalogInfo, Patch)} method to the
     * corresponding {@link CatalogFacade#save} method in the decorated old style {@link
     * CatalogFacade}.
     *
     * <p>This would be unnecessary if {@link ExtendedCatalogFacade}'s {@code update()} is
     * incorporated to the official {@link CatalogFacade} interface.
     */
    public @Override <I extends CatalogInfo> I update(final I info, final Patch patch) {
        if (!adapt) {
            return ((ExtendedCatalogFacade) facade).update(info, patch);
        }

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

    public @Override <T extends CatalogInfo> Stream<T> query(Query<T> query) {
        if (!adapt) {
            return ((ExtendedCatalogFacade) facade).query(query);
        }
        Class<T> of = query.getType();
        Filter filter = query.getFilter();
        Integer offset = query.getOffset();
        Integer count = query.getCount();
        SortBy sortOrder = query.getSortBy().stream().findFirst().orElse(null);
        CloseableIterator<T> iterator = facade.list(of, filter, offset, count, sortOrder);

        int characteristics = ORDERED | DISTINCT | IMMUTABLE | NONNULL;
        Spliterator<T> spliterator = Spliterators.spliteratorUnknownSize(iterator, characteristics);
        Stream<T> stream = StreamSupport.stream(spliterator, false);
        stream.onClose(iterator::close);
        return stream;
    }
}
