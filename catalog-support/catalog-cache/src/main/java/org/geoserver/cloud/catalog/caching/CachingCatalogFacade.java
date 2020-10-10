/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.caching;

import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.ClassMappings;
import org.geoserver.catalog.plugin.ExtendedCatalogFacade;

/** */
public interface CachingCatalogFacade extends ExtendedCatalogFacade {

    String CACHE_NAME = "catalog";
    String DEFAULT_NAMESPACE_CACHE_KEY = "defaultNamespace";
    String DEFAULT_WORKSPACE_CACHE_KEY = "defaultWorkspace";
    String DEFAULT_DATASTORE_CACHE_KEY_PREFIX = "defaultDataStore.";

    <C extends CatalogInfo> boolean evict(C info);

    static Object generateDefaultDataStoreKey(WorkspaceInfo workspace) {
        return DEFAULT_DATASTORE_CACHE_KEY_PREFIX + workspace.getId();
    }

    static Key generateLayersByResourceKey(ResourceInfo resource) {
        return new Key("layers@" + resource.getId(), ClassMappings.LAYER);
    }
}
