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
import org.springframework.lang.Nullable;

public interface CatalogInfoRepository<T extends CatalogInfo> {

    Class<T> getContentType();

    void add(@NonNull T value);

    void remove(@NonNull T value);

    <I extends T> I update(@NonNull I value, @NonNull Patch patch);

    void dispose();

    default Stream<T> findAll() {
        return findAll(Query.all(getContentType()));
    }

    <U extends T> Stream<U> findAll(Query<U> query);

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
