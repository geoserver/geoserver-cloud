/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.plugin;

import java.util.List;
import javax.annotation.Nullable;
import org.geoserver.catalog.Catalog;
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
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;

public interface CatalogInfoRepository<T extends CatalogInfo> {

    void setCatalog(@Nullable Catalog catalog);

    void add(T value);

    void remove(T value);

    void update(T value);

    void dispose();

    List<T> findAll();

    <U extends T> List<U> findAll(Filter filter);

    <U extends T> List<U> findAll(Filter filter, Class<U> infoType);

    /** Looks up a CatalogInfo by class and identifier */
    <U extends T> U findById(String id, Class<U> clazz);

    /** Looks up a CatalogInfo by class and name */
    <U extends T> U findByName(Name name, Class<U> clazz);

    void syncTo(CatalogInfoRepository<T> target);

    public interface NamespaceRepository extends CatalogInfoRepository<NamespaceInfo> {
        void setDefaultNamespace(NamespaceInfo namespace);

        NamespaceInfo getDefaultNamespace();

        NamespaceInfo findOneByURI(String uri);

        List<NamespaceInfo> findAllByURI(String uri);
    }

    public interface WorkspaceRepository extends CatalogInfoRepository<WorkspaceInfo> {
        void setDefaultWorkspace(WorkspaceInfo workspace);

        WorkspaceInfo getDefaultWorkspace();
    }

    public interface StoreRepository extends CatalogInfoRepository<StoreInfo> {
        void setDefaultDataStore(WorkspaceInfo workspace, DataStoreInfo dataStore);

        DataStoreInfo getDefaultDataStore(WorkspaceInfo workspace);

        List<DataStoreInfo> getDefaultDataStores();

        <T extends StoreInfo> T findOneByName(String name, Class<T> clazz);

        <T extends StoreInfo> List<T> findAllByWorkspace(WorkspaceInfo workspace, Class<T> clazz);

        <T extends StoreInfo> List<T> findAllByType(Class<T> clazz);
    }

    public interface ResourceRepository extends CatalogInfoRepository<ResourceInfo> {

        <T extends ResourceInfo> T findOneByName(String name, Class<T> clazz);

        <T extends ResourceInfo> List<T> findAllByType(Class<T> clazz);

        <T extends ResourceInfo> List<T> findAllByNamespace(NamespaceInfo ns, Class<T> clazz);

        <T extends ResourceInfo> T findByStoreAndName(StoreInfo store, String name, Class<T> clazz);

        <T extends ResourceInfo> List<T> findAllByStore(StoreInfo store, Class<T> clazz);
    }

    public interface LayerRepository extends CatalogInfoRepository<LayerInfo> {

        LayerInfo findOneByName(String name);

        List<LayerInfo> findAllByDefaultStyleOrStyles(StyleInfo style);

        List<LayerInfo> findAllByResource(ResourceInfo resource);
    }

    public interface LayerGroupRepository extends CatalogInfoRepository<LayerGroupInfo> {

        LayerGroupInfo findOneByName(String name);

        List<LayerGroupInfo> findAllByWorkspaceIsNull();

        List<LayerGroupInfo> findAllByWorkspace(WorkspaceInfo workspace);
    }

    public interface StyleRepository extends CatalogInfoRepository<StyleInfo> {

        StyleInfo findOneByName(String name);

        List<StyleInfo> findAllByNullWorkspace();

        List<StyleInfo> findAllByWorkspace(WorkspaceInfo ws);
    }

    public interface MapRepository extends CatalogInfoRepository<MapInfo> {}
}
