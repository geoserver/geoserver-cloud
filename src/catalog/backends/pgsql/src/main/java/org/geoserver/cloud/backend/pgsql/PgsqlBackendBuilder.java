/*
 * (c) 2023 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.backend.pgsql;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.catalog.plugin.CatalogPlugin;
import org.geoserver.catalog.plugin.ExtendedCatalogFacade;
import org.geoserver.catalog.plugin.forwarding.ResolvingCatalogFacadeDecorator;
import org.geoserver.catalog.plugin.resolving.CatalogPropertyResolver;
import org.geoserver.catalog.plugin.resolving.CollectionPropertiesInitializer;
import org.geoserver.catalog.plugin.resolving.ResolvingProxyResolver;
import org.geoserver.cloud.backend.pgsql.catalog.PgsqlCatalogFacade;
import org.geoserver.cloud.backend.pgsql.config.PgsqlGeoServerFacade;
import org.geoserver.config.plugin.GeoServerImpl;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.function.UnaryOperator;

import javax.sql.DataSource;

/**
 * @since 1.4
 */
@RequiredArgsConstructor
public class PgsqlBackendBuilder {

    private final @NonNull DataSource dataSource;

    public CatalogPlugin createCatalog() {
        return initCatalog(new CatalogPlugin());
    }

    public GeoServerImpl createGeoServer(Catalog catalog) {
        PgsqlGeoServerFacade facade = createGeoServerFacade();
        GeoServerImpl gs = new GeoServerImpl(facade);
        gs.setCatalog(catalog);
        return gs;
    }

    public PgsqlGeoServerFacade createGeoServerFacade() {
        return new PgsqlGeoServerFacade(new JdbcTemplate(dataSource));
    }

    public ExtendedCatalogFacade createCatalogFacade(Catalog catalog) {
        JdbcTemplate template = new JdbcTemplate(dataSource);
        PgsqlCatalogFacade facade = new PgsqlCatalogFacade(template);

        return createResolvingCatalogFacade(catalog, facade);
    }

    public static ExtendedCatalogFacade createResolvingCatalogFacade(
            Catalog catalog, PgsqlCatalogFacade rawFacade) {
        UnaryOperator<CatalogInfo> resolvingFunction =
                CatalogPropertyResolver.<CatalogInfo>of(catalog)
                                .andThen(ResolvingProxyResolver.<CatalogInfo>of(catalog))
                                .andThen(CollectionPropertiesInitializer.instance())
                        ::apply;

        ResolvingCatalogFacadeDecorator resolving = new ResolvingCatalogFacadeDecorator(rawFacade);
        resolving.setOutboundResolver(resolvingFunction);
        return resolving;
    }

    public <C extends CatalogImpl> C initCatalog(C catalog) {
        ExtendedCatalogFacade facade = createCatalogFacade(catalog);
        catalog.setFacade(facade);
        return catalog;
    }
}
