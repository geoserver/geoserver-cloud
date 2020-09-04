/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.test.backend;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogFacade;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerFacade;
import org.geoserver.config.GeoServerLoader;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.ResourceStore;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ActiveProfiles("test")
public abstract class GeoServerBackendConfigurerTest {

    protected @Autowired ApplicationContext context;

    protected @Autowired @Qualifier("catalog") Catalog catalog;
    protected @Autowired @Qualifier("rawCatalog") Catalog rawCatalog;
    protected @Autowired @Qualifier("catalogFacade") CatalogFacade rawCatalogFacade;
    protected @Autowired @Qualifier("geoServer") GeoServer geoServer;
    protected @Autowired @Qualifier("resourceLoader") GeoServerResourceLoader resourceLoader;
    protected @Autowired(required = false) @Qualifier("geoserverFacade") GeoServerFacade
            geoserverFacade;
    protected @Autowired @Qualifier("geoServerLoaderImpl") GeoServerLoader geoserverLoader;
    protected @Autowired @Qualifier("resourceStoreImpl") ResourceStore resourceStoreImpl;
}
