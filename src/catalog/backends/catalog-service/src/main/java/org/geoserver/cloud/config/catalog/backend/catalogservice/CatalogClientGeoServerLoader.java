/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.catalog.backend.catalogservice;

import lombok.extern.slf4j.Slf4j;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.GeoServerLoader;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geotools.api.filter.Filter;

import java.io.IOException;

/** */
@Slf4j
public class CatalogClientGeoServerLoader extends GeoServerLoader {

    public CatalogClientGeoServerLoader(GeoServerResourceLoader resourceLoader) {
        super(resourceLoader);
    }

    protected @Override void initializeDefaultStyles(Catalog catalog) throws IOException {
        try {
            super.initializeDefaultStyles(catalog);
        } catch (Exception e) {
            log.warn(
                    "Unable to connect to catalog-service's catalog API during GeoServerLoader bean initialization",
                    e);
        }
    }

    protected @Override void loadCatalog(Catalog catalog, XStreamPersister xp) throws Exception {
        log.info("Checking catalog service health...");
        try {
            int count = catalog.count(WorkspaceInfo.class, Filter.INCLUDE);
            log.info(
                    "Catalog-service reports {} workspaces, roundtrip succeeded, assuming service is healthy",
                    count);
        } catch (Exception e) {
            log.warn(
                    "Unable to connect to catalog-service's catalog API during GeoServerLoader bean initialization",
                    e);
        }
    }

    protected @Override void loadGeoServer(GeoServer geoServer, XStreamPersister xp)
            throws Exception {
        log.info("Checking config service health...");
        try {
            GeoServerInfo global = geoServer.getGlobal();
            log.info(
                    "Catalog-service returned global config, update sequence: {}, roundtrip succeeded, assuming service is healthy",
                    global.getUpdateSequence());
        } catch (Exception e) {
            log.warn(
                    "Unable to connect to catalog-service's config API during GeoServerLoader bean initialization",
                    e);
        }
    }
}
