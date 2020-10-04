/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogConformanceTest;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;

public class XmlCatalogInfoLookupConformanceTest extends CatalogConformanceTest {

    protected @Override Catalog createCatalog() {
        CatalogPlugin catalog = new org.geoserver.catalog.plugin.CatalogPlugin();
        XStreamPersisterFactory xpf = new XStreamPersisterFactory();
        XStreamPersister codec = xpf.createXMLPersister();
        codec.setCatalog(catalog);

        DefaultMemoryCatalogFacade facade = new DefaultMemoryCatalogFacade();

        facade.setWorkspaceRepository(new XmlCatalogInfoLookup.WorkspaceInfoLookup(codec));
        facade.setNamespaceRepository(new XmlCatalogInfoLookup.NamespaceInfoLookup(codec));
        facade.setStoreRepository(new XmlCatalogInfoLookup.StoreInfoLookup(codec));
        facade.setResourceRepository(new XmlCatalogInfoLookup.ResourceInfoLookup(codec));
        facade.setLayerRepository(new XmlCatalogInfoLookup.LayerInfoLookup(codec));
        facade.setLayerGroupRepository(new XmlCatalogInfoLookup.LayerGroupInfoLookup(codec));
        facade.setStyleRepository(new XmlCatalogInfoLookup.StyleInfoLookup(codec));
        facade.setMapRepository(new XmlCatalogInfoLookup.MapInfoLookup(codec));

        catalog.setFacade(facade);
        return catalog;
    }
}
