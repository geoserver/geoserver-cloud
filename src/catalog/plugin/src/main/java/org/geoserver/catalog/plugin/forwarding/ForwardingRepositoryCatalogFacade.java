/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.catalog.plugin.forwarding;

import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.plugin.CatalogInfoRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.LayerGroupRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.LayerRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.MapRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.NamespaceRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.ResourceRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.StoreRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.StyleRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.WorkspaceRepository;
import org.geoserver.catalog.plugin.RepositoryCatalogFacade;

/**
 * A decorator implementation of {@link RepositoryCatalogFacade} that forwards all method calls to an underlying
 * {@link RepositoryCatalogFacade} instance.
 *
 * <p>This class extends {@link ForwardingExtendedCatalogFacade} to provide a base for creating decorators
 * that modify or extend the behavior of an existing {@link RepositoryCatalogFacade}. It implements the full
 * {@link RepositoryCatalogFacade} interface, including repository management methods, by delegating to the
 * wrapped facade. Subclasses can override specific methods to customize functionality (e.g., adding logging,
 * caching, or isolation) while retaining the default forwarding behavior for others.
 *
 * <p>Key characteristics:
 * <ul>
 *   <li><strong>Forwarding:</strong> All operations (e.g., {@link #get(String)}, {@link #query(Query)},
 *       repository setters/getters) are passed to the underlying facade.</li>
 *   <li><strong>Decorator Pattern:</strong> Facilitates extending facade behavior without altering the
 *       original implementation.</li>
 *   <li><strong>Repository Support:</strong> Implements {@link CatalogInfoRepositoryHolder} methods for
 *       managing type-specific repositories.</li>
 * </ul>
 *
 * <p>Use this class as a starting point when you need to enhance a {@link RepositoryCatalogFacade} with
 * additional logic while preserving its core functionality.
 *
 * @since 1.0
 * @see RepositoryCatalogFacade
 * @see ForwardingExtendedCatalogFacade
 * @see CatalogFacade
 */
