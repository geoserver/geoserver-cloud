/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.config.catalog.backend.core;

import org.geoserver.catalog.plugin.CatalogPlugin;

/**
 * Callback applied to the raw {@link CatalogPlugin} right after construction, letting backend modules install catalog
 * collaborators (e.g. a decorating {@code ResourcePool}) at a deterministic point, before the catalog is used by any
 * other bean.
 */
@FunctionalInterface
public interface RawCatalogCustomizer {

    void customize(CatalogPlugin rawCatalog);
}
