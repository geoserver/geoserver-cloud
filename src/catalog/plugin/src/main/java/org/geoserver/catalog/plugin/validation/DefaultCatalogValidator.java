/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin.validation;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
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
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.KeywordInfo;
import org.geoserver.catalog.LayerGroupHelper;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerGroupInfo.Mode;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.SLDNamedLayerValidator;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.plugin.CatalogPlugin;
import org.geotools.api.style.StyledLayerDescriptor;
import org.geotools.util.logging.Logging;

/** Default validation rules for {@link CatalogInfo} pre-add and pre-modify states */
public class DefaultCatalogValidator implements CatalogValidator {
    private static final Logger LOGGER = Logging.getLogger(DefaultCatalogValidator.class);

    private Catalog catalog;
    private DefaultPropertyValuesResolver newObjectPropertiesResolver;

    public DefaultCatalogValidator(Catalog catalog) {
        Objects.requireNonNull(catalog);
        this.catalog = catalog;
        this.newObjectPropertiesResolver = new DefaultPropertyValuesResolver(catalog);
    }

    @Override
    public void validate(WorkspaceInfo workspace, boolean isNew) {
        checkNotEmpty(workspace.getName(), "workspace name must not be null");
        checkArgument(
                !Catalog.DEFAULT.equals(workspace.getName()),
                "%s is a reserved keyword, can't be used as the workspace name",
                Catalog.DEFAULT);

        if (isNew) {
            newObjectPropertiesResolver.resolve(workspace);
        }
        checkArgument(
                !workspace.isIsolated() || catalog.getCatalogCapabilities().supportsIsolatedWorkspaces(),
                "Workspace '%s' is isolated but isolated workspaces are not supported by this catalog.",
                workspace.getName());

        WorkspaceInfo existing = catalog.getWorkspaceByName(workspace.getName());
        checkArgument(
                existing == null || existing.getId().equals(workspace.getId()),
                "Workspace named '%s' already exists.",
                workspace.getName());
    }

