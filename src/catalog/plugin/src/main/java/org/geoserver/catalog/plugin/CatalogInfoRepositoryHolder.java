/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin;

import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.plugin.CatalogInfoRepository.LayerGroupRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.LayerRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.MapRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.NamespaceRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.ResourceRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.StoreRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.StyleRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.WorkspaceRepository;

/**
 * Interface for managing and accessing specialized {@link CatalogInfoRepository} instances in GeoServer Cloud.
 *
 * <p>This interface acts as a central holder or registry for type-specific catalog repositories, providing
 * uniform access to repositories for different {@link CatalogInfo} subtypes (e.g., workspaces, layers,
 * styles). It enables retrieval of repositories either by catalog info type or instance, and supports
 * setting and getting each repository explicitly. This design facilitates a modular, repository-based
 * approach to catalog data access, decoupling storage operations from higher-level catalog logic.
 *
 * <p>Key features:
 * <ul>
 *   <li>Generic retrieval methods ({@link #repository(Class)}, {@link #repositoryFor(CatalogInfo)}) for
 *       type-safe access to repositories.</li>
 *   <li>Specific getter and setter methods for each repository type (e.g., {@link #getLayerRepository()},
 *       {@link #setStyleRepository(StyleRepository)}).</li>
 *   <li>Support for all core catalog info types defined in {@link CatalogInfoRepository} sub-interfaces.</li>
 * </ul>
 *
 * <p>Implementations of this interface are expected to maintain a consistent mapping between catalog info
 * types and their corresponding repositories, throwing exceptions if a requested repository is unavailable
 * or misconfigured.
 *
 * @since 1.0
 * @see CatalogInfoRepository
 * @see CatalogInfo
 */
public interface CatalogInfoRepositoryHolder {

    /**
     * Retrieves the repository responsible for managing objects of the specified {@link CatalogInfo} type.
     *
     * <p>This generic method returns a type-specific repository based on the provided class (e.g.,
     * {@link WorkspaceInfo.class} returns a {@link WorkspaceRepository}). It enables dynamic access to
     * repositories without requiring explicit casting in most cases.
     *
     * @param <T> The type of {@link CatalogInfo} to query (e.g., {@link LayerInfo}).
     * @param <R> The corresponding repository type (e.g., {@link LayerRepository}).
     * @param of  The class of catalog info objects to retrieve the repository for; must not be null.
     * @return The repository managing objects of type {@code T}; never null.
     * @throws NullPointerException if {@code of} is null.
     * @throws IllegalArgumentException if no repository is configured for the specified type.
     * @example Retrieving a layer repository:
     *          <pre>
     *          LayerRepository layerRepo = holder.repository(LayerInfo.class);
     *          </pre>
     */
    <T extends CatalogInfo, R extends CatalogInfoRepository<T>> R repository(Class<T> of);

    /**
     * Retrieves the repository responsible for managing the type of the provided {@link CatalogInfo} instance.
     *
     * <p>This method infers the repository type from the given object (e.g., passing a {@link LayerInfo}
     * returns a {@link LayerRepository}). It provides a convenient alternative to {@link #repository(Class)}
     * when an instance is available.
     *
     * @param <T>  The type of {@link CatalogInfo} (e.g., {@link StyleInfo}).
     * @param <R>  The corresponding repository type (e.g., {@link StyleRepository}).
     * @param info The catalog info object whose type determines the repository; must not be null.
     * @return The repository managing objects of the same type as {@code info}; never null.
     * @throws NullPointerException if {@code info} is null.
     * @throws IllegalArgumentException if no repository is configured for the object’s type.
     * @example Retrieving a repository for a specific style:
     *          <pre>
     *          StyleInfo style = ...; // existing style
     *          StyleRepository styleRepo = holder.repositoryFor(style);
     *          </pre>
     */
    <T extends CatalogInfo, R extends CatalogInfoRepository<T>> R repositoryFor(T info);

    /**
     * Sets the repository for managing {@link NamespaceInfo} objects.
     *
     * @param namespaces The {@link NamespaceRepository} to set; must not be null.
     * @throws NullPointerException if {@code namespaces} is null.
     * @example Setting a namespace repository:
     *          <pre>
     *          NamespaceRepository nsRepo = new DefaultNamespaceRepository();
     *          holder.setNamespaceRepository(nsRepo);
     *          </pre>
     */
    void setNamespaceRepository(NamespaceRepository namespaces);

