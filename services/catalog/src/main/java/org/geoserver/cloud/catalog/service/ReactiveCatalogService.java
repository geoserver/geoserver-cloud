/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.service;

import static java.util.Spliterator.DISTINCT;
import static java.util.Spliterator.IMMUTABLE;
import static java.util.Spliterator.NONNULL;
import static java.util.Spliterator.ORDERED;
import static java.util.Spliterators.spliteratorUnknownSize;

import com.google.common.base.Objects;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.ClassMappings;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.catalog.util.CloseableIteratorAdapter;
import org.geoserver.ows.util.OwsUtils;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

@Service
@Slf4j
public class ReactiveCatalogService {

    private Catalog catalog;

    private Scheduler scheduler;

    public @Autowired ReactiveCatalogService( //
            @Qualifier("catalog") Catalog realCatalog, //
            @Qualifier("catalogScheduler") Scheduler catalogScheduler) {

        this.catalog = realCatalog;
        this.scheduler = catalogScheduler;
    }

    public <T extends CatalogInfo> Mono<T> create(T info, Class<T> type) {
        return async(() -> workerFor(type).add(info));
    }

    public <T extends CatalogInfo> Mono<T> update(T info, Class<T> type) {
        return async(() -> workerFor(type).update(info));
    }

    public <T extends CatalogInfo> Mono<T> delete(String id, Class<T> type) {
        return async(() -> workerFor(type).delete(id, type));
    }

    public <T extends CatalogInfo> Mono<T> findById(final String id, final Class<T> type) {
        return async(() -> workerFor(type).getById(id, type));
    }

    public <T extends CatalogInfo> Mono<T> findByName(Name name, Class<T> type) {
        return async(() -> workerFor(type).getByName(name, type));
    }

    public <T extends CatalogInfo> Flux<T> findAll(@NonNull Class<T> infoType) {
        return Flux.fromStream(() -> iterable(infoType))
                .publishOn(scheduler)
                .subscribeOn(scheduler);
    }

    private static final AtomicLong iteratorSequence = new AtomicLong();

    private static class IdentifiedIterator<T> extends CloseableIteratorAdapter<T> {
        private final long id;

        public IdentifiedIterator(Iterator<T> wrapped) {
            super(wrapped);
            this.id = iteratorSequence.incrementAndGet();
        }

        public @Override String toString() {
            return getClass().getSimpleName() + "-" + id;
        }
    }

    private <T extends CatalogInfo> Stream<T> iterable(Class<T> infoType) {
        final String threadName = Thread.currentThread().getName();
        log.trace("opening iterator on {}", threadName);
        final CloseableIterator<T> iterator;
        try {
            iterator = new IdentifiedIterator<>(catalog.list(infoType, Filter.INCLUDE));
        } catch (RuntimeException e) {
            throw e;
        }
        log.trace("{} open on {}", iterator, threadName);
        final int characteristics = NONNULL | DISTINCT | IMMUTABLE | ORDERED;
        final Spliterator<T> spliterator = spliteratorUnknownSize(iterator, characteristics);
        final boolean parallel = false;
        final Stream<T> stream = StreamSupport.stream(spliterator, parallel);
        // the Flux closes the stream, make the stream close() call close the iterator to release
        // resources
        stream.onClose(
                () -> {
                    log.trace("{} closing on {}", iterator, threadName);
                    iterator.close();
                });
        return stream;
    }

    private <T> Mono<T> async(Callable<T> callable) {
        return Mono.fromCallable(callable).publishOn(scheduler);
    }

    @SuppressWarnings("unchecked")
    private <T extends CatalogInfo> Worker<T> workerFor(@NonNull Class<T> type) {
        if (WorkspaceInfo.class.isAssignableFrom(type)) return (Worker<T>) workspaceWorker;
        if (NamespaceInfo.class.isAssignableFrom(type)) return (Worker<T>) namespaceWorker;
        if (StoreInfo.class.isAssignableFrom(type)) return (Worker<T>) storeWorker;
        if (ResourceInfo.class.isAssignableFrom(type)) return (Worker<T>) resourceWorker;
        if (LayerInfo.class.isAssignableFrom(type)) return (Worker<T>) layerWorker;
        if (LayerGroupInfo.class.isAssignableFrom(type)) return (Worker<T>) layerGroupWorker;
        if (StyleInfo.class.isAssignableFrom(type)) return (Worker<T>) styleWorker;

        throw new IllegalArgumentException(
                "Unknown or unsupported CatalogInfo subtype: " + type.getName());
    }

    private interface Worker<T extends CatalogInfo> {
        T getById(String id, Class<? extends T> type);

        T getByName(Name name, Class<? extends T> type);

        T add(T info);

        T update(T info);

        T delete(String id, Class<? extends T> type);
    }

    @RequiredArgsConstructor(access = AccessLevel.PROTECTED)
    private abstract class AbstractWorker<T extends CatalogInfo> implements Worker<T> {

        private final @NonNull BiConsumer<Catalog, T> adder;
        private final @NonNull BiConsumer<Catalog, T> updater;
        private final @NonNull BiConsumer<Catalog, T> remover;

