/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin.validation;

import static com.google.common.base.Preconditions.checkArgument;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
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
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.ValidationResult;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WMTSLayerInfo;
import org.geoserver.catalog.WMTSStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.catalog.plugin.AbstractCatalogVisitor;
import org.geoserver.platform.GeoServerExtensions;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Support class implementing the {@link Catalog} state validation rules for {@link CatalogInfo}
 * objects before being created, updated, or removed;
 */
public class CatalogValidationRules {

    private Catalog catalog;
    private CatalogValidator defaultValidator;

    /** extended validation switch */
    protected boolean extendedValidation = true;

    public CatalogValidationRules(Catalog catalog) {
        Objects.requireNonNull(catalog);
        this.catalog = catalog;
        defaultValidator = new DefaultCatalogValidator(catalog);
    }

    /**
     * Turn on/off extended validation switch.
     *
     * <p>This is not part of the public api, it is used for testing purposes where we have to
     * bootstrap catalog contents.
     */
    public void setExtendedValidation(boolean extendedValidation) {
        this.extendedValidation = extendedValidation;
    }

    public boolean isExtendedValidation() {
        return extendedValidation;
    }

    public Iterable<CatalogValidator> getValidators() {
        return GeoServerExtensions.extensions(CatalogValidator.class);
    }

    public <T extends CatalogInfo> ValidationResult validate(T object, boolean isNew) {
        CatalogValidatorVisitor visitor = new CatalogValidatorVisitor(defaultValidator, isNew);
        if (isNew) {
            // REVISIT: of course the webui will call catalog.validate with a mod proxy and
            // isNew=true... ResourceConfigurationPage.doSaveInternal() for instance
            object = ModificationProxy.unwrap(object);
        }

        object.accept(visitor);
        return postValidate(object, isNew);
    }

    public <T extends CatalogInfo> void beforeRemove(T info) {
        info.accept(new BeforeRemoveValidator(catalog));
    }

    private ValidationResult postValidate(CatalogInfo info, boolean isNew) {
        List<RuntimeException> errors = new ArrayList<>();

        if (!extendedValidation) {
            return new ValidationResult(null);
        }

        for (CatalogValidator constraint : getValidators()) {
            try {
                info.accept(new CatalogValidatorVisitor(constraint, isNew));
            } catch (RuntimeException e) {
                errors.add(e);
            }
        }
        return new ValidationResult(errors);
    }

    @RequiredArgsConstructor
    private static final class BeforeRemoveValidator extends AbstractCatalogVisitor {

        private final @NonNull Catalog catalog;

        @Override
        public void visit(WorkspaceInfo workspace) {
            // JD: maintain the link between namespace and workspace, remove this when this is no
            // longer necessary
            if (catalog.getNamespaceByPrefix(workspace.getName()) != null) {
                throw new IllegalArgumentException("Cannot delete workspace with linked namespace");
            }
            if (!catalog.getStoresByWorkspace(workspace, StoreInfo.class).isEmpty()) {
                throw new IllegalArgumentException("Cannot delete non-empty workspace.");
            }
        }

        @Override
        public void visit(NamespaceInfo namespace) {
            // REVISIT: horrible performance, fetching all resources in memory
            if (!catalog.getResourcesByNamespace(namespace, ResourceInfo.class).isEmpty()) {
                throw new IllegalArgumentException("Unable to delete non-empty namespace.");
            }
        }

        @Override
        protected void visit(StoreInfo store) {
            // REVISIT: horrible performance, fetching all resources in memory
            if (!catalog.getResourcesByStore(store, ResourceInfo.class).isEmpty()) {
                throw new IllegalArgumentException("Unable to delete non-empty store.");
            }
        }

        @Override
        protected void visit(ResourceInfo resource) {
            // ensure no references to the resource
            if (!catalog.getLayers(resource).isEmpty()) {
                throw new IllegalArgumentException("Unable to delete resource referenced by layer");
            }
        }

        @Override
        public void visit(LayerInfo layer) {
            // ensure no references to the layer
            for (LayerGroupInfo lg : catalog.getLayerGroups()) {
                if (lg.getLayers().contains(layer) || layer.equals(lg.getRootLayer())) {
                    String msg =
                            "Unable to delete layer referenced by layer group '%s'"
                                    .formatted(lg.getName());
                    throw new IllegalArgumentException(msg);
                }
            }
        }

        @Override
        public void visit(StyleInfo style) {
            // ensure no references to the style
            List<LayerInfo> layers = catalog.getLayers(style);
            if (!layers.isEmpty()) {
                throw new IllegalArgumentException(
                        "Unable to delete style referenced by '%s'"
                                .formatted(layers.get(0).getName()));
            }
            // REVISIT: what about layer groups per workspace?
            for (LayerGroupInfo lg : catalog.getLayerGroups()) {
                if (lg.getStyles().contains(style) || style.equals(lg.getRootLayerStyle())) {
                    String msg =
                            "Unable to delete style referenced by layer group '%s'"
                                    .formatted(lg.getName());
                    throw new IllegalArgumentException(msg);
                }
            }
            checkArgument(
                    !DefaultCatalogValidator.isDefaultStyle(style),
                    "Unable to delete a default style");
        }

        @Override
        public void visit(LayerGroupInfo layerGroup) {
            // ensure no references to the layer group
            // REVISIT: should recurse into nested layer groups
            for (LayerGroupInfo lg : catalog.getLayerGroups()) {
                if (lg.getLayers().contains(layerGroup)) {
                    String msg =
                            "Unable to delete layer group referenced by layer group '%s'"
                                    .formatted(lg.getName());
                    throw new IllegalArgumentException(msg);
                }
            }
        }
    }

    /**
     * Bridges a {@link CatalogVisitor} to {@link CatalogValidator}{@literal .validate(...)} methods
     */
    static class CatalogValidatorVisitor extends CatalogVisitorAdapter {

        CatalogValidator validator;
        boolean isNew;

        CatalogValidatorVisitor(CatalogValidator validator, boolean isNew) {
            this.validator = validator;
            this.isNew = isNew;
        }

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
}
