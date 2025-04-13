/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.catalog.plugin;

import java.util.List;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MapInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.plugin.CatalogInfoRepository.LayerGroupRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.LayerRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.MapRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.NamespaceRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.ResourceRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.StoreRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.StyleRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.WorkspaceRepository;

/**
 * A concrete implementation of {@link CatalogInfoRepositoryHolder} that manages a registry of type-specific
 * catalog repositories in GeoServer Cloud.
 *
 * <p>This class maintains a collection of {@link CatalogInfoRepository} instances, each responsible for a
 * specific type of {@link CatalogInfo} (e.g., {@link WorkspaceInfo}, {@link LayerInfo}), using a
 * {@link CatalogInfoTypeRegistry} for type-safe mapping and retrieval. It provides methods to set and get
 * repositories for all core catalog info types, ensuring that catalog operations can access the appropriate
 * persistence layer. The implementation supports hierarchical types (e.g., {@link StoreInfo} and its
 * subtypes) through recursive registration where applicable.
 *
 * <p>Key features:
 * <ul>
 *   <li><strong>Type-Safe Registry:</strong> Maps {@link CatalogInfo} types to their repositories using
 *       {@link CatalogInfoTypeRegistry}.</li>
 *   <li><strong>Comprehensive Coverage:</strong> Handles all standard catalog info types via dedicated
 *       setters and getters.</li>
 *   <li><strong>Resource Management:</strong> Provides a {@link #dispose()} method to clean up repository
 *       resources.</li>
 * </ul>
 *
 * <p>This class is typically used within a {@link RepositoryCatalogFacade} to coordinate repository access
 * for catalog operations in GeoServer Cloud.
 *
 * @since 1.0
 * @see CatalogInfoRepositoryHolder
 * @see CatalogInfoRepository
 * @see CatalogInfoTypeRegistry
 */
public class CatalogInfoRepositoryHolderImpl implements CatalogInfoRepositoryHolder {

    protected NamespaceRepository namespaces;
    protected WorkspaceRepository workspaces;
    protected StoreRepository stores;
    protected ResourceRepository resources;
    protected StyleRepository styles;
    protected MapRepository maps;

    protected LayerRepository layers;
    protected LayerGroupRepository layerGroups;

    protected CatalogInfoTypeRegistry<CatalogInfoRepository<?>> repos = new CatalogInfoTypeRegistry<>();

    /**
     * Retrieves the repository for a specific {@link CatalogInfo} type from the internal registry.
     *
     * <p>This method uses the {@link CatalogInfoTypeRegistry} to map the provided type to its corresponding
     * repository (e.g., {@link LayerInfo.class} to {@link LayerRepository}). It ensures type safety through
     * generics, suppressing unchecked warnings due to the registry’s type erasure handling.
     *
     * @param <T> The type of {@link CatalogInfo} to query (e.g., {@link WorkspaceInfo}).
     * @param <R> The corresponding repository type (e.g., {@link WorkspaceRepository}).
     * @param of  The class of catalog info objects to retrieve the repository for; must not be null.
     * @return The repository managing objects of type {@code T}; never null.
     * @throws NullPointerException if {@code of} is null.
     * @throws IllegalArgumentException if no repository is configured for the specified type.
     * @example Retrieving a layer repository:
     *          <pre>
     *          LayerRepository layerRepo = holder.repository(LayerInfo.class);
     *          </pre>
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T extends CatalogInfo, R extends CatalogInfoRepository<T>> R repository(Class<T> of) {
        return (R) repos.of(of);
    }

    /**
     * Retrieves the repository for the type of a given {@link CatalogInfo} instance.
     *
     * <p>This method determines the repository based on the object’s runtime type (e.g., a {@link StyleInfo}
     * instance returns a {@link StyleRepository}), using the {@link CatalogInfoTypeRegistry} for mapping.
     * It suppresses unchecked warnings due to type erasure in the registry.
     *
     * @param <T>  The type of {@link CatalogInfo} (e.g., {@link StyleInfo}).
     * @param <R>  The corresponding repository type (e.g., {@link StyleRepository}).
     * @param info The catalog info object whose type determines the repository; must not be null.
     * @return The repository managing objects of the same type as {@code info}; never null.
     * @throws NullPointerException if {@code info} is null.
     * @throws IllegalArgumentException if no repository is configured for the object’s type.
     * @example Retrieving a repository for a specific style:
     *          <pre>
     *          StyleInfo style = new StyleInfoImpl();
     *          StyleRepository styleRepo = holder.repositoryFor(style);
     *          </pre>
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T extends CatalogInfo, R extends CatalogInfoRepository<T>> R repositoryFor(T info) {
        return (R) repos.forObject(info);
    }

    /**
     * Returns a list of all configured repositories.
     *
     * <p>This method provides access to all repositories registered in the internal
     * {@link CatalogInfoTypeRegistry}, useful for introspection or bulk operations. The wildcard return
     * type is intentionally suppressed as the list contains heterogeneous repository types.
     *
     * @return A list of all {@link CatalogInfoRepository} instances; never null.
     * @SuppressWarnings("java:S1452") Wildcard return type is intentional due to mixed repository types.
     */
    @SuppressWarnings("java:S1452")
    public List<CatalogInfoRepository<?>> all() {
        return repos.getAll();
    }

