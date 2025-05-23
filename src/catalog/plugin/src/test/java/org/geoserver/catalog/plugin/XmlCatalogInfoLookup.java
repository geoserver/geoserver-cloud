/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.catalog.plugin;

import com.google.common.collect.Ordering;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MapInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.ClassMappings;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.ows.util.OwsUtils;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.sort.SortBy;
import org.geotools.api.filter.sort.SortOrder;
import org.geotools.util.logging.Logging;

/**
 * Exemplar alternative implementations of {@link CatalogInfoRepository} that store serialized
 * string versions of {@link CatalogInfo} objects in memory, to validate the API and showcase the
 * ease of implementation.
 *
 * <p>This is not for production, it will have the worse possible performance since all the lookups
 * are performed by de-serializing on the fly with only an id to serialized form lookup table. The
 * point is to have a test implementation that mimics what'd happen when the {@link CatalogInfo}
 * back-end is not the {@link CatalogInfoLookup default in-memory} one, by returning "detached"
 * objects.
 *
 * @see XmlCatalogInfoLookupConformanceTest
 */
abstract class XmlCatalogInfoLookup<T extends CatalogInfo> implements CatalogInfoRepository<T> {
    static final Logger LOGGER = Logging.getLogger(XmlCatalogInfoLookup.class);

    /** constant no-op Comparator for {@link #providedOrder()} */
    private static final Ordering<?> PROVIDED_ORDER = Ordering.allEqual();

    protected ConcurrentMap<String, String> idMap = new ConcurrentHashMap<>();

    private XStreamPersister codec;

    protected XmlCatalogInfoLookup(XStreamPersister codec) {
        this.codec = codec;
    }

    static final <T> Predicate<T> alwaysTrue() {
        return x -> true;
    }

    private static void checkNotAProxy(CatalogInfo value) {
        if (Proxy.isProxyClass(value.getClass())) {
            throw new IllegalArgumentException("Proxy values shall not be passed to CatalogInfoLookup");
        }
    }

