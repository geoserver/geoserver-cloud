/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.jdbcconfig;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.catalog.plugin.CatalogFacadeExtensionAdapter;
import org.geoserver.catalog.plugin.CatalogFacadeExtensionAdapter.SilentCatalog;
import org.geoserver.jdbcconfig.catalog.JDBCCatalogFacade;
import org.geoserver.jdbcconfig.internal.ConfigDatabase;

/** */
public class CloudJdbcCatalogFacade extends JDBCCatalogFacade {

    private Catalog catalog;
    private ConfigDatabase db;

    public CloudJdbcCatalogFacade(ConfigDatabase db) {
        super(db);
        this.db = db;
    }

    public @Override void setCatalog(Catalog catalog) {
        this.catalog = catalog;
        CatalogImpl catalogForResolvingCatalogProperties = (CatalogImpl) catalog;
        if (catalog instanceof CatalogFacadeExtensionAdapter.SilentCatalog) {
            catalogForResolvingCatalogProperties = ((SilentCatalog) catalog).getSubject();
        }
        db.setCatalog(catalogForResolvingCatalogProperties);
    }

    public @Override Catalog getCatalog() {
        return this.catalog;
    }
}