    /**
     * Sets the repository for managing {@link NamespaceInfo} objects.
     *
     * <p>Registers the provided {@link NamespaceRepository} in the internal type registry and assigns it
     * to the namespaces field.
     *
     * @param namespaces The {@link NamespaceRepository} to set; must not be null.
     * @throws NullPointerException if {@code namespaces} is null.
     * @example Setting a namespace repository:
     *          <pre>
     *          NamespaceRepository nsRepo = new DefaultNamespaceRepository();
     *          holder.setNamespaceRepository(nsRepo);
     *          </pre>
     */
    @Override
    public void setNamespaceRepository(NamespaceRepository namespaces) {
        this.namespaces = namespaces;
        repos.register(NamespaceInfo.class, namespaces);
    }

    /**
     * Retrieves the repository for managing {@link NamespaceInfo} objects.
     *
     * @return The configured {@link NamespaceRepository}; may be null if not set.
     */
    @Override
    public NamespaceRepository getNamespaceRepository() {
        return namespaces;
    }

    /**
     * Sets the repository for managing {@link WorkspaceInfo} objects.
     *
     * <p>Registers the provided {@link WorkspaceRepository} in the internal type registry and assigns it
     * to the workspaces field.
     *
     * @param workspaces The {@link WorkspaceRepository} to set; must not be null.
     * @throws NullPointerException if {@code workspaces} is null.
     */
    @Override
    public void setWorkspaceRepository(WorkspaceRepository workspaces) {
        this.workspaces = workspaces;
        repos.register(WorkspaceInfo.class, workspaces);
    }

    /**
     * Retrieves the repository for managing {@link WorkspaceInfo} objects.
     *
     * @return The configured {@link WorkspaceRepository}; may be null if not set.
     */
    @Override
    public WorkspaceRepository getWorkspaceRepository() {
        return workspaces;
    }

    /**
     * Sets the repository for managing {@link StoreInfo} objects and its subtypes.
     *
     * <p>Registers the provided {@link StoreRepository} recursively in the internal type registry to
     * support all {@link StoreInfo} subtypes (e.g., {@link DataStoreInfo}) and assigns it to the stores
     * field.
     *
     * @param stores The {@link StoreRepository} to set; must not be null.
     * @throws NullPointerException if {@code stores} is null.
     */
    @Override
    public void setStoreRepository(StoreRepository stores) {
        this.stores = stores;
        repos.registerRecursively(StoreInfo.class, stores);
    }

    /**
     * Retrieves the repository for managing {@link StoreInfo} objects.
     *
     * @return The configured {@link StoreRepository}; may be null if not set.
     */
    @Override
    public StoreRepository getStoreRepository() {
        return stores;
    }

    /**
     * Sets the repository for managing {@link ResourceInfo} objects and its subtypes.
     *
     * <p>Registers the provided {@link ResourceRepository} recursively in the internal type registry to
     * support all {@link ResourceInfo} subtypes (e.g., {@link org.geoserver.catalog.FeatureTypeInfo}) and
     * assigns it to the resources field.
     *
     * @param resources The {@link ResourceRepository} to set; must not be null.
     * @throws NullPointerException if {@code resources} is null.
     */
    @Override
    public void setResourceRepository(ResourceRepository resources) {
        this.resources = resources;
        repos.registerRecursively(ResourceInfo.class, resources);
    }

