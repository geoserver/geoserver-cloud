/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin;

import java.util.Optional;
import java.util.stream.Stream;
import lombok.NonNull;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MapInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.opengis.filter.Filter;
import org.springframework.lang.Nullable;

/**
 * Raw data access API for {@link CatalogInfo} back-end implementations.
 *
 * <p>This is a null-free, DDD inspired "repository" API to provide storage and querying of plain
 * {@link CatalogInfo} objects, with precise semantics. No method that receives an argument can
 * receive {@code null}. In cases where different semantics shall be applied, each specialization
 * provides alternative, semantically clear query methods. For example, {@link
 * StyleRepository#findAllByNullWorkspace()} and {@link
 * StyleRepository#findAllByWorkspace(WorkspaceInfo)} are self-explanatory, no magic is involved nor
 * decision making, which are handled at a higer level of abstraction ({@code CatalogFacade} and/or
 * {@code Catalog}).
 *
 * <p>All query methods that could return zero or one result, return {@link Optional}. All query
 * methods that could return zero or more results, return {@link Stream}.
 *
 * <p>Care shall be taken that {@code Stream} implements {@link AutoCloseable} and hence it is
 * expected for users of this api to properly close the received stream, may the implementation be
 * using live connections to some back-end storage and need to release resources.
 *
 * @see WorkspaceRepository
 * @see NamespaceRepository
 * @see StoreRepository
 * @see ResourceRepository
 * @see LayerRepository
 * @see LayerGroupRepository
 * @see StyleRepository
 * @see MapRepository
 * @param <T>
 */
public interface CatalogInfoRepository<T extends CatalogInfo> {

    /**
     * @return the less concrete {@link CatalogInfo} type this repository handles (e.g. if {@link
     *     ResourceInfo}, then this repository handles all {@code ResourceInfo} subtypes)
     */
    Class<T> getContentType();

    void add(@NonNull T value);

    void remove(@NonNull T value);

    /**
     * Applies the provided {@link Patch patch} to this repository's copy of the provided {@code
     * value} object and returns the "patched" object.
     *
     * <p>In contrast to a typical {@code save(I value)} method, the patch helps implementors to
     * apply MVCC (Multi-Version Concurrency Control) semantics to the operation. It provides the
     * property names and values that need to be changed, and hence the implementation can choose
     * the best mechanism to apply those specific changes without overriding the whole object. For
     * example, it can be translated to an SQL {@code UPDATE} with only the required column changes,
     * or improve concurrency on an in-memory store by only having to lock the live-object while the
     * patch is applied.
     *
     * <p>The provided value shall not be modified by this method. Whether the provided value and
     * the updated repository copy refer to the same object instance is up to the implementation,
     * but they're considered different from the point of view of the API.
     */
    <I extends T> I update(@NonNull I value, @NonNull Patch patch);

    void dispose();

    default Stream<T> findAll() {
        return findAll(Query.all(getContentType()));
    }

    /**
     * Returns all objects in this repository that satisfy the query criteria (type and filter),
     * additional restrictions such as paging and order.
     *
     * @see Query
     */
    <U extends T> Stream<U> findAll(Query<U> query);

    <U extends T> long count(Class<U> of, Filter filter);

    /** Looks up a CatalogInfo by class and identifier */
    <U extends T> Optional<U> findById(@NonNull String id, @Nullable Class<U> clazz);

    /**
     * Looks up a CatalogInfo by class and name
     *
     * @param name
     * @return the first match found based on {@code name}, or {@code null}
     */
    <U extends T> Optional<U> findFirstByName(@NonNull String name, Class<U> clazz);

    public boolean canSortBy(@NonNull String propertyName);

    // revisit: some sort of progress listener/cancel flag would be nice
    void syncTo(@NonNull CatalogInfoRepository<T> target);

    public interface NamespaceRepository extends CatalogInfoRepository<NamespaceInfo> {
        /** Establishes {@code namespace} as the {@link #getDefaultNamespace() default} on */
        void setDefaultNamespace(@NonNull NamespaceInfo namespace);

