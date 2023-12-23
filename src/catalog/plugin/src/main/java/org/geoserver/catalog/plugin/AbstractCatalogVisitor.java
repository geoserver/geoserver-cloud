/*
 * (c) 2023 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin;

import org.geoserver.catalog.CatalogVisitorAdapter;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WMTSLayerInfo;
import org.geoserver.catalog.WMTSStoreInfo;

/**
 * {@link CatalogVisitorAdapter} that proxies all concrete {@link ResourceInfo} visit methods to
 * {@link #visit(ResourceInfo)} and all concrete {@link StoreInfo} visit methods to {@link
 * #visit(StoreInfo)}
 */
public abstract class AbstractCatalogVisitor extends CatalogVisitorAdapter {

    @Override
    public void visit(DataStoreInfo dataStore) {
        visit((StoreInfo) dataStore);
    }

    @Override
    public void visit(CoverageStoreInfo coverageStore) {
        visit((StoreInfo) coverageStore);
    }

    @Override
    public void visit(WMSStoreInfo wmsStore) {
        visit((StoreInfo) wmsStore);
    }

    @Override
    public void visit(WMTSStoreInfo wmtsStore) {
        visit((StoreInfo) wmtsStore);
    }

    protected void visit(StoreInfo store) {
        // to be overridden to catch all concrete StoreInfos
    }

    @Override
    public void visit(FeatureTypeInfo featureType) {
        visit((ResourceInfo) featureType);
    }

    @Override
    public void visit(CoverageInfo coverage) {
        visit((ResourceInfo) coverage);
    }

    @Override
    public void visit(WMSLayerInfo wmsLayer) {
        visit((ResourceInfo) wmsLayer);
    }

    @Override
    public void visit(WMTSLayerInfo wmtsLayer) {
        visit((ResourceInfo) wmtsLayer);
    }

    protected void visit(ResourceInfo resource) {
        // to be overridden to catch all concrete ResourceInfos
    }
}