    @Override
    public void validate(NamespaceInfo namespace, boolean isNew) {
        checkNotEmpty(namespace.getPrefix(), "Namespace prefix must not be null");
        checkNotEmpty(namespace.getURI(), "Namespace uri must not be null");
        if (isNew) {
            newObjectPropertiesResolver.resolve(namespace);
        }
        if (namespace.isIsolated() && !catalog.getCatalogCapabilities().supportsIsolatedWorkspaces()) {
            // isolated namespaces \ workspaces are not supported by this catalog
            throw new IllegalArgumentException(
                    "Namespace '%s:%s' is isolated but isolated workspaces are not supported by this catalog."
                            .formatted(namespace.getPrefix(), namespace.getURI()));
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
                    "Invalid URI syntax for '%s' in namespace '%s'"
                            .formatted(namespace.getURI(), namespace.getPrefix()),
                    e);
        }
    }

    @Override
    public void validate(StoreInfo store, boolean isNew) {
        if (isNew) {
            newObjectPropertiesResolver.resolve(store);
        }
        // note: throwing IAE if null to match CatalogImpl's behavior, though it should be NPE
        checkArgument(null != store.getName(), "Store name must not be null");
        checkNotEmpty(store.getName(), "Store name must not be empty");
        checkArgument(null != store.getWorkspace(), "Store must be part of a workspace");

        WorkspaceInfo workspace = store.getWorkspace();
        StoreInfo existing = catalog.getStoreByName(workspace, store.getName(), StoreInfo.class);
        if (existing != null && (isNew || !existing.getId().equals(store.getId()))) {
            String msg = "Store '%s' already exists in workspace '%s'".formatted(store.getName(), workspace.getName());
            throw new IllegalArgumentException(msg);
        }
    }

    @Override
    public void validate(ResourceInfo resource, boolean isNew) {
        checkNotEmpty(resource.getName(), "Resource name must not be null");
        if (isNew) {
            newObjectPropertiesResolver.resolve(resource);
        }
        if (isNullOrEmpty(resource.getNativeName())
                && !(resource instanceof CoverageInfo coverage && coverage.getNativeCoverageName() != null)) {
            throw new NullPointerException("Resource native name must not be null");
        }
        checkArgument(null != resource.getStore(), "Resource must be part of a store");
        checkArgument(null != resource.getNamespace(), "Resource must be part of a namespace");

        StoreInfo store = resource.getStore();
        ResourceInfo existing = catalog.getResourceByStore(store, resource.getName(), ResourceInfo.class);
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
    }

    @Override
    public void validate(LayerInfo layer, boolean isNew) {
        final ResourceInfo resource = layer.getResource();
        checkNotNull(resource, "Layer resource must not be null");
        if (isNew) {
            newObjectPropertiesResolver.resolve(layer);
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
        LayerInfo existing = CatalogPlugin.getLayerByName(catalog, prefix, layer.getName());
        checkArgument(
                existing == null || existing.getId().equals(layer.getId()),
                "Layer named '%s' in workspace '%s' already exists.",
                layer.getName(),
                prefix);

        // if the style is missing associate a default one, to avoid breaking WMS
        if (layer.getDefaultStyle() == null) {
            try {
                LOGGER.info(() -> "Layer %s is missing the default style, assigning one automatically"
                        .formatted(layer.prefixedName()));
                StyleInfo style = new CatalogBuilder(catalog).getDefaultStyle(resource);
                layer.setDefaultStyle(style);
            } catch (IOException e) {
                LOGGER.log(
                        Level.WARNING,
                        e,
                        () -> "Layer %s is missing the default style, failed to associate one automatically"
                                .formatted(layer.prefixedName()));
            }
        }

        // clean up eventual dangling references to missing alternate styles
        Set<StyleInfo> styles = layer.getStyles();
        for (Iterator<StyleInfo> it = styles.iterator(); it.hasNext(); ) {
            StyleInfo styleInfo = it.next();
            if (styleInfo == null) {
                it.remove();
            }
        }
    }

    @Override
    public void validate(LayerGroupInfo layerGroup, boolean isNew) {
        checkNotEmpty(layerGroup.getName(), "Layer group name must not be null");
        if (isNew) {
            newObjectPropertiesResolver.resolve(layerGroup);
        }
        verifyDoesntClashWithExisting(layerGroup);

        sanitizeLayersAndLayerStyles(layerGroup);

        checkLayerGroupStyles(layerGroup);

        verifyNoLoopsInStructure(layerGroup);

        // if the layer group has a workspace assigned, ensure that every resource in that layer
        // group lives within the same workspace
        final WorkspaceInfo ws = layerGroup.getWorkspace();
        if (ws != null) {
            checkLayerGroupResourceIsInWorkspace(layerGroup, ws);
        }

        validateModeMatchesStructure(layerGroup);
    }

    private void validateModeMatchesStructure(LayerGroupInfo layerGroup) {
        Mode mode = layerGroup.getMode();
        checkArgument(mode != null, "Layer group mode must not be null");
        if (LayerGroupInfo.Mode.EO.equals(mode)) {
            checkArgument(
                    layerGroup.getRootLayer() != null, "Layer group in mode %s must have a root layer", mode.getName());
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
    }

    private void verifyNoLoopsInStructure(LayerGroupInfo layerGroup) {
        LayerGroupHelper helper = new LayerGroupHelper(layerGroup);
        Stack<LayerGroupInfo> loopPath = helper.checkLoops();
        if (loopPath != null) {
            String loopAsString = helper.getLoopAsString(loopPath);
            throw new IllegalArgumentException("Layer group is in a loop: %s".formatted(loopAsString));
        }
    }

    private void checkLayerGroupStyles(LayerGroupInfo layerGroup) {
        if (layerGroup.getStyles() != null
                && !layerGroup.getStyles().isEmpty()
                && (layerGroup.getStyles().size() != layerGroup.getLayers().size())) {
            throw new IllegalArgumentException("Layer group has different number of styles than layers");
        }
    }

    private void sanitizeLayersAndLayerStyles(LayerGroupInfo layerGroup) {
        // sanitize a bit broken layer references
        List<PublishedInfo> layers = layerGroup.getLayers();
        checkArgument(layers != null && !layers.isEmpty(), "Layer group must not be empty");
        List<StyleInfo> styles = checkSameSize(layers, layerGroup.getStyles());

        final int size = layers.size();
        for (int i = size - 1; i >= 0; i--) {
            PublishedInfo layer = layers.get(i);
            StyleInfo style = styles.get(i);
            if (layer == null && style == null) {
                layers.remove(i);
                styles.remove(i);
            } else if (layer == null) {
                validateStyleGroup(style);
            }
        }
    }

    private void validateStyleGroup(StyleInfo style) {
        // Validate style group
        List<Exception> errors;
        try {
            // validate style groups
            StyledLayerDescriptor sld = style.getSLD();
            errors = SLDNamedLayerValidator.validate(catalog, sld);
        } catch (IOException e) {
            throw new IllegalArgumentException("Error validating style group: %s".formatted(e.getMessage()), e);
        }
        if (!errors.isEmpty()) {
            Exception first = errors.get(0);
            throw new IllegalArgumentException("Invalid style group: %s".formatted(first.getMessage()), first);
        }
    }

    private List<StyleInfo> checkSameSize(List<PublishedInfo> layers, List<StyleInfo> styles) {
        if (styles == null) {
            styles = List.of();
        }
        checkArgument(styles.size() == layers.size(), "Styles must have the same number of elements as layers");
        return styles;
    }

    private void verifyDoesntClashWithExisting(LayerGroupInfo layerGroup) {
        final WorkspaceInfo ws = layerGroup.getWorkspace();
        final LayerGroupInfo existing = catalog.getLayerGroupByName(ws, layerGroup.getName());
        if (existing != null && !existing.getId().equals(layerGroup.getId())) {
            // null workspace can cause layer group in any workspace to be returned, check that
            // workspaces match
            WorkspaceInfo ews = existing.getWorkspace();
            if ((ws == null && ews == null) || (ws != null && ws.equals(ews))) {
                String msg = "Layer group named '%s' already exists".formatted(layerGroup.getName());
                if (ws != null) {
                    msg = "%s in workspace %s".formatted(msg, ws.getName());
                }
                throw new IllegalArgumentException(msg);
            }
        }
    }

    @Override
    public void validate(StyleInfo style, boolean isNew) {
        if (isNew) {
            newObjectPropertiesResolver.resolve(style);
        }
        checkNotEmpty(style.getName(), "Style name must not be null");
        checkNotEmpty(style.getFilename(), "Style fileName must not be null");

        WorkspaceInfo ws = style.getWorkspace();
        StyleInfo existing = catalog.getStyleByName(ws, style.getName());
        if (existing != null && (isNew || !existing.getId().equals(style.getId()))) {
            // null workspace can cause style in any workspace to be returned, check that
            // workspaces match
            WorkspaceInfo ews = existing.getWorkspace();
            String msg = "Style named '%s' already exists".formatted(style.getName());
            if (ews != null) {
                msg = "%s in workspace %s".formatted(msg, ews.getName());
            }
            throw new IllegalArgumentException(msg);
        }

        if (!isNew) {
            StyleInfo current = catalog.getStyle(style.getId());

            // Default style validation
            if (isDefaultStyle(current)) {
                checkArgument(current.getName().equals(style.getName()), "Cannot rename default styles");
                checkArgument(null == style.getWorkspace(), "Cannot change the workspace of default styles");
            }
        }
    }

    static void checkNotNull(Object value, String message, Object... messageArgs) {
        if (value == null) {
            throw new NullPointerException(message.formatted(messageArgs));
        }
    }

    static void checkArgument(boolean condition, String message, Object... messageArgs) {
        if (!condition) {
            throw new IllegalArgumentException(message.formatted(messageArgs));
        }
    }

    static void checkNotEmpty(String value, String message, Object... messageArgs) {
        checkNotNull(value, message, messageArgs);
        checkArgument(!value.isEmpty(), message, messageArgs);
    }

    static boolean isNullOrEmpty(String string) {
        return string == null || "".equals(string.trim());
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
                if (p instanceof LayerGroupInfo lg) {
                    checkLayerGroupResourceIsInWorkspace(lg, ws);
                } else if (p instanceof LayerInfo l) {
                    checkLayerGroupResourceIsInWorkspace(l, ws);
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
                    "Layer group within a workspace (%s) can not contain styles from other workspace: %s"
                            .formatted(ws.getName(), style.getWorkspace()));
        }
    }

    private void checkLayerGroupResourceIsInWorkspace(LayerInfo layer, WorkspaceInfo ws) {
        if (layer == null) return;

        ResourceInfo r = layer.getResource();
        if (r.getStore().getWorkspace() != null && !ws.equals(r.getStore().getWorkspace())) {
            throw new IllegalArgumentException(
                    "Layer group within a workspace (%s) can not contain resources from other workspace: %s"
                            .formatted(ws.getName(), r.getStore().getWorkspace().getName()));
        }
    }

    static boolean isDefaultStyle(StyleInfo s) {
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
                            "Illegal keyword '%s'. Keywords must not be empty and must not contain the '\\' character"
                                    .formatted(kw));
                }
                if (kw.getVocabulary() != null) {
                    m = KeywordInfo.RE.matcher(kw.getVocabulary());
                    if (!m.matches()) {
                        throw new IllegalArgumentException("Keyword vocbulary must not contain the '\\' character");
                    }
                }
            }
        }
    }
}
