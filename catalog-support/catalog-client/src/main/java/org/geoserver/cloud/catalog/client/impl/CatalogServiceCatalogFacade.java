/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.impl;

import java.util.function.Function;
import lombok.NonNull;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.plugin.ExtendedCatalogFacade;
import org.geoserver.catalog.plugin.forwarding.ResolvingCatalogFacadeDecorator;
import org.geoserver.catalog.plugin.resolving.CatalogPropertyResolver;
import org.geoserver.catalog.plugin.resolving.CollectionPropertiesInitializer;
import org.geoserver.catalog.plugin.resolving.ModificationProxyDecorator;
import org.geoserver.catalog.plugin.resolving.ResolvingProxyResolver;

public class CatalogServiceCatalogFacade extends ResolvingCatalogFacadeDecorator {

    public CatalogServiceCatalogFacade(@NonNull ExtendedCatalogFacade rawFacade) {
        super(rawFacade);
    }

    // set up resolving chain
    public @Override void setCatalog(Catalog catalog) {
        super.setCatalog(catalog);

        Function<CatalogInfo, CatalogInfo> resolvingFunction;
        resolvingFunction =
                CatalogPropertyResolver.of(catalog)
                        .andThen(ResolvingProxyResolver.of(catalog))
                        .andThen(CollectionPropertiesInitializer.instance())
                        // this one should be set by the catalog once it can take full ownership of
                        // wrapping and unwrapping ModificationProxies
                        .andThen(ModificationProxyDecorator.wrap());

        setOutboundResolver(resolvingFunction);
        setInboundResolver(ModificationProxyDecorator.unwrap());
    }
}