public class ForwardingRepositoryCatalogFacade extends ForwardingExtendedCatalogFacade
        implements RepositoryCatalogFacade {

    /**
     * Constructs a new forwarding facade that delegates to the provided {@link RepositoryCatalogFacade}.
     *
     * @param facade The underlying {@link RepositoryCatalogFacade} to forward calls to; must not be null.
     * @throws NullPointerException if {@code facade} is null.
     */
    public ForwardingRepositoryCatalogFacade(RepositoryCatalogFacade facade) {
        super(facade);
    }

    /**
     * Sets the repository for managing {@link NamespaceInfo} objects by delegating to the underlying facade.
     *
     * @param namespaces The {@link NamespaceRepository} to set; must not be null.
     * @throws NullPointerException if {@code namespaces} is null.
     * @see RepositoryCatalogFacade#setNamespaceRepository(NamespaceRepository)
     */
    @Override
    public void setNamespaceRepository(NamespaceRepository namespaces) {
        asExtendedFacade().setNamespaceRepository(namespaces);
    }

    /**
     * Sets the repository for managing {@link WorkspaceInfo} objects by delegating to the underlying facade.
     *
     * @param workspaces The {@link WorkspaceRepository} to set; must not be null.
     * @throws NullPointerException if {@code workspaces} is null.
     * @see RepositoryCatalogFacade#setWorkspaceRepository(WorkspaceRepository)
     */
    @Override
    public void setWorkspaceRepository(WorkspaceRepository workspaces) {
        asExtendedFacade().setWorkspaceRepository(workspaces);
    }

    /**
     * Sets the repository for managing {@link StoreInfo} objects by delegating to the underlying facade.
     *
     * @param stores The {@link StoreRepository} to set; must not be null.
     * @throws NullPointerException if {@code stores} is null.
     * @see RepositoryCatalogFacade#setStoreRepository(StoreRepository)
     */
    @Override
    public void setStoreRepository(StoreRepository stores) {
        asExtendedFacade().setStoreRepository(stores);
    }

    /**
     * Sets the repository for managing {@link ResourceInfo} objects by delegating to the underlying facade.
     *
     * @param resources The {@link ResourceRepository} to set; must not be null.
     * @throws NullPointerException if {@code resources} is null.
     * @see RepositoryCatalogFacade#setResourceRepository(ResourceRepository)
     */
    @Override
    public void setResourceRepository(ResourceRepository resources) {
        asExtendedFacade().setResourceRepository(resources);
    }

    /**
     * Sets the repository for managing {@link LayerInfo} objects by delegating to the underlying facade.
     *
     * @param layers The {@link LayerRepository} to set; must not be null.
     * @throws NullPointerException if {@code layers} is null.
     * @see RepositoryCatalogFacade#setLayerRepository(LayerRepository)
     */
    @Override
    public void setLayerRepository(LayerRepository layers) {
        asExtendedFacade().setLayerRepository(layers);
    }

    /**
     * Sets the repository for managing {@link LayerGroupInfo} objects by delegating to the underlying facade.
     *
     * @param layerGroups The {@link LayerGroupRepository} to set; must not be null.
     * @throws NullPointerException if {@code layerGroups} is null.
     * @see RepositoryCatalogFacade#setLayerGroupRepository(LayerGroupRepository)
     */
    @Override
    public void setLayerGroupRepository(LayerGroupRepository layerGroups) {
        asExtendedFacade().setLayerGroupRepository(layerGroups);
    }

    /**
     * Sets the repository for managing {@link StyleInfo} objects by delegating to the underlying facade.
     *
     * @param styles The {@link StyleRepository} to set; must not be null.
     * @throws NullPointerException if {@code styles} is null.
     * @see RepositoryCatalogFacade#setStyleRepository(StyleRepository)
     */
    @Override
    public void setStyleRepository(StyleRepository styles) {
        asExtendedFacade().setStyleRepository(styles);
    }

    /**
     * Sets the repository for managing {@link MapInfo} objects by delegating to the underlying facade.
     *
     * @param maps The {@link MapRepository} to set; must not be null.
     * @throws NullPointerException if {@code maps} is null.
     * @see RepositoryCatalogFacade#setMapRepository(MapRepository)
     */
    @Override
    public void setMapRepository(MapRepository maps) {
        asExtendedFacade().setMapRepository(maps);
    }

    /**
     * Retrieves the repository for managing {@link NamespaceInfo} objects.
     *
     * <p>Forwards the call to the underlying facade’s {@link RepositoryCatalogFacade#getNamespaceRepository()}.
     *
     * @return The configured {@link NamespaceRepository}; never null.
     * @throws IllegalStateException if no namespace repository has been set in the underlying facade.
     * @example Accessing the namespace repository:
     *          <pre>
     *          NamespaceRepository nsRepo = facade.getNamespaceRepository();
     *          </pre>
     */
    @Override
    public NamespaceRepository getNamespaceRepository() {
        return asExtendedFacade().getNamespaceRepository();
    }

    /**
     * Retrieves the repository for managing {@link WorkspaceInfo} objects.
     *
     * <p>Forwards the call to the underlying facade’s {@link RepositoryCatalogFacade#getWorkspaceRepository()}.
     *
     * @return The configured {@link WorkspaceRepository}; never null.
     * @throws IllegalStateException if no workspace repository has been set in the underlying facade.
     */
    @Override
    public WorkspaceRepository getWorkspaceRepository() {
        return asExtendedFacade().getWorkspaceRepository();
    }

    /**
     * Retrieves the repository for managing {@link StoreInfo} objects.
     *
     * <p>Forwards the call to the underlying facade’s {@link RepositoryCatalogFacade#getStoreRepository()}.
     *
     * @return The configured {@link StoreRepository}; never null.
     * @throws IllegalStateException if no store repository has been set in the underlying facade.
     */
    @Override
    public StoreRepository getStoreRepository() {
        return asExtendedFacade().getStoreRepository();
    }

    /**
     * Retrieves the repository for managing {@link ResourceInfo} objects.
     *
     * <p>Forwards the call to the underlying facade’s {@link RepositoryCatalogFacade#getResourceRepository()}.
     *
     * @return The configured {@link ResourceRepository}; never null.
     * @throws IllegalStateException if no resource repository has been set in the underlying facade.
     */
    @Override
    public ResourceRepository getResourceRepository() {
        return asExtendedFacade().getResourceRepository();
    }

    /**
     * Retrieves the repository for managing {@link LayerInfo} objects.
     *
     * <p>Forwards the call to the underlying facade’s {@link RepositoryCatalogFacade#getLayerRepository()}.
     *
     * @return The configured {@link LayerRepository}; never null.
     * @throws IllegalStateException if no layer repository has been set in the underlying facade.
     */
    @Override
    public LayerRepository getLayerRepository() {
        return asExtendedFacade().getLayerRepository();
    }

    /**
     * Retrieves the repository for managing {@link LayerGroupInfo} objects.
     *
     * <p>Forwards the call to the underlying facade’s {@link RepositoryCatalogFacade#getLayerGroupRepository()}.
     *
     * @return The configured {@link LayerGroupRepository}; never null.
     * @throws IllegalStateException if no layer group repository has been set in the underlying facade.
     */
    @Override
    public LayerGroupRepository getLayerGroupRepository() {
        return asExtendedFacade().getLayerGroupRepository();
    }

    /**
     * Retrieves the repository for managing {@link StyleInfo} objects.
     *
     * <p>Forwards the call to the underlying facade’s {@link RepositoryCatalogFacade#getStyleRepository()}.
     *
     * @return The configured {@link StyleRepository}; never null.
     * @throws IllegalStateException if no style repository has been set in the underlying facade.
     */
    @Override
    public StyleRepository getStyleRepository() {
        return asExtendedFacade().getStyleRepository();
    }

    /**
     * Retrieves the repository for managing {@link MapInfo} objects.
     *
     * <p>Forwards the call to the underlying facade’s {@link RepositoryCatalogFacade#getMapRepository()}.
     *
     * @return The configured {@link MapRepository}; never null.
     * @throws IllegalStateException if no map repository has been set in the underlying facade.
     */
    @Override
    public MapRepository getMapRepository() {
        return asExtendedFacade().getMapRepository();
    }

    /**
     * Casts the underlying facade to {@link RepositoryCatalogFacade} for method delegation.
     *
     * <p>Overrides the parent method to ensure type safety when accessing repository-specific methods.
     *
     * @return The wrapped {@link RepositoryCatalogFacade}; never null.
     */
    @Override
    protected RepositoryCatalogFacade asExtendedFacade() {
        return (RepositoryCatalogFacade) facade;
    }

    /**
     * Retrieves the repository responsible for managing objects of the specified {@link CatalogInfo} type.
     *
     * <p>Forwards the call to the underlying facade’s {@link RepositoryCatalogFacade#repository(Class)}.
     *
     * @param <T> The type of {@link CatalogInfo} (e.g., {@link LayerInfo}).
     * @param <R> The corresponding repository type (e.g., {@link LayerRepository}).
     * @param of  The class of catalog info objects to retrieve the repository for; must not be null.
     * @return The repository managing objects of type {@code T}; never null.
     * @throws NullPointerException if {@code of} is null.
     * @throws IllegalArgumentException if no repository is configured for the specified type in the underlying facade.
     * @example Retrieving a layer repository:
     *          <pre>
     *          LayerRepository layerRepo = facade.repository(LayerInfo.class);
     *          </pre>
     */
    @Override
    public <T extends CatalogInfo, R extends CatalogInfoRepository<T>> R repository(Class<T> of) {
        return asExtendedFacade().repository(of);
    }

    /**
     * Retrieves the repository responsible for managing the type of the provided {@link CatalogInfo} instance.
     *
     * <p>Forwards the call to the underlying facade’s {@link RepositoryCatalogFacade#repositoryFor(CatalogInfo)}.
     *
     * @param <T>  The type of {@link CatalogInfo} (e.g., {@link StyleInfo}).
     * @param <R>  The corresponding repository type (e.g., {@link StyleRepository}).
     * @param info The catalog info object whose type determines the repository; must not be null.
     * @return The repository managing objects of the same type as {@code info}; never null.
     * @throws NullPointerException if {@code info} is null.
     * @throws IllegalArgumentException if no repository is configured for the object’s type in the underlying facade.
     * @example Retrieving a repository for a specific style:
     *          <pre>
     *          StyleInfo style = ...; // existing style
     *          StyleRepository styleRepo = facade.repositoryFor(style);
     *          </pre>
     */
    @Override
    public <T extends CatalogInfo, R extends CatalogInfoRepository<T>> R repositoryFor(T info) {
        return asExtendedFacade().repositoryFor(info);
    }
}