    protected String serialize(T info) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            codec.save(info, out);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out.toString(StandardCharsets.UTF_8);
    }

    protected <I extends Info> I deserialize(String serialized, Class<I> type) {
        if (serialized == null) {
            return null;
        }
        I loaded;
        try {
            ByteArrayInputStream in = new ByteArrayInputStream(serialized.getBytes(StandardCharsets.UTF_8));
            loaded = codec.load(in, type);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        OwsUtils.resolveCollections(loaded);
        return loaded;
    }

    protected T deserialize(String serialized) {
        return deserialize(serialized, getContentType());
    }

    @Override
    public void add(T value) {
        checkNotAProxy(value);

        String serialized = serialize(value);
        if (null != idMap.putIfAbsent(value.getId(), serialized)) {
            String msg = "%s:%s already exists, not replaced"
                    .formatted(getContentType().getSimpleName(), value.getId());
            LOGGER.warning(msg);
        }
    }

    @Override
    public void remove(T value) {
        checkNotAProxy(value);
        idMap.remove(value.getId());
    }

    @Override
    public <I extends T> I update(final I value, Patch patch) {
        checkNotAProxy(value);
        T storedValue;
        // for the sake of correctness, get the stored value, contract does not force the supplied
        // value to be attached
        @SuppressWarnings("unchecked")
        Class<I> type = (Class<I>) ClassMappings.fromImpl(value.getClass()).getInterface();
        synchronized (idMap) {
            storedValue = this.findById(value.getId(), type)
                    .orElseThrow(() -> new NoSuchElementException(
                            "%s with id %s does not exist".formatted(type.getSimpleName(), value.getId())));

            patch.applyTo(storedValue);
            idMap.put(value.getId(), serialize(storedValue));
        }
        return type.cast(storedValue);
    }

    protected Stream<T> all() {
        return idMap.values().stream().map(this::deserialize);
    }

    protected <U extends T> Stream<U> allOf(Class<U> clazz) {
        return all().filter(clazz::isInstance).map(clazz::cast);
    }

    @Override
    public void dispose() {
        idMap.clear();
    }

    @Override
    public boolean canSortBy(String propertyName) {
        return CatalogInfoLookup.canSort(propertyName, getContentType());
    }

    @Override
    public <U extends T> Stream<U> findAll(Query<U> query) {
        Comparator<U> comparator = toComparator(query);
        Predicate<U> predicate = toPredicate(query.getFilter());
        return list(query.getType(), predicate, comparator)
                .skip(query.offset().orElse(0))
                .limit(query.count().orElse(Integer.MAX_VALUE));
    }

    @Override
    public <U extends T> long count(Class<U> type, Filter filter) {
        return Filter.INCLUDE.equals(filter) && getContentType().equals(type)
                ? idMap.size()
                : findAll(Query.valueOf(type, filter)).count();
    }

    public static <U extends CatalogInfo> Comparator<U> toComparator(Query<?> query) {
        Comparator<U> comparator = providedOrder();
        for (SortBy sortBy : query.getSortBy()) {
            comparator =
                    (comparator == PROVIDED_ORDER) ? comparator(sortBy) : comparator.thenComparing(comparator(sortBy));
        }
        return comparator;
    }

    @SuppressWarnings("unchecked")
    private static <U extends CatalogInfo> Comparator<U> providedOrder() {
        return (Comparator<U>) PROVIDED_ORDER;
    }

    protected <V> Predicate<V> toPredicate(Filter filter) {
        return filter::evaluate;
    }

    private static <U extends CatalogInfo> Comparator<U> comparator(final SortBy sortOrder) {
        Comparator<U> comparator = new Comparator<>() {
            @Override
            public int compare(U o1, U o2) {
                Object v1 = OwsUtils.get(o1, sortOrder.getPropertyName().getPropertyName());
                Object v2 = OwsUtils.get(o2, sortOrder.getPropertyName().getPropertyName());
                if (v1 == null) {
                    if (v2 == null) {
                        return 0;
                    } else {
                        return -1;
                    }
                } else if (v2 == null) {
                    return 1;
                }
                @SuppressWarnings({"rawtypes", "unchecked"})
                Comparable<Object> c1 = (Comparable) v1;
                @SuppressWarnings({"rawtypes", "unchecked"})
                Comparable<Object> c2 = (Comparable) v2;
                return c1.compareTo(c2);
            }
        };
        if (SortOrder.DESCENDING.equals(sortOrder.getSortOrder())) {
            comparator = comparator.reversed();
        }
        return comparator;
    }

    <U extends CatalogInfo> Stream<U> list(Class<U> clazz, Predicate<U> predicate, Comparator<U> comparator) {

        Stream<U> stream = all().filter(clazz::isInstance).map(clazz::cast).filter(predicate);
        if (comparator != CatalogInfoLookup.PROVIDED_ORDER) {
            stream = stream.sorted(comparator);
        }
        return stream;
    }

    /** Looks up a CatalogInfo by class and identifier */
    @Override
    public <U extends T> Optional<U> findById(String id, Class<U> clazz) {
        T deserialized = deserialize(idMap.get(id));
        return Optional.ofNullable(clazz.isInstance(deserialized) ? clazz.cast(deserialized) : null);
    }

    <U extends T> Optional<U> findFirst(Class<U> clazz, Predicate<U> predicate) {
        return allOf(clazz).filter(predicate).findFirst();
    }

    @Override
    public void syncTo(CatalogInfoRepository<T> target) {
        all().forEach(target::add);
    }

    public static class NamespaceInfoLookup extends XmlCatalogInfoLookup<NamespaceInfo> implements NamespaceRepository {

        public NamespaceInfoLookup(XStreamPersister codec) {
            super(codec);
        }

        private String defaultNamespaceId;

        @Override
        public void setDefaultNamespace(NamespaceInfo namespace) {
            this.defaultNamespaceId = findById(namespace.getId(), NamespaceInfo.class)
                    .map(NamespaceInfo::getId)
                    .orElseThrow(NoSuchElementException::new);
        }

        @Override
        public Optional<NamespaceInfo> getDefaultNamespace() {
            return defaultNamespaceId == null ? Optional.empty() : findById(defaultNamespaceId, NamespaceInfo.class);
        }

        @Override
        public Optional<NamespaceInfo> findOneByURI(String uri) {
            return findFirst(NamespaceInfo.class, ns -> uri.equals(ns.getURI()));
        }

        @Override
        public Stream<NamespaceInfo> findAllByURI(String uri) {
            return all().filter(ns -> ns.getURI().equals(uri));
        }

        @Override
        public void unsetDefaultNamespace() {
            defaultNamespaceId = null;
        }

        @Override
        public Class<NamespaceInfo> getContentType() {
            return NamespaceInfo.class;
        }

        @Override
        public <U extends NamespaceInfo> Optional<U> findFirstByName(String name, Class<U> clazz) {
            return allOf(clazz).filter(ns -> ns.getPrefix().equals(name)).findFirst();
        }
    }

    public static class WorkspaceInfoLookup extends XmlCatalogInfoLookup<WorkspaceInfo> implements WorkspaceRepository {

        public WorkspaceInfoLookup(XStreamPersister codec) {
            super(codec);
        }

        private WorkspaceInfo defaultWorkspace;

        @Override
        public void setDefaultWorkspace(WorkspaceInfo workspace) {
            this.defaultWorkspace =
                    findById(workspace.getId(), WorkspaceInfo.class).orElseThrow(NoSuchElementException::new);
        }

        @Override
        public Optional<WorkspaceInfo> getDefaultWorkspace() {
            return Optional.ofNullable(defaultWorkspace);
        }

        @Override
        public void unsetDefaultWorkspace() {
            defaultWorkspace = null;
        }

        @Override
        public Class<WorkspaceInfo> getContentType() {
            return WorkspaceInfo.class;
        }

        @Override
        public <U extends WorkspaceInfo> Optional<U> findFirstByName(String name, Class<U> clazz) {
            return all().map(clazz::cast).filter(w -> name.equals(w.getName())).findFirst();
        }
    }

    public static class StoreInfoLookup extends XmlCatalogInfoLookup<StoreInfo> implements StoreRepository {

        public StoreInfoLookup(XStreamPersister codec) {
            super(codec);
        }

        /** The default store id keyed by workspace id */
        protected ConcurrentMap<String, String> defaultStores = new ConcurrentHashMap<>();

        @Override
        public void setDefaultDataStore(WorkspaceInfo workspace, DataStoreInfo store) {
            String wsId = workspace.getId();
            final DataStoreInfo localStore =
                    super.findById(store.getId(), DataStoreInfo.class).orElseThrow(NoSuchElementException::new);
            defaultStores.put(wsId, localStore.getId());
        }

        @Override
        public void unsetDefaultDataStore(WorkspaceInfo workspace) {
            defaultStores.remove(workspace.getId());
        }

        @Override
        public Optional<DataStoreInfo> getDefaultDataStore(WorkspaceInfo workspace) {
            String storeId = defaultStores.get(workspace.getId());
            return storeId == null ? Optional.empty() : findById(storeId, DataStoreInfo.class);
        }

        @Override
        public Stream<DataStoreInfo> getDefaultDataStores() {
            return defaultStores.values().stream().map(s -> deserialize(s, DataStoreInfo.class));
        }

        @Override
        public void dispose() {
            super.dispose();
            defaultStores.clear();
        }

        @Override
        public <T extends StoreInfo> Stream<T> findAllByWorkspace(WorkspaceInfo workspace, Class<T> clazz) {

            return all().filter(clazz::isInstance)
                    .map(clazz::cast)
                    .filter(s -> s.getWorkspace().getId().equals(workspace.getId()));
        }

        @Override
        public <T extends StoreInfo> Stream<T> findAllByType(Class<T> clazz) {
            return all().filter(clazz::isInstance).map(clazz::cast);
        }

        @Override
        public <T extends StoreInfo> Optional<T> findByNameAndWorkspace(
                String name, WorkspaceInfo workspace, Class<T> clazz) {

            return findAllByWorkspace(workspace, clazz)
                    .filter(s -> s.getName().equals(name))
                    .findFirst();
        }

        @Override
        public Class<StoreInfo> getContentType() {
            return StoreInfo.class;
        }

        @Override
        public <U extends StoreInfo> Optional<U> findFirstByName(String name, Class<U> clazz) {
            return all().filter(clazz::isInstance)
                    .map(clazz::cast)
                    .filter(s -> name.equals(s.getName()))
                    .findFirst();
        }
    }

    public static class LayerGroupInfoLookup extends XmlCatalogInfoLookup<LayerGroupInfo>
            implements LayerGroupRepository {

        public LayerGroupInfoLookup(XStreamPersister codec) {
            super(codec);
        }

        @Override
        public Stream<LayerGroupInfo> findAllByWorkspaceIsNull() {
            return all().filter(lg -> lg.getWorkspace() == null);
        }

        @Override
        public Stream<LayerGroupInfo> findAllByWorkspace(WorkspaceInfo workspace) {
            Predicate<LayerGroupInfo> predicate =
                    lg -> lg.getWorkspace() != null && lg.getWorkspace().getId().equals(workspace.getId());
            return all().filter(predicate);
        }

        @Override
        public Optional<LayerGroupInfo> findByNameAndWorkspaceIsNull(String name) {
            return findAllByWorkspaceIsNull()
                    .filter(lg -> lg.getName().equals(name))
                    .findFirst();
        }

        @Override
        public Optional<LayerGroupInfo> findByNameAndWorkspace(String name, WorkspaceInfo workspace) {
            return findAllByWorkspace(workspace)
                    .filter(lg -> lg.getName().equals(name))
                    .findFirst();
        }

        @Override
        public Class<LayerGroupInfo> getContentType() {
            return LayerGroupInfo.class;
        }

        @Override
        public <U extends LayerGroupInfo> Optional<U> findFirstByName(String name, Class<U> clazz) {
            return all().filter(clazz::isInstance)
                    .map(clazz::cast)
                    .filter(s -> name.equals(s.getName()))
                    .findFirst();
        }
    }

    public static class MapInfoLookup extends XmlCatalogInfoLookup<MapInfo> implements MapRepository {

        protected MapInfoLookup(XStreamPersister codec) {
            super(codec);
        }

        @Override
        public Class<MapInfo> getContentType() {
            return MapInfo.class;
        }

        @Override
        public <U extends MapInfo> Optional<U> findFirstByName(String name, Class<U> clazz) {
            return all().filter(clazz::isInstance)
                    .map(clazz::cast)
                    .filter(s -> name.equals(s.getName()))
                    .findFirst();
        }
    }

    public static class ResourceInfoLookup extends XmlCatalogInfoLookup<ResourceInfo> implements ResourceRepository {

        protected ResourceInfoLookup(XStreamPersister codec) {
            super(codec);
        }

        @Override
        public <T extends ResourceInfo> Stream<T> findAllByType(Class<T> clazz) {
            return allOf(clazz);
        }

        @Override
        public <T extends ResourceInfo> Stream<T> findAllByNamespace(NamespaceInfo ns, Class<T> clazz) {

            return allOf(clazz).filter(r -> ns.getId().equals(r.getNamespace().getId()));
        }

        @Override
        public <T extends ResourceInfo> Optional<T> findByStoreAndName(StoreInfo store, String name, Class<T> clazz) {

            return findAllByStore(store, clazz)
                    .filter(r -> name.equals(r.getName()))
                    .findFirst();
        }

        @Override
        public <T extends ResourceInfo> Stream<T> findAllByStore(StoreInfo store, Class<T> clazz) {

            return allOf(clazz).filter(r -> store.getId().equals(r.getStore().getId()));
        }

        @Override
        public <T extends ResourceInfo> Optional<T> findByNameAndNamespace(
                String name, NamespaceInfo namespace, Class<T> clazz) {

            return findAllByNamespace(namespace, clazz)
                    .filter(s -> s.getName().equals(name))
                    .findFirst();
        }

        @Override
        public Class<ResourceInfo> getContentType() {
            return ResourceInfo.class;
        }

        @Override
        public <U extends ResourceInfo> Optional<U> findFirstByName(String name, Class<U> clazz) {
            return all().filter(clazz::isInstance)
                    .map(clazz::cast)
                    .filter(s -> name.equals(s.getName()))
                    .findFirst();
        }
    }

    public static class LayerInfoLookup extends XmlCatalogInfoLookup<LayerInfo> implements LayerRepository {

        public LayerInfoLookup(XStreamPersister codec) {
            super(codec);
        }

        @Override
        public Optional<LayerInfo> findOneByName(String name) {
            return findFirst(LayerInfo.class, li -> name.equals(li.getName()));
        }

        @Override
        public Stream<LayerInfo> findAllByDefaultStyleOrStyles(StyleInfo style) {
            String id = style.getId();
            Predicate<? super LayerInfo> predicate = li -> (li.getDefaultStyle() != null
                            && id.equals(li.getDefaultStyle().getId()))
                    || li.getStyles().stream().map(Info::getId).anyMatch(id::equals);
            return all().filter(predicate);
        }

        @Override
        public Stream<LayerInfo> findAllByResource(ResourceInfo resource) {
            return all().filter(l -> l.getResource().getId().equals(resource.getId()));
        }

        public Class<LayerInfo> getContentType() {
            return LayerInfo.class;
        }

        @Override
        public <U extends LayerInfo> Optional<U> findFirstByName(String name, Class<U> clazz) {
            return all().filter(clazz::isInstance)
                    .map(clazz::cast)
                    .filter(s -> name.equals(s.getName()))
                    .findFirst();
        }
    }

    public static class StyleInfoLookup extends XmlCatalogInfoLookup<StyleInfo> implements StyleRepository {

        public StyleInfoLookup(XStreamPersister codec) {
            super(codec);
        }

        @Override
        public Stream<StyleInfo> findAllByNullWorkspace() {
            return all().filter(s -> s.getWorkspace() == null);
        }

        @Override
        public Stream<StyleInfo> findAllByWorkspace(WorkspaceInfo ws) {
            return all().filter(s -> s.getWorkspace() != null)
                    .filter(s -> s.getWorkspace().getId().equals(ws.getId()));
        }

        @Override
        public Optional<StyleInfo> findByNameAndWordkspaceNull(String name) {
            return findAllByNullWorkspace()
                    .filter(s -> s.getName().equals(name))
                    .findFirst();
        }

        @Override
        public Optional<StyleInfo> findByNameAndWorkspace(String name, WorkspaceInfo workspace) {

            return findAllByWorkspace(workspace)
                    .filter(s -> s.getName().equals(name))
                    .findFirst();
        }

        @Override
        public Class<StyleInfo> getContentType() {
            return StyleInfo.class;
        }

        @Override
        public <U extends StyleInfo> Optional<U> findFirstByName(String name, Class<U> clazz) {
            return all().filter(clazz::isInstance)
                    .map(clazz::cast)
                    .filter(s -> name.equals(s.getName()))
                    .findFirst();
        }
    }
}
