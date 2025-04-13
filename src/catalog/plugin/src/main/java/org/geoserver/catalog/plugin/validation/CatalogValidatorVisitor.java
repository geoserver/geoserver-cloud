/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.catalog.plugin.validation;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.geoserver.catalog.CatalogValidator;
import org.geoserver.catalog.CatalogVisitor;
import org.geoserver.catalog.CatalogVisitorAdapter;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WMTSLayerInfo;
import org.geoserver.catalog.WMTSStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;

/** Bridges a {@link CatalogVisitor} to {@link CatalogValidator}{@literal .validate(...)} methods */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class CatalogValidatorVisitor extends CatalogVisitorAdapter {

    private final @NonNull CatalogValidator validator;
    private final boolean isNew;

    @Override
    public void visit(WorkspaceInfo workspace) {
        validator.validate(workspace, isNew);
    }

    @Override
    public void visit(NamespaceInfo namespace) {
        validator.validate(namespace, isNew);
    }

    @Override
    public void visit(DataStoreInfo dataStore) {
        validator.validate(dataStore, isNew);
    }

    @Override
    public void visit(CoverageStoreInfo coverageStore) {
        validator.validate(coverageStore, isNew);
    }

    @Override
    public void visit(WMSStoreInfo wmsStore) {
        validator.validate(wmsStore, isNew);
    }

    @Override
    public void visit(WMTSStoreInfo wmtsStore) {
        validator.validate(wmtsStore, isNew);
    }

    @Override
    public void visit(FeatureTypeInfo featureType) {
        validator.validate(featureType, isNew);
    }

    @Override
    public void visit(CoverageInfo coverage) {
        validator.validate(coverage, isNew);
    }

    @Override
    public void visit(LayerInfo layer) {
        validator.validate(layer, isNew);
    }

    @Override
    public void visit(StyleInfo style) {
        validator.validate(style, isNew);
    }

    @Override
    public void visit(LayerGroupInfo layerGroup) {
        validator.validate(layerGroup, isNew);
    }

    @Override
    public void visit(WMSLayerInfo wmsLayer) {
        validator.validate(wmsLayer, isNew);
    }

    @Override
    public void visit(WMTSLayerInfo wmtsLayer) {
        validator.validate(wmtsLayer, isNew);
    }
}