        /** Unlinks the current default namespace, leaving no default */
        void unsetDefaultNamespace();

        Optional<NamespaceInfo> getDefaultNamespace();

        Optional<NamespaceInfo> findOneByURI(@NonNull String uri);

        Stream<NamespaceInfo> findAllByURI(@NonNull String uri);
    }

    public interface WorkspaceRepository extends CatalogInfoRepository<WorkspaceInfo> {
        /** Unlinks the current default workspace, leaving no default */
        void unsetDefaultWorkspace();

        /** Establishes {@code workspace} as the {@link #getDefaultWorkspace() default} on */
        void setDefaultWorkspace(@NonNull WorkspaceInfo workspace);

        Optional<WorkspaceInfo> getDefaultWorkspace();
    }

    public interface StoreRepository extends CatalogInfoRepository<StoreInfo> {

        void setDefaultDataStore(
                @NonNull WorkspaceInfo workspace, @NonNull DataStoreInfo dataStore);

        void unsetDefaultDataStore(@NonNull WorkspaceInfo workspace);

        Optional<DataStoreInfo> getDefaultDataStore(@NonNull WorkspaceInfo workspace);

        Stream<DataStoreInfo> getDefaultDataStores();

        <T extends StoreInfo> Stream<T> findAllByWorkspace(
                @NonNull WorkspaceInfo workspace, @NonNull Class<T> clazz);

        <T extends StoreInfo> Stream<T> findAllByType(@NonNull Class<T> clazz);

        <T extends StoreInfo> Optional<T> findByNameAndWorkspace(
                @NonNull String name, @NonNull WorkspaceInfo workspace, @NonNull Class<T> clazz);
    }

    public interface ResourceRepository extends CatalogInfoRepository<ResourceInfo> {

        <T extends ResourceInfo> Optional<T> findByNameAndNamespace(
                @NonNull String name, @NonNull NamespaceInfo namespace, @NonNull Class<T> clazz);

        <T extends ResourceInfo> Stream<T> findAllByType(@NonNull Class<T> clazz);

        <T extends ResourceInfo> Stream<T> findAllByNamespace(
                @NonNull NamespaceInfo ns, @NonNull Class<T> clazz);

        <T extends ResourceInfo> Optional<T> findByStoreAndName(
                @NonNull StoreInfo store, @NonNull String name, @NonNull Class<T> clazz);

        <T extends ResourceInfo> Stream<T> findAllByStore(StoreInfo store, Class<T> clazz);
    }

    public interface LayerRepository extends CatalogInfoRepository<LayerInfo> {

        Optional<LayerInfo> findOneByName(@NonNull String possiblyPrefixedName);

        Stream<LayerInfo> findAllByDefaultStyleOrStyles(@NonNull StyleInfo style);

        Stream<LayerInfo> findAllByResource(@NonNull ResourceInfo resource);
    }

    public interface LayerGroupRepository extends CatalogInfoRepository<LayerGroupInfo> {

        Optional<LayerGroupInfo> findByNameAndWorkspaceIsNull(@NonNull String name);

        Optional<LayerGroupInfo> findByNameAndWorkspace(
                @NonNull String name, @NonNull WorkspaceInfo workspace);

        Stream<LayerGroupInfo> findAllByWorkspaceIsNull();

        Stream<LayerGroupInfo> findAllByWorkspace(WorkspaceInfo workspace);
    }

    public interface StyleRepository extends CatalogInfoRepository<StyleInfo> {

        Stream<StyleInfo> findAllByNullWorkspace();

        Stream<StyleInfo> findAllByWorkspace(@NonNull WorkspaceInfo ws);

        Optional<StyleInfo> findByNameAndWordkspaceNull(@NonNull String name);

        Optional<StyleInfo> findByNameAndWordkspace(
                @NonNull String name, @NonNull WorkspaceInfo workspace);
    }

    public interface MapRepository extends CatalogInfoRepository<MapInfo> {}
}
