/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.catalogclient;

import org.geoserver.catalog.CatalogFacade;
import org.geoserver.cloud.config.catalog.GeoServerBackendConfigurer;
import org.geoserver.config.GeoServerFacade;
import org.geoserver.config.GeoServerLoader;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.ResourceStore;
import org.springframework.context.ApplicationContext;

public class CatalogServiceBackendConfigurer implements GeoServerBackendConfigurer {

    public @Override CatalogFacade catalogFacade() {
        // TODO Auto-generated method stub
        return null;
    }

    public @Override ApplicationContext getContext() {
        // TODO Auto-generated method stub
        return null;
    }

    public @Override GeoServerFacade geoserverFacade() {
        // TODO Auto-generated method stub
        return null;
    }

    public @Override ResourceStore resourceStoreImpl() {
        // TODO Auto-generated method stub
        return null;
    }

    public @Override GeoServerResourceLoader resourceLoader() {
        // TODO Auto-generated method stub
        return null;
    }

    public @Override GeoServerLoader geoServerLoaderImpl() {
        // TODO Auto-generated method stub
        return null;
    }
}
