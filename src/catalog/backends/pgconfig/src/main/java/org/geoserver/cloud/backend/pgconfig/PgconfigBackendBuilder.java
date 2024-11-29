/*
 * (c) 2023 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.backend.pgconfig;

import javax.sql.DataSource;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.catalog.plugin.CatalogPlugin;
import org.geoserver.catalog.plugin.ExtendedCatalogFacade;
import org.geoserver.cloud.backend.pgconfig.catalog.PgconfigCatalogFacade;
import org.geoserver.cloud.backend.pgconfig.config.PgconfigGeoServerFacade;
import org.geoserver.config.plugin.GeoServerImpl;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @since 1.4
 */
@RequiredArgsConstructor
public class PgconfigBackendBuilder {

    private final @NonNull DataSource dataSource;

    public CatalogPlugin createCatalog() {
        return initCatalog(new CatalogPlugin());
    }

    public GeoServerImpl createGeoServer(Catalog catalog) {
        PgconfigGeoServerFacade facade = createGeoServerFacade();
        GeoServerImpl gs = new GeoServerImpl(facade);
        gs.setCatalog(catalog);
        return gs;
    }

    public PgconfigGeoServerFacade createGeoServerFacade() {
        return new PgconfigGeoServerFacade(new JdbcTemplate(dataSource));
    }

    public ExtendedCatalogFacade createCatalogFacade() {
        JdbcTemplate template = new JdbcTemplate(dataSource);
        return new PgconfigCatalogFacade(template);
    }

    public <C extends CatalogImpl> C initCatalog(C catalog) {
        ExtendedCatalogFacade facade = createCatalogFacade();
        catalog.setFacade(facade);
        return catalog;
    }
}
