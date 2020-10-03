/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin;

import static java.lang.String.format;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.CatalogValidator;
import org.geoserver.catalog.CatalogVisitor;
import org.geoserver.catalog.CoverageDimensionInfo;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.KeywordInfo;
import org.geoserver.catalog.LayerGroupHelper;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerGroupInfo.Mode;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MapInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.PublishedType;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.SLDNamedLayerValidator;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.ValidationResult;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WMTSLayerInfo;
import org.geoserver.catalog.WMTSStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.CoverageDimensionImpl;
import org.geoserver.catalog.impl.CoverageInfoImpl;
import org.geoserver.catalog.impl.FeatureTypeInfoImpl;
import org.geoserver.catalog.impl.ResourceInfoImpl;
import org.geoserver.catalog.impl.StoreInfoImpl;
import org.geoserver.catalog.impl.StyleInfoImpl;
import org.geoserver.catalog.impl.WMSLayerInfoImpl;
import org.geoserver.catalog.impl.WMTSLayerInfoImpl;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.styling.StyledLayerDescriptor;
import org.geotools.util.SuppressFBWarnings;
import org.geotools.util.logging.Logging;

/**
 * Support class implementing the {@link Catalog} state validation rules for {@link CatalogInfo}
 * objects before being created, updated, or removed;
 */
public class CatalogValidationRules {

    private static final Logger LOGGER = Logging.getLogger(CatalogValidationRules.class);

    private Catalog catalog;

    /** extended validation switch */
    protected boolean extendedValidation = true;

