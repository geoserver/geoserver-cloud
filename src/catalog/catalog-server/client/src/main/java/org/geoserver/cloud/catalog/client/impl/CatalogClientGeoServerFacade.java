/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.impl;

import org.geoserver.cloud.catalog.client.repository.CatalogClientConfigRepository;
import org.geoserver.config.plugin.RepositoryGeoServerFacadeImpl;

/** */
public class CatalogClientGeoServerFacade extends RepositoryGeoServerFacadeImpl {

    public CatalogClientGeoServerFacade(CatalogClientConfigRepository repository) {
        super(repository);
    }
}