    /**
     * Retrieves the repository for managing {@link NamespaceInfo} objects.
     *
     * @return The configured {@link NamespaceRepository}; never null.
     * @throws IllegalStateException if no namespace repository has been set.
     */
    NamespaceRepository getNamespaceRepository();

    /**
     * Sets the repository for managing {@link WorkspaceInfo} objects.
     *
     * @param workspaces The {@link WorkspaceRepository} to set; must not be null.
     * @throws NullPointerException if {@code workspaces} is null.
     */
    void setWorkspaceRepository(WorkspaceRepository workspaces);

    /**
     * Retrieves the repository for managing {@link WorkspaceInfo} objects.
     *
     * @return The configured {@link WorkspaceRepository}; never null.
     * @throws IllegalStateException if no workspace repository has been set.
     */
    WorkspaceRepository getWorkspaceRepository();

    /**
     * Sets the repository for managing {@link StoreInfo} objects.
     *
     * @param stores The {@link StoreRepository} to set; must not be null.
     * @throws NullPointerException if {@code stores} is null.
     */
    void setStoreRepository(StoreRepository stores);

    /**
     * Retrieves the repository for managing {@link StoreInfo} objects.
     *
     * @return The configured {@link StoreRepository}; never null.
     * @throws IllegalStateException if no store repository has been set.
     */
    StoreRepository getStoreRepository();

    /**
     * Sets the repository for managing {@link ResourceInfo} objects.
     *
     * @param resources The {@link ResourceRepository} to set; must not be null.
     * @throws NullPointerException if {@code resources} is null.
     */
    void setResourceRepository(ResourceRepository resources);

    /**
     * Retrieves the repository for managing {@link ResourceInfo} objects.
     *
     * @return The configured {@link ResourceRepository}; never null.
     * @throws IllegalStateException if no resource repository has been set.
     */
    ResourceRepository getResourceRepository();

    /**
     * Sets the repository for managing {@link LayerInfo} objects.
     *
     * @param layers The {@link LayerRepository} to set; must not be null.
     * @throws NullPointerException if {@code layers} is null.
     */
    void setLayerRepository(LayerRepository layers);

    /**
     * Retrieves the repository for managing {@link LayerInfo} objects.
     *
     * @return The configured {@link LayerRepository}; never null.
     * @throws IllegalStateException if no layer repository has been set.
     */
    LayerRepository getLayerRepository();

    /**
     * Sets the repository for managing {@link LayerGroupInfo} objects.
     *
     * @param layerGroups The {@link LayerGroupRepository} to set; must not be null.
     * @throws NullPointerException if {@code layerGroups} is null.
     */
    void setLayerGroupRepository(LayerGroupRepository layerGroups);

    /**
     * Retrieves the repository for managing {@link LayerGroupInfo} objects.
     *
     * @return The configured {@link LayerGroupRepository}; never null.
     * @throws IllegalStateException if no layer group repository has been set.
     */
    LayerGroupRepository getLayerGroupRepository();

    /**
     * Sets the repository for managing {@link StyleInfo} objects.
     *
     * @param styles The {@link StyleRepository} to set; must not be null.
     * @throws NullPointerException if {@code styles} is null.
     */
    void setStyleRepository(StyleRepository styles);

    /**
     * Retrieves the repository for managing {@link StyleInfo} objects.
     *
     * @return The configured {@link StyleRepository}; never null.
     * @throws IllegalStateException if no style repository has been set.
     */
    StyleRepository getStyleRepository();

    /**
     * Sets the repository for managing {@link MapInfo} objects.
     *
     * @param maps The {@link MapRepository} to set; must not be null.
     * @throws NullPointerException if {@code maps} is null.
     */
    void setMapRepository(MapRepository maps);

    /**
     * Retrieves the repository for managing {@link MapInfo} objects.
     *
     * @return The configured {@link MapRepository}; never null.
     * @throws IllegalStateException if no map repository has been set.
     */
    MapRepository getMapRepository();
}
