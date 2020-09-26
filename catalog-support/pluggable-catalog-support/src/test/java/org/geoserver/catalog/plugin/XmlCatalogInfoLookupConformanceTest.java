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
        CatalogImpl catalog = new org.geoserver.catalog.plugin.CatalogImpl();
        XStreamPersisterFactory xpf = new XStreamPersisterFactory();
        XStreamPersister codec = xpf.createXMLPersister();
        codec.setCatalog(catalog);

        DefaultCatalogFacade facade = new DefaultCatalogFacade();

        facade.setWorkspaces(new XmlCatalogInfoLookup.WorkspaceInfoLookup(codec));
        facade.setNamespaces(new XmlCatalogInfoLookup.NamespaceInfoLookup(codec));
        facade.setStores(new XmlCatalogInfoLookup.StoreInfoLookup(codec));
        facade.setResources(new XmlCatalogInfoLookup.ResourceInfoLookup(codec));
        facade.setLayers(new XmlCatalogInfoLookup.LayerInfoLookup(codec));
        facade.setLayerGroups(new XmlCatalogInfoLookup.LayerGroupInfoLookup(codec));
        facade.setStyles(new XmlCatalogInfoLookup.StyleInfoLookup(codec));
        facade.setMaps(new XmlCatalogInfoLookup.MapInfoLookup(codec));

        catalog.setFacade(facade);
        return catalog;
    }
}