    /**
     * Retrieves the repository for managing {@link ResourceInfo} objects.
     *
     * @return The configured {@link ResourceRepository}; may be null if not set.
     */
    @Override
    public ResourceRepository getResourceRepository() {
        return resources;
    }

    /**
     * Sets the repository for managing {@link LayerInfo} objects.
     *
     * <p>Registers the provided {@link LayerRepository} in the internal type registry and assigns it to
     * the layers field.
     *
     * @param layers The {@link LayerRepository} to set; must not be null.
     * @throws NullPointerException if {@code layers} is null.
     */
    @Override
    public void setLayerRepository(LayerRepository layers) {
        this.layers = layers;
        repos.register(LayerInfo.class, layers);
    }

    /**
     * Retrieves the repository for managing {@link LayerInfo} objects.
     *
     * @return The configured {@link LayerRepository}; may be null if not set.
     */
    @Override
    public LayerRepository getLayerRepository() {
        return layers;
    }

    /**
     * Sets the repository for managing {@link LayerGroupInfo} objects.
     *
     * <p>Registers the provided {@link LayerGroupRepository} in the internal type registry and assigns it
     * to the layerGroups field.
     *
     * @param layerGroups The {@link LayerGroupRepository} to set; must not be null.
     * @throws NullPointerException if {@code layerGroups} is null.
     */
    @Override
    public void setLayerGroupRepository(LayerGroupRepository layerGroups) {
        this.layerGroups = layerGroups;
        repos.register(LayerGroupInfo.class, layerGroups);
    }

    /**
     * Retrieves the repository for managing {@link LayerGroupInfo} objects.
     *
     * @return The configured {@link LayerGroupRepository}; may be null if not set.
     */
    @Override
    public LayerGroupRepository getLayerGroupRepository() {
        return layerGroups;
    }

    /**
     * Sets the repository for managing {@link StyleInfo} objects.
     *
     * <p>Registers the provided {@link StyleRepository} in the internal type registry and assigns it to
     * the styles field.
     *
     * @param styles The {@link StyleRepository} to set; must not be null.
     * @throws NullPointerException if {@code styles} is null.
     */
    @Override
    public void setStyleRepository(StyleRepository styles) {
        this.styles = styles;
        repos.register(StyleInfo.class, styles);
    }

    /**
     * Retrieves the repository for managing {@link StyleInfo} objects.
     *
     * @return The configured {@link StyleRepository}; may be null if not set.
     */
    @Override
    public StyleRepository getStyleRepository() {
        return styles;
    }

    /**
     * Sets the repository for managing {@link MapInfo} objects.
     *
     * <p>Registers the provided {@link MapRepository} in the internal type registry and assigns it to the
     * maps field.
     *
     * @param maps The {@link MapRepository} to set; must not be null.
     * @throws NullPointerException if {@code maps} is null.
     */
    @Override
    public void setMapRepository(MapRepository maps) {
        this.maps = maps;
        repos.register(MapInfo.class, maps);
    }

    /**
     * Retrieves the repository for managing {@link MapInfo} objects.
     *
     * @return The configured {@link MapRepository}; may be null if not set.
     */
    @Override
    public MapRepository getMapRepository() {
        return maps;
    }

    /**
     * Disposes of all resources held by the configured repositories.
     *
     * <p>Iterates through all registered repositories and calls their {@code dispose()} methods to release
     * resources (e.g., database connections). Null repositories are skipped.
     */
    public void dispose() {
        dispose(stores);
        dispose(resources);
        dispose(namespaces);
        dispose(workspaces);
        dispose(layers);
        dispose(layerGroups);
        dispose(maps);
        dispose(styles);
    }

    /**
     * Disposes of a single repository’s resources if it exists.
     *
     * <p>Calls the {@code dispose()} method on the provided repository, safely handling null cases.
     *
     * @param repository The {@link CatalogInfoRepository} to dispose; may be null.
     */
    private void dispose(CatalogInfoRepository<?> repository) {
        if (repository != null) {
            repository.dispose();
        }
    }
}
