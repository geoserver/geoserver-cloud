/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin.rules;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.plugin.PropertyDiff;

/**
 * Interface to be implemented on a per {@link CatalogInfo} concrete type in order to apply {@link
 * Catalog} business rules upon different CRUD related events
 *
 * @see CatalogBusinessRules
 */
public interface CatalogInfoBusinessRules<T extends CatalogInfo> {

    default void onAfterAdd(Catalog catalog, T info) {}

    default void onBeforeSave(Catalog catalog, T info, PropertyDiff diff) {}

    default void onAfterSave(Catalog catalog, T info, PropertyDiff diff) {}

    default void onSaveError(Catalog catalog, T info, PropertyDiff diff, Throwable error) {}

    default void onRemoved(Catalog catalog, T info) {}
}