        public @Override T add(T info) {
            log.trace("Adding {} in {}", info, Thread.currentThread().getName());
            final String providedId = info.getId();
            Class<T> type = ClassMappings.fromImpl(info.getClass()).getInterface();
            try {
                adder.accept(catalog, info);
            } catch (RuntimeException e) {
                log.error(e.getMessage(), e);
                throw e;
            }
            if (null != providedId && !Objects.equal(providedId, info.getId())) {
                String msg =
                        String.format(
                                "Backend catalog implementation did not respect the provided object id. Expected: %s, assigned: %s",
                                providedId, info.getId());
                throw new IllegalStateException(msg);
            }
            // kind of a race condition here, if someone changed this very same object after it was
            // just added; yet it's important to return the stored version and not the one gotten as
            // argument, as it may have been populated with default values for some properties
            T created = getById(info.getId(), type);
            if (null == created) {
                throw new IllegalStateException(
                        String.format(
                                "%s[%s] not found once created",
                                type.getSimpleName(), info.getId()));
            }
            return created;
        }

        public @Override T update(T info) {
            Class<T> type = ClassMappings.fromImpl(info.getClass()).getInterface();
            T real = this.getById(info.getId(), type);
            if (real == null) {
                return null;
            }
            try {
                real = patchCatalogInfo(info, real, type);
                updater.accept(catalog, real);
            } catch (RuntimeException e) {
                log.error(
                        "Error updating {}:{}. {}",
                        type.getSimpleName(),
                        info.getId(),
                        e.getMessage(),
                        e);
                throw e;
            }
            return real;
        }

        private T patchCatalogInfo(T patch, T target, Class<T> type) {
            OwsUtils.copy(patch, target, type);
            return target;
        }

        public @Override T delete(String id, Class<? extends T> clazz) {
            Class<T> type = ClassMappings.fromInterface(clazz).getInterface();
            T real = this.getById(id, type);
            if (real == null) {
                return null;
            }
            remover.accept(catalog, real);
            return real;
        }
    }

    private Worker<WorkspaceInfo> workspaceWorker =
            new AbstractWorker<WorkspaceInfo>(Catalog::add, Catalog::save, Catalog::remove) {

                public @Override WorkspaceInfo getById(
                        String id, Class<? extends WorkspaceInfo> type) {
                    return catalog.getWorkspace(id);
                }

                public @Override WorkspaceInfo getByName(
                        Name name, Class<? extends WorkspaceInfo> type) {
                    return catalog.getWorkspaceByName(name.getLocalPart());
                }
            };

    private Worker<NamespaceInfo> namespaceWorker =
            new AbstractWorker<NamespaceInfo>(Catalog::add, Catalog::save, Catalog::remove) {

                @Override
                public NamespaceInfo getById(String id, Class<? extends NamespaceInfo> type) {
                    return catalog.getNamespace(id);
                }

                @Override
                public NamespaceInfo getByName(Name name, Class<? extends NamespaceInfo> type) {
                    return catalog.getNamespaceByPrefix(name.getLocalPart());
                }
            };

    private Worker<StoreInfo> storeWorker =
            new AbstractWorker<StoreInfo>(Catalog::add, Catalog::save, Catalog::remove) {

                @Override
                public StoreInfo getById(String id, Class<? extends StoreInfo> type) {
                    return catalog.getStore(id, type);
                }

                @Override
                public StoreInfo getByName(Name name, Class<? extends StoreInfo> type) {
                    String workspaceName = name.getNamespaceURI();
                    String storeName = name.getLocalPart();
                    if (StringUtils.isEmpty(workspaceName))
                        return catalog.getStoreByName(storeName, type);
                    return catalog.getStoreByName(workspaceName, storeName, type);
                }
            };

    private Worker<ResourceInfo> resourceWorker =
            new AbstractWorker<ResourceInfo>(Catalog::add, Catalog::save, Catalog::remove) {

                @Override
                public ResourceInfo getById(String id, Class<? extends ResourceInfo> type) {
                    return catalog.getResource(id, type);
                }

                @Override
                public ResourceInfo getByName(Name name, Class<? extends ResourceInfo> type) {
                    return catalog.getResourceByName(name, type);
                }
            };
    private Worker<LayerInfo> layerWorker =
            new AbstractWorker<LayerInfo>(Catalog::add, Catalog::save, Catalog::remove) {

                @Override
                public LayerInfo getById(String id, Class<? extends LayerInfo> type) {
                    return catalog.getLayer(id);
                }

                @Override
                public LayerInfo getByName(Name name, Class<? extends LayerInfo> type) {
                    return catalog.getLayerByName(name);
                }
            };

    private Worker<LayerGroupInfo> layerGroupWorker =
            new AbstractWorker<LayerGroupInfo>(Catalog::add, Catalog::save, Catalog::remove) {

                @Override
                public LayerGroupInfo getById(String id, Class<? extends LayerGroupInfo> type) {
                    return catalog.getLayerGroup(id);
                }

                @Override
                public LayerGroupInfo getByName(Name name, Class<? extends LayerGroupInfo> type) {
                    String workspaceName = name.getNamespaceURI();
                    String localName = name.getLocalPart();
                    if (StringUtils.isEmpty(workspaceName))
                        return catalog.getLayerGroupByName(localName);

                    return catalog.getLayerGroupByName(workspaceName, localName);
                }
            };

    private Worker<StyleInfo> styleWorker =
            new AbstractWorker<StyleInfo>(Catalog::add, Catalog::save, Catalog::remove) {

                @Override
                public StyleInfo getById(String id, Class<? extends StyleInfo> type) {
                    return catalog.getStyle(id);
                }

                @Override
                public StyleInfo getByName(Name name, Class<? extends StyleInfo> type) {
                    String workspaceName = name.getNamespaceURI();
                    String localName = name.getLocalPart();
                    if (StringUtils.isEmpty(workspaceName))
                        return catalog.getStyleByName(localName);
                    return catalog.getStyleByName(workspaceName, localName);
                }
            };
}
