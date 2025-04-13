/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.catalog.plugin.rules;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.event.CatalogAddEvent;
import org.geoserver.catalog.event.CatalogBeforeAddEvent;
import org.geoserver.catalog.event.CatalogModifyEvent;
import org.geoserver.catalog.event.CatalogPostModifyEvent;
import org.geoserver.catalog.event.CatalogRemoveEvent;
import org.geoserver.catalog.plugin.ExtendedCatalogFacade;

/**
 * Interface to be implemented on a per {@link CatalogInfo} concrete type in order to apply {@link
 * Catalog} business rules upon different CRUD related events
 *
 * @see CatalogBusinessRules
 */
public interface CatalogInfoBusinessRules<T extends CatalogInfo> {

    /**
     * Apply business rules before {@link CatalogOpContext#getCatalog() catalog} publishes a {@link
     * CatalogBeforeAddEvent} and calls {@link CatalogFacade#add}; may use {@link
     * CatalogOpContext#setContextOption} to provide further context to {@link #afterAdd}
     */
    default void beforeAdd(CatalogOpContext<T> context) {}

    /**
     * Apply business rules once {@link CatalogOpContext#getCatalog() catalog} has called {@link
     * CatalogFacade#add} and before publishing a {@link CatalogAddEvent}; may use {@link
     * CatalogOpContext#setContextOption} to obtain further context information from {@link
     * #beforeAdd}
     *
     * @param context same context used for {@link #beforeAdd}
     */
    default void afterAdd(CatalogOpContext<T> context) {}

    /**
     * Apply business rules before {@link CatalogOpContext#getCatalog() catalog} publishes a {@link
     * CatalogModifyEvent} and calls {@link CatalogFacade#save}/{@link
     * ExtendedCatalogFacade#update}; may use {@link CatalogOpContext#setContextOption} to provide
     * further context to {@link #afterSave}
     */
    default void beforeSave(CatalogOpContext<T> context) {}

    /**
     * Apply business rules once {@link CatalogOpContext#getCatalog() catalog} has called {@link
     * CatalogFacade#save} and before publishing a {@link CatalogPostModifyEvent}; may use {@link
     * CatalogOpContext#setContextOption} to obtain further context information from {@link
     * #beforeSave}
     *
     * @param context same context used for {@link #beforeSave}
     */
    default void afterSave(CatalogOpContext<T> context) {}

    /**
     * Apply business rules before {@link CatalogOpContext#getCatalog() catalog} calls {@link
     * CatalogFacade#remove} (no before-remove event is published); may use {@link
     * CatalogOpContext#setContextOption} to provide further context to {@link #afterRemove}
     */
    default void beforeRemove(CatalogOpContext<T> context) {}

    /**
     * Apply business rules once {@link CatalogOpContext#getCatalog() catalog} has called {@link
     * CatalogFacade#remove} and before publishing a {@link CatalogRemoveEvent}; may use {@link
     * CatalogOpContext#setContextOption} to obtain further context information from {@link
     * #beforeRemove}
     *
     * @param context same context used for {@link #beforeRemove}
     */
    default void afterRemove(CatalogOpContext<T> context) {}
}
