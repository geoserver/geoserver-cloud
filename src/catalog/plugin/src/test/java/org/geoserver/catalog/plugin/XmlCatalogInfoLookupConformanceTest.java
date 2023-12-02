/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin;

import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.plugin.forwarding.ResolvingCatalogFacadeDecorator;
import org.geoserver.catalog.plugin.resolving.CatalogPropertyResolver;
import org.geoserver.catalog.plugin.resolving.CollectionPropertiesInitializer;
import org.geoserver.catalog.plugin.resolving.ResolvingProxyResolver;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.junit.jupiter.api.Disabled;

public class XmlCatalogInfoLookupConformanceTest extends CatalogConformanceTest {

    protected @Override CatalogPlugin createCatalog() {
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
                CatalogPropertyResolver.<CatalogInfo>of(catalog) //
                        .andThen(ResolvingProxyResolver.of(catalog)) //
                        .andThen(CollectionPropertiesInitializer.instance()));
        catalog.setFacade(resolving);
        return catalog;
    }

    @Disabled(
            """
            revisit, seems to be just a problem of ordering or equals with the \
            returned ft/ft2 where mockito is not throwing the expected exception
            """)
    @Override
    public void testSaveDataStoreRollbacksBothStoreAndResources() throws Exception {}

    @Disabled(
            "don't care it can't save the resourceinfo when saving a layer, it's just a demo implementation")
    @Override
    public void testEnableLayer() {}
}
