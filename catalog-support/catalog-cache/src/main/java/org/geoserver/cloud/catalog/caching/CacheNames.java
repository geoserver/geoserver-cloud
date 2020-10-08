/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.caching;

public interface CacheNames {

    String DEFAULT_KEY_GENERATOR_BEAN_NAME = "catalogIdCacheGenerator";

    String WORKSPACE_CACHE = "WorkspaceInfo";
    String NAMESPACE_CACHE = "NamespaceInfo";
    String STORE_CACHE = "StoreInfo";
    String RESOURCE_CACHE = "ResourceInfo";
    String LAYER_CACHE = "LayerInfo";
    String LAYER_GROUP_CACHE = "LayerGroupInfo";
    String STYLE_CACHE = "StyleInfo";
    String MAP_CACHE = "MapInfo";
}
