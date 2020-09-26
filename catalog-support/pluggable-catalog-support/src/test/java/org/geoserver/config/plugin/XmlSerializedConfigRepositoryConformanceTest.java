/*
 * /* (c) 2014 Open Source Geospatial Foundation - all rights reserved (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.config.plugin;

import org.geoserver.catalog.plugin.CatalogImpl;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerConfigConformanceTest;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;

public class XmlSerializedConfigRepositoryConformanceTest extends GeoServerConfigConformanceTest {

    protected @Override GeoServer createGeoServer() {
        CatalogImpl catalog = new CatalogImpl();
        GeoServerImpl gs = new GeoServerImpl();
        gs.setCatalog(catalog);

        XStreamPersisterFactory xpf = new XStreamPersisterFactory();
        XStreamPersister codec = xpf.createXMLPersister();
        codec.setCatalog(catalog);
        XmlSerializedConfigRepository repository = new XmlSerializedConfigRepository(codec);

        DefaultGeoServerFacade facade = new DefaultGeoServerFacade();
        facade.setRepository(repository);
        gs.setFacade(facade);

        return gs;
    }
}
