/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogConformanceTest;
import org.geoserver.catalog.plugin.forwarding.ResolvingCatalogFacadeDecorator;
import org.geoserver.catalog.plugin.resolving.CatalogPropertyResolver;
import org.geoserver.catalog.plugin.resolving.CollectionPropertiesInitializer;
import org.geoserver.catalog.plugin.resolving.ResolvingProxyResolver;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;

public class XmlCatalogInfoLookupConformanceTest extends CatalogConformanceTest {

    protected @Override Catalog createCatalog() {
        CatalogPlugin catalog = new org.geoserver.catalog.plugin.CatalogPlugin();
        XStreamPersisterFactory xpf = new XStreamPersisterFactory();
        XStreamPersister codec = xpf.createXMLPersister();
        codec.setCatalog(catalog);

        DefaultMemoryCatalogFacade rawFacade = new DefaultMemoryCatalogFacade();

        rawFacade.setWorkspaceRepository(new XmlCatalogInfoLookup.WorkspaceInfoLookup(codec));
        rawFacade.setNamespaceRepository(new XmlCatalogInfoLookup.NamespaceInfoLookup(codec));
        rawFacade.setStoreRepository(new XmlCatalogInfoLookup.StoreInfoLookup(codec));
        rawFacade.setResourceRepository(new XmlCatalogInfoLookup.ResourceInfoLookup(codec));
        rawFacade.setLayerRepository(new XmlCatalogInfoLookup.LayerInfoLookup(codec));
        rawFacade.setLayerGroupRepository(new XmlCatalogInfoLookup.LayerGroupInfoLookup(codec));
        rawFacade.setStyleRepository(new XmlCatalogInfoLookup.StyleInfoLookup(codec));
        rawFacade.setMapRepository(new XmlCatalogInfoLookup.MapInfoLookup(codec));

        ResolvingCatalogFacadeDecorator resolving = new ResolvingCatalogFacadeDecorator(rawFacade);

        resolving.setOutboundResolver( //
                CatalogPropertyResolver.of(catalog) //
                        .andThen(ResolvingProxyResolver.of(catalog)) //
                        .andThen(CollectionPropertiesInitializer.instance()));
        catalog.setFacade(resolving);
        return catalog;
    }
}
