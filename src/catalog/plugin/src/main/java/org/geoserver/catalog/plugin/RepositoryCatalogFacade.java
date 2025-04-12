/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.catalog.plugin;

import org.geoserver.catalog.CatalogFacade;

/**
 * A {@link CatalogFacade} extension that integrates repository-based catalog management by combining
 * {@link ExtendedCatalogFacade} and {@link CatalogInfoRepositoryHolder}.
 *
 * <p>This interface defines a catalog facade backed by {@link CatalogInfoRepository} instances, providing
 * a unified API for accessing and manipulating catalog data through specialized repositories (e.g.,
 * {@link WorkspaceRepository}, {@link LayerRepository}). It extends {@link ExtendedCatalogFacade} to
 * offer modern catalog operations like type-safe retrieval ({@link #get(String, Class)}), patch-based
 * updates ({@link #update(CatalogInfo, Patch)}), and stream-based querying ({@link #query(Query)}), while
 * incorporating the repository management capabilities of {@link CatalogInfoRepositoryHolder} to handle
 * type-specific data access (e.g., {@link #getNamespaceRepository()}).
 *
 * <p>Key features:
 * <ul>
 *   <li><strong>Repository-Backed:</strong> Delegates catalog operations to underlying repositories for
 *       persistence and querying.</li>
 *   <li><strong>Extended Functionality:</strong> Adds advanced methods beyond the standard
 *       {@link CatalogFacade}, enhancing usability and flexibility.</li>
 *   <li><strong>Type Safety:</strong> Ensures consistent mapping between {@link CatalogInfo} types and
 *       their repositories.</li>
 * </ul>
 *
 * <p>Implementations of this interface are expected to provide concrete repository instances and
 * coordinate their use with catalog operations, serving as a bridge between high-level facade logic and
 * low-level repository data access in GeoServer Cloud’s catalog system.
 *
 * @since 1.0
 * @see ExtendedCatalogFacade
 * @see CatalogInfoRepositoryHolder
 * @see CatalogInfoRepository
 */
public interface RepositoryCatalogFacade extends ExtendedCatalogFacade, CatalogInfoRepositoryHolder {}