    public CatalogValidationRules(Catalog catalog) {
        Objects.requireNonNull(catalog);
        this.catalog = catalog;
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

    public ValidationResult validate(MapInfo map, boolean isNew) {
        if (isNew) {
            resolve(map);
        }
        // there's no visitor method of MapInfo
        return new ValidationResult(null);
    }

    public ValidationResult validate(StoreInfo store, boolean isNew) {
        if (isNew) {
            resolve(store);
        }
        checkNotEmpty(store.getName(), "Store name must not be null");
        checkNotNull(store.getWorkspace(), "Store must be part of a workspace");

        WorkspaceInfo workspace = store.getWorkspace();
        StoreInfo existing = catalog.getStoreByName(workspace, store.getName(), StoreInfo.class);
        if (existing != null && (isNew || !existing.getId().equals(store.getId()))) {
            String msg =
                    "Store '"
                            + store.getName()
                            + "' already exists in workspace '"
                            + workspace.getName()
                            + "'";
            throw new IllegalArgumentException(msg);
        }

        return postValidate(store, isNew);
    }

    public ValidationResult validate(ResourceInfo resource, boolean isNew) {
        checkNotEmpty(resource.getName(), "Resource name must not be null");
        if (isNew) {
            resolve(resource);
        }
        if (isNullOrEmpty(resource.getNativeName())
                && !(resource instanceof CoverageInfo
                        && ((CoverageInfo) resource).getNativeCoverageName() != null)) {
            throw new NullPointerException("Resource native name must not be null");
        }
        checkNotNull(resource.getStore(), "Resource must be part of a store");
        checkNotNull(resource.getNamespace(), "Resource must be part of a namespace");

        StoreInfo store = resource.getStore();
        ResourceInfo existing =
                catalog.getResourceByStore(store, resource.getName(), ResourceInfo.class);
        checkArgument(
                existing == null || existing.getId().equals(resource.getId()),
                "Resource named '%s' already exists in store: '%s'",
                resource.getName(),
                store.getName());

        NamespaceInfo namespace = resource.getNamespace();
        existing = catalog.getResourceByName(namespace, resource.getName(), ResourceInfo.class);
        checkArgument(
                existing == null || existing.getId().equals(resource.getId()),
                "Resource named '%s' already exists in namespace: '%s'",
                resource.getName(),
                namespace.getPrefix());

        validateKeywords(resource.getKeywords());
        return postValidate(resource, isNew);
    }

    @SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE")
    public ValidationResult validate(LayerInfo layer, boolean isNew) {
        // TODO: bring back when the layer/publishing split is in act
        // if ( isNull(layer.getName()) ) {
        // throw new NullPointerException( "Layer name must not be null" );
        // }
        final ResourceInfo resource = layer.getResource();
        checkNotNull(resource, "Layer resource must not be null");
        if (isNew) {
            resolve(layer);
        }
        // calling LayerInfo.setName(String) updates the resource (until the layer/publishing split
        // is in act), but that doesn't mean the resource was saved previously, which can leave the
        // catalog in an inconsistent state
        final NamespaceInfo ns = resource.getNamespace();
        checkArgument(
                null != catalog.getResourceByName(ns, resource.getName(), ResourceInfo.class),
                "Found no resource named %s, Layer with that name can't be added",
                layer.prefixedName());

        final String prefix = ns != null ? ns.getPrefix() : null;
        LayerInfo existing = CatalogImpl.getLayerByName(catalog, prefix, layer.getName());
        checkArgument(
                existing == null || existing.getId().equals(layer.getId()),
                "Layer named '%s' in workspace '%s' already exists.",
                layer.getName(),
                prefix);

        // if the style is missing associate a default one, to avoid breaking WMS
        if (layer.getDefaultStyle() == null) {
            try {
                LOGGER.log(
                        Level.INFO,
                        "Layer "
                                + layer.prefixedName()
                                + " is missing the default style, assigning one automatically");
                StyleInfo style = new CatalogBuilder(catalog).getDefaultStyle(resource);
                layer.setDefaultStyle(style);
            } catch (IOException e) {
                LOGGER.log(
                        Level.WARNING,
                        "Layer "
                                + layer.prefixedName()
                                + " is missing the default style, "
                                + "failed to associate one automatically",
                        e);
            }
        }

        // clean up eventual dangling references to missing alternate styles
        Set<StyleInfo> styles = layer.getStyles();
        for (Iterator<StyleInfo> it = styles.iterator(); it.hasNext(); ) {
            StyleInfo styleInfo = (StyleInfo) it.next();
            if (styleInfo == null) {
                it.remove();
            }
        }

        return postValidate(layer, isNew);
    }

    @SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE")
    public ValidationResult validate(LayerGroupInfo layerGroup, boolean isNew) {
        checkNotEmpty(layerGroup.getName(), "Layer group name must not be null");
        if (isNew) {
            resolve(layerGroup);
        }

        WorkspaceInfo ws = layerGroup.getWorkspace();
        LayerGroupInfo existing = catalog.getLayerGroupByName(ws, layerGroup.getName());
        if (existing != null && !existing.getId().equals(layerGroup.getId())) {
            // null workspace can cause layer group in any workspace to be returned, check that
            // workspaces match
            WorkspaceInfo ews = existing.getWorkspace();
            if ((ws == null && ews == null) || (ws != null && ws.equals(ews))) {
                String msg = "Layer group named '" + layerGroup.getName() + "' already exists";
                if (ws != null) {
                    msg += " in workspace " + ws.getName();
                }
                throw new IllegalArgumentException(msg);
            }
        }

        // sanitize a bit broken layer references
        List<PublishedInfo> layers = layerGroup.getLayers();
        checkArgument(layers != null && !layers.isEmpty(), "Layer group must not be empty");
        List<StyleInfo> styles = layerGroup.getStyles();
        for (int i = 0; i < layers.size(); ) {
            if (styles != null && layers.get(i) == null && styles.get(i) == null) {
                layers.remove(i);
                styles.remove(i);
            } else {
                // Validate style group
                if (layers.get(i) == null) {
                    try {
                        // validate style groups
                        StyledLayerDescriptor sld = styles.get(i).getSLD();
                        List<Exception> errors = SLDNamedLayerValidator.validate(catalog, sld);
                        if (errors.size() > 0) {
                            throw new IllegalArgumentException(
                                    "Invalid style group: " + errors.get(0).getMessage(),
                                    errors.get(0));
                        }
                    } catch (IOException e) {
                        throw new IllegalArgumentException(
                                "Error validating style group: " + e.getMessage(), e);
                    }
                }
                i++;
            }
        }
        if (layerGroup.getStyles() != null
                && !layerGroup.getStyles().isEmpty()
                && !(layerGroup.getStyles().size() == layerGroup.getLayers().size())) {
            throw new IllegalArgumentException(
                    "Layer group has different number of styles than layers");
        }

        LayerGroupHelper helper = new LayerGroupHelper(layerGroup);
        Stack<LayerGroupInfo> loopPath = helper.checkLoops();
        if (loopPath != null) {
            throw new IllegalArgumentException(
                    "Layer group is in a loop: " + helper.getLoopAsString(loopPath));
        }

        // if the layer group has a workspace assigned, ensure that every resource in that layer
        // group lives within the same workspace
        if (ws != null) {
            checkLayerGroupResourceIsInWorkspace(layerGroup, ws);
        }

        Mode mode = layerGroup.getMode();
        checkArgument(mode != null, "Layer group mode must not be null");
        if (LayerGroupInfo.Mode.EO.equals(mode)) {
            checkArgument(
                    layerGroup.getRootLayer() != null,
                    "Layer group in mode %s must have a root layer",
                    mode.getName());
            checkArgument(
                    layerGroup.getRootLayerStyle() != null,
                    "Layer group in mode %s must have a root layer style",
                    mode.getName());
        } else {
            checkArgument(
                    layerGroup.getRootLayer() == null,
                    "Layer group in mode %s must not have a root layer",
                    mode.getName());
            checkArgument(
                    layerGroup.getRootLayerStyle() == null,
                    "Layer group in mode %s must not have a root layer style",
                    mode.getName());
        }

        return postValidate(layerGroup, isNew);
    }

    private void checkLayerGroupResourceIsInWorkspace(LayerGroupInfo layerGroup, WorkspaceInfo ws) {
        if (layerGroup == null) return;
        if (layerGroup.getWorkspace() != null) {
            checkArgument(
                    ws.getId().equals(layerGroup.getWorkspace().getId()),
                    "Layer group within a workspace (%s) can not contain resources from other workspace: %s",
                    ws.getName(),
                    layerGroup.getWorkspace().getName());
        }

        checkLayerGroupResourceIsInWorkspace(layerGroup.getRootLayer(), ws);
        checkLayerGroupResourceIsInWorkspace(layerGroup.getRootLayerStyle(), ws);
        if (layerGroup.getLayers() != null) {
            for (PublishedInfo p : layerGroup.getLayers()) {
                if (p instanceof LayerGroupInfo) {
                    checkLayerGroupResourceIsInWorkspace((LayerGroupInfo) p, ws);
                } else if (p instanceof LayerInfo) {
                    checkLayerGroupResourceIsInWorkspace((LayerInfo) p, ws);
                }
            }
        }

        if (layerGroup.getStyles() != null) {
            for (StyleInfo s : layerGroup.getStyles()) {
                checkLayerGroupResourceIsInWorkspace(s, ws);
            }
        }
    }

    private void checkLayerGroupResourceIsInWorkspace(StyleInfo style, WorkspaceInfo ws) {
        if (style == null) return;

        if (style.getWorkspace() != null && !ws.equals(style.getWorkspace())) {
            throw new IllegalArgumentException(
                    "Layer group within a workspace ("
                            + ws.getName()
                            + ") can not contain styles from other workspace: "
                            + style.getWorkspace());
        }
    }

    private void checkLayerGroupResourceIsInWorkspace(LayerInfo layer, WorkspaceInfo ws) {
        if (layer == null) return;

        ResourceInfo r = layer.getResource();
        if (r.getStore().getWorkspace() != null && !ws.equals(r.getStore().getWorkspace())) {
            throw new IllegalArgumentException(
                    "Layer group within a workspace ("
                            + ws.getName()
                            + ") can not contain resources from other workspace: "
                            + r.getStore().getWorkspace().getName());
        }
    }

    public ValidationResult validate(NamespaceInfo namespace, boolean isNew) {
        checkNotEmpty(namespace.getPrefix(), "Namespace prefix must not be null");
        checkNotEmpty(namespace.getURI(), "Namespace uri must not be null");
        if (isNew) {
            resolve(namespace);
        }
        if (namespace.isIsolated()
                && !catalog.getCatalogCapabilities().supportsIsolatedWorkspaces()) {
            // isolated namespaces \ workspaces are not supported by this catalog
            throw new IllegalArgumentException(
                    String.format(
                            "Namespace '%s:%s' is isolated but isolated workspaces are not supported by this catalog.",
                            namespace.getPrefix(), namespace.getURI()));
        }
        checkArgument(
                !namespace.getPrefix().equals(Catalog.DEFAULT),
                "%s is a reserved keyword, can't be used as the namespace prefix",
                Catalog.DEFAULT);

        NamespaceInfo existing = catalog.getNamespaceByPrefix(namespace.getPrefix());
        checkArgument(
                existing == null || existing.getId().equals(namespace.getId()),
                "Namespace with prefix '%s' already exists.",
                namespace.getPrefix());

        if (!namespace.isIsolated()) {
            // not an isolated namespace \ workplace so we need to check for duplicates
            existing = catalog.getNamespaceByURI(namespace.getURI());
            checkArgument(
                    existing == null || existing.getId().equals(namespace.getId()),
                    "Namespace with URI '%s' already exists.",
                    namespace.getURI());
        }

        try {
            new URI(namespace.getURI());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(
                    format(
                            "Invalid URI syntax for '%s' in namespace '%s'",
                            namespace.getURI(), namespace.getPrefix()),
                    e);
        }

        return postValidate(namespace, isNew);
    }

    public ValidationResult validate(WorkspaceInfo workspace, boolean isNew) {
        checkNotEmpty(workspace.getName(), "workspace name must not be null");
        checkArgument(
                !Catalog.DEFAULT.equals(workspace.getName()),
                "%s is a reserved keyword, can't be used as the workspace name",
                Catalog.DEFAULT);

        if (isNew) {
            resolve(workspace);
        }
        checkArgument(
                !workspace.isIsolated()
                        || catalog.getCatalogCapabilities().supportsIsolatedWorkspaces(),
                "Workspace '%s' is isolated but isolated workspaces are not supported by this catalog.",
                workspace.getName());

        WorkspaceInfo existing = catalog.getWorkspaceByName(workspace.getName());
        checkArgument(
                existing == null || existing.getId().equals(workspace.getId()),
                "Workspace named '%s' already exists.",
                workspace.getName());

        return postValidate(workspace, isNew);
    }

    public ValidationResult validate(StyleInfo style, boolean isNew) {
        if (isNew) {
            resolve(style);
        }
        checkNotEmpty(style.getName(), "Style name must not be null");
        checkNotEmpty(style.getFilename(), "Style fileName must not be null");

        WorkspaceInfo ws = style.getWorkspace();
        StyleInfo existing = catalog.getStyleByName(ws, style.getName());
        if (existing != null && (isNew || !existing.getId().equals(style.getId()))) {
            // null workspace can cause style in any workspace to be returned, check that
            // workspaces match
            WorkspaceInfo ews = existing.getWorkspace();
            String msg = "Style named '" + style.getName() + "' already exists";
            if (ews != null) {
                msg += " in workspace " + ews.getName();
            }
            throw new IllegalArgumentException(msg);
        }

        if (!isNew) {
            StyleInfo current = catalog.getStyle(style.getId());

            // Default style validation
            if (isDefaultStyle(current)) {
                checkArgument(
                        current.getName().equals(style.getName()), "Cannot rename default styles");
                checkArgument(
                        null == style.getWorkspace(),
                        "Cannot change the workspace of default styles");
            }
        }

        return postValidate(style, isNew);
    }

    public void beforeRemove(WorkspaceInfo workspace) {
        // JD: maintain the link between namespace and workspace, remove this when this is no
        // longer necessary
        if (catalog.getNamespaceByPrefix(workspace.getName()) != null) {
            throw new IllegalArgumentException("Cannot delete workspace with linked namespace");
        }
        if (!catalog.getStoresByWorkspace(workspace, StoreInfo.class).isEmpty()) {
            throw new IllegalArgumentException("Cannot delete non-empty workspace.");
        }
    }

    public void beforeRemove(NamespaceInfo namespace) {
        // REVISIT: horrible performance, fetching all resources in memory
        if (!catalog.getResourcesByNamespace(namespace, ResourceInfo.class).isEmpty()) {
            throw new IllegalArgumentException("Unable to delete non-empty namespace.");
        }
    }

    public void beforeRemove(StoreInfo store) {
        // REVISIT: horrible performance, fetching all resources in memory
        if (!catalog.getResourcesByStore(store, ResourceInfo.class).isEmpty()) {
            throw new IllegalArgumentException("Unable to delete non-empty store.");
        }
    }

    public void beforeRemove(ResourceInfo resource) {
        // ensure no references to the resource
        if (!catalog.getLayers(resource).isEmpty()) {
            throw new IllegalArgumentException("Unable to delete resource referenced by layer");
        }
    }

    public void beforeRemove(LayerInfo layer) {
        // ensure no references to the layer
        for (LayerGroupInfo lg : catalog.getLayerGroups()) {
            if (lg.getLayers().contains(layer) || layer.equals(lg.getRootLayer())) {
                String msg =
                        "Unable to delete layer referenced by layer group '" + lg.getName() + "'";
                throw new IllegalArgumentException(msg);
            }
        }
    }

    public void beforeRemove(LayerGroupInfo layerGroup) {
        // ensure no references to the layer group
        // REVISIT: should recurse into nested layer groups
        for (LayerGroupInfo lg : catalog.getLayerGroups()) {
            if (lg.getLayers().contains(layerGroup)) {
                String msg =
                        "Unable to delete layer group referenced by layer group '"
                                + lg.getName()
                                + "'";
                throw new IllegalArgumentException(msg);
            }
        }
    }

    public void beforeRemove(StyleInfo style) {
        // ensure no references to the style
        List<LayerInfo> layers = catalog.getLayers(style);
        if (!layers.isEmpty()) {
            throw new IllegalArgumentException(
                    "Unable to delete style referenced by '" + layers.get(0).getName() + "'");
        }
        // REVISIT: what about layer groups per workspace?
        for (LayerGroupInfo lg : catalog.getLayerGroups()) {
            if (lg.getStyles().contains(style) || style.equals(lg.getRootLayerStyle())) {
                String msg =
                        "Unable to delete style referenced by layer group '" + lg.getName() + "'";
                throw new IllegalArgumentException(msg);
            }
        }
        checkArgument(!isDefaultStyle(style), "Unable to delete a default style");
    }

    public void beforeRemove(MapInfo map) {
        // no-op
    }

    private boolean isDefaultStyle(StyleInfo s) {
        return s.getWorkspace() == null
                && (StyleInfo.DEFAULT_POINT.equals(s.getName())
                        || StyleInfo.DEFAULT_LINE.equals(s.getName())
                        || StyleInfo.DEFAULT_POLYGON.equals(s.getName())
                        || StyleInfo.DEFAULT_RASTER.equals(s.getName())
                        || StyleInfo.DEFAULT_GENERIC.equals(s.getName()));
    }

    static void validateKeywords(List<KeywordInfo> keywords) {
        if (keywords != null) {
            for (KeywordInfo kw : keywords) {
                Matcher m = KeywordInfo.RE.matcher(kw.getValue());
                if (!m.matches()) {
                    throw new IllegalArgumentException(
                            "Illegal keyword '"
                                    + kw
                                    + "'. "
                                    + "Keywords must not be empty and must not contain the '\\' character");
                }
                if (kw.getVocabulary() != null) {
                    m = KeywordInfo.RE.matcher(kw.getVocabulary());
                    if (!m.matches()) {
                        throw new IllegalArgumentException(
                                "Keyword vocbulary must not contain the '\\' character");
                    }
                }
            }
        }
    }

    protected void checkNotNull(Object value, String message, Object... messageArgs) {
        if (value == null) {
            throw new NullPointerException(format(message, messageArgs));
        }
    }

    protected void checkArgument(boolean condition, String message, Object... messageArgs) {
        if (!condition) {
            throw new IllegalArgumentException(format(message, messageArgs));
        }
    }

    protected void checkNotEmpty(String value, String message, Object... messageArgs) {
        checkNotNull(value, message, messageArgs);
        checkArgument(!value.isEmpty(), message, messageArgs);
    }

    protected boolean isNullOrEmpty(String string) {
        return string == null || "".equals(string.trim());
    }

    private ValidationResult postValidate(CatalogInfo info, boolean isNew) {
        List<RuntimeException> errors = new ArrayList<RuntimeException>();

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

    static class CatalogValidatorVisitor implements CatalogVisitor {

        CatalogValidator validator;
        boolean isNew;

        CatalogValidatorVisitor(CatalogValidator validator, boolean isNew) {
            this.validator = validator;
            this.isNew = isNew;
        }

        public @Override void visit(Catalog catalog) {}

        public @Override void visit(WorkspaceInfo workspace) {
            validator.validate(workspace, isNew);
        }

        public @Override void visit(NamespaceInfo namespace) {
            validator.validate(namespace, isNew);
        }

        public @Override void visit(DataStoreInfo dataStore) {
            validator.validate(dataStore, isNew);
        }

        public @Override void visit(CoverageStoreInfo coverageStore) {
            validator.validate(coverageStore, isNew);
        }

        public @Override void visit(WMSStoreInfo wmsStore) {
            validator.validate(wmsStore, isNew);
        }

        public @Override void visit(WMTSStoreInfo wmtsStore) {
            validator.validate(wmtsStore, isNew);
        }

        public @Override void visit(FeatureTypeInfo featureType) {
            validator.validate(featureType, isNew);
        }

        public @Override void visit(CoverageInfo coverage) {
            validator.validate(coverage, isNew);
        }

        public @Override void visit(LayerInfo layer) {
            validator.validate(layer, isNew);
        }

        public @Override void visit(StyleInfo style) {
            validator.validate(style, isNew);
        }

        public @Override void visit(LayerGroupInfo layerGroup) {
            validator.validate(layerGroup, isNew);
        }

        public @Override void visit(WMSLayerInfo wmsLayer) {
            validator.validate(wmsLayer, isNew);
        }

        public @Override void visit(WMTSLayerInfo wmtsLayer) {
            validator.validate(wmtsLayer, isNew);
        }
    }

    protected WorkspaceInfo resolve(WorkspaceInfo workspace) {
        if (catalog.getWorkspaceByName(workspace.getName()) != null) {
            throw new IllegalArgumentException(
                    "Workspace with name '" + workspace.getName() + "' already exists.");
        }

        resolveCollections(workspace);
        return workspace;
    }

    private void resolve(NamespaceInfo namespace) {
        resolveCollections(namespace);
    }

    private void resolve(StoreInfo store) {
        if (store.getWorkspace() == null) {
            store.setWorkspace(catalog.getDefaultWorkspace());
        }

        resolveCollections(store);
        StoreInfoImpl s = (StoreInfoImpl) store;
        s.setCatalog(catalog);
    }

    public void resolve(CatalogInfo info) {
        if (info instanceof LayerGroupInfo) {
            resolve((LayerGroupInfo) info);
        } else if (info instanceof LayerInfo) {
            resolve((LayerInfo) info);
        } else if (info instanceof MapInfo) {
            resolve((MapInfo) info);
        } else if (info instanceof NamespaceInfo) {
            resolve((NamespaceInfo) info);
        } else if (info instanceof ResourceInfo) {
            resolve((ResourceInfo) info);
        } else if (info instanceof StoreInfo) {
            resolve((StoreInfo) info);
        } else if (info instanceof StyleInfo) {
            resolve((StyleInfo) info);
        } else if (info instanceof WorkspaceInfo) {
            resolve((WorkspaceInfo) info);
        } else {
            throw new IllegalArgumentException("Unknown resource type: " + info);
        }
    }

    private void resolve(ResourceInfo resource) {
        ResourceInfoImpl r = (ResourceInfoImpl) resource;
        r.setCatalog(catalog);

        if (resource.getNamespace() == null) {
            // default to default namespace
            resource.setNamespace(catalog.getDefaultNamespace());
        }
        if (resource.getNativeName() == null) {
            resource.setNativeName(resource.getName());
        }

        if (resource instanceof FeatureTypeInfo) {
            resolve((FeatureTypeInfo) resource);
        }
        if (r instanceof CoverageInfo) {
            resolve((CoverageInfo) resource);
        }
        if (r instanceof WMSLayerInfo) {
            resolve((WMSLayerInfo) resource);
        }
        if (r instanceof WMTSLayerInfo) {
            resolve((WMTSLayerInfo) resource);
        }
    }

    private CoverageInfo resolve(CoverageInfo r) {
        CoverageInfoImpl c = (CoverageInfoImpl) r;
        if (c.getDimensions() != null) {
            for (CoverageDimensionInfo dim : c.getDimensions()) {
                if (dim.getNullValues() == null) {
                    ((CoverageDimensionImpl) dim).setNullValues(new ArrayList<Double>());
                }
            }
        }
        resolveCollections(r);
        return r;
    }

    /**
     * We don't want the world to be able and call this without going trough {@link
     * #resolve(ResourceInfo)}
     */
    private void resolve(FeatureTypeInfo featureType) {
        FeatureTypeInfoImpl ft = (FeatureTypeInfoImpl) featureType;
        resolveCollections(ft);
    }

    private WMSLayerInfo resolve(WMSLayerInfo wmsLayer) {
        WMSLayerInfoImpl impl = (WMSLayerInfoImpl) wmsLayer;
        resolveCollections(impl);
        return wmsLayer;
    }

    private WMTSLayerInfo resolve(WMTSLayerInfo wmtsLayer) {
        WMTSLayerInfoImpl impl = (WMTSLayerInfoImpl) wmtsLayer;
        resolveCollections(impl);
        return wmtsLayer;
    }

    private void resolve(LayerInfo layer) {
        if (layer.getAttribution() == null) {
            layer.setAttribution(catalog.getFactory().createAttribution());
        }
        if (layer.getType() == null) {
            if (layer.getResource() instanceof FeatureTypeInfo) {
                layer.setType(PublishedType.VECTOR);
            } else if (layer.getResource() instanceof CoverageInfo) {
                layer.setType(PublishedType.RASTER);
            } else if (layer.getResource() instanceof WMTSLayerInfo) {
                layer.setType(PublishedType.WMTS);
            } else if (layer.getResource() instanceof WMSLayerInfo) {
                layer.setType(PublishedType.WMS);
            } else {
                String msg = "Layer type not set and can't be derived from resource";
                throw new IllegalArgumentException(msg);
            }
        }
        resolveCollections(layer);
    }

    private void resolve(LayerGroupInfo layerGroup) {
        List<StyleInfo> styles = layerGroup.getStyles();
        if (styles.isEmpty()) {
            layerGroup.getLayers().forEach(l -> styles.add(null));
        }
        resolveCollections(layerGroup);
    }

    private void resolve(StyleInfo style) {
        ((StyleInfoImpl) style).setCatalog(catalog);
    }

    private void resolve(MapInfo map) {
        resolveCollections(map);
    }

    /** Method which reflectively sets all collections when they are null. */
    protected void resolveCollections(Object object) {
        OwsUtils.resolveCollections(object);
    }
}
