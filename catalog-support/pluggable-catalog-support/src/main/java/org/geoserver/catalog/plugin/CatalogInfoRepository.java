/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin;

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

public interface CatalogInfoRepository<T extends CatalogInfo> {

    void add(@NonNull T value);

    void remove(@NonNull T value);

    T update(@NonNull T value, @NonNull Patch patch);

    void dispose();

    Stream<T> findAll();

    Stream<T> findAll(Filter filter);

    <U extends T> Stream<U> findAll(Filter filter, Class<U> infoType);

    /** Looks up a CatalogInfo by class and identifier */
    @Nullable
    <U extends T> U findById(@NonNull String id, @Nullable Class<U> clazz);

    /**
     * Looks up a CatalogInfo by class and name
     *
     * @param name
     * @return the first match found based on {@code name}, or {@code null}
     */
    @Nullable
    <U extends T> U findFirstByName(@NonNull String name, Class<U> clazz);

    // revisit: some sort of progress listener/cancel flag would be nice
    void syncTo(@NonNull CatalogInfoRepository<T> target);

    public interface NamespaceRepository extends CatalogInfoRepository<NamespaceInfo> {
        void setDefaultNamespace(@NonNull NamespaceInfo namespace);

        @Nullable
        NamespaceInfo getDefaultNamespace();

        @Nullable
        NamespaceInfo findOneByURI(@NonNull String uri);

        Stream<NamespaceInfo> findAllByURI(@NonNull String uri);
    }

    public interface WorkspaceRepository extends CatalogInfoRepository<WorkspaceInfo> {
        void setDefaultWorkspace(@NonNull WorkspaceInfo workspace);

        @Nullable
        WorkspaceInfo getDefaultWorkspace();
    }

    public interface StoreRepository extends CatalogInfoRepository<StoreInfo> {
        void setDefaultDataStore(WorkspaceInfo workspace, DataStoreInfo dataStore);

        @Nullable
        DataStoreInfo getDefaultDataStore(@NonNull WorkspaceInfo workspace);

        Stream<DataStoreInfo> getDefaultDataStores();

        <T extends StoreInfo> Stream<T> findAllByWorkspace(
                @NonNull WorkspaceInfo workspace, @Nullable Class<T> clazz);

        <T extends StoreInfo> Stream<T> findAllByType(@Nullable Class<T> clazz);

        <T extends StoreInfo> T findByNameAndWorkspace(
                String name, WorkspaceInfo workspace, Class<T> clazz);
    }

    public interface ResourceRepository extends CatalogInfoRepository<ResourceInfo> {

        @Nullable
        <T extends ResourceInfo> T findByNameAndNamespace(
                @NonNull String name, @NonNull NamespaceInfo namespace, @Nullable Class<T> clazz);

        <T extends ResourceInfo> Stream<T> findAllByType(@Nullable Class<T> clazz);

        <T extends ResourceInfo> Stream<T> findAllByNamespace(
                @NonNull NamespaceInfo ns, @Nullable Class<T> clazz);

        @Nullable
        <T extends ResourceInfo> T findByStoreAndName(
                @NonNull StoreInfo store, @NonNull String name, @Nullable Class<T> clazz);

        <T extends ResourceInfo> Stream<T> findAllByStore(StoreInfo store, Class<T> clazz);
    }

    public interface LayerRepository extends CatalogInfoRepository<LayerInfo> {

        @Nullable
        LayerInfo findOneByName(@NonNull String possiblyPrefixedName);

        Stream<LayerInfo> findAllByDefaultStyleOrStyles(@NonNull StyleInfo style);

        Stream<LayerInfo> findAllByResource(@NonNull ResourceInfo resource);
    }

    public interface LayerGroupRepository extends CatalogInfoRepository<LayerGroupInfo> {

        @Nullable
        LayerGroupInfo findByNameAndWorkspaceIsNull(@NonNull String name);

        LayerGroupInfo findByNameAndWorkspace(String name, WorkspaceInfo workspace);

        Stream<LayerGroupInfo> findAllByWorkspaceIsNull();

        Stream<LayerGroupInfo> findAllByWorkspace(WorkspaceInfo workspace);
    }

    public interface StyleRepository extends CatalogInfoRepository<StyleInfo> {

        Stream<StyleInfo> findAllByNullWorkspace();

        Stream<StyleInfo> findAllByWorkspace(@NonNull WorkspaceInfo ws);

        StyleInfo findByNameAndWordkspaceNull(String name);

        StyleInfo findByNameAndWordkspace(String name, WorkspaceInfo workspace);
    }

    public interface MapRepository extends CatalogInfoRepository<MapInfo> {}
}
