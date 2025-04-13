/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.catalog.plugin.validation;

import static com.google.common.base.Preconditions.checkArgument;
import static org.geoserver.catalog.Predicates.acceptAll;
import static org.geoserver.catalog.Predicates.equal;
import static org.geoserver.catalog.Predicates.isNull;
import static org.geoserver.catalog.Predicates.or;

import com.google.common.collect.Streams;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.Cleanup;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.plugin.AbstractCatalogVisitor;
import org.geoserver.catalog.util.CloseableIterator;
import org.geotools.api.filter.Filter;

/**
 * Implements validation rules to apply before {@code Catalog.remove(...)}
 *
 * @see CatalogValidationRules#beforeRemove(CatalogInfo)
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
// don't let SonarLint complain about duplicate "workspace.id" literal
@SuppressWarnings("java:S1192")
final class BeforeRemoveValidator extends AbstractCatalogVisitor {

    private final @NonNull Catalog catalog;

    private void checkEmpty(
            Class<? extends CatalogInfo> type, String queryProperty, String queryValue, String errorMessage) {

        Filter filter = equal(queryProperty, queryValue);
        int count = catalog.count(type, filter);
        if (count > 0) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    @Override
    public void visit(WorkspaceInfo workspace) {
        // JD: maintain the link between namespace and workspace, remove this when this is no
        // longer necessary
        if (catalog.getNamespaceByPrefix(workspace.getName()) != null) {
            throw new IllegalArgumentException("Cannot delete workspace with linked namespace");
        }

        checkEmpty(StoreInfo.class, "workspace.id", workspace.getId(), "Cannot delete non-empty workspace.");
    }

    @Override
    public void visit(NamespaceInfo namespace) {
        checkEmpty(ResourceInfo.class, "namespace.id", namespace.getId(), "Unable to delete non-empty namespace.");
    }

    @Override
    protected void visit(StoreInfo store) {
        checkEmpty(ResourceInfo.class, "store.id", store.getId(), "Unable to delete non-empty store.");
    }

    @Override
    protected void visit(ResourceInfo resource) {
        checkEmpty(LayerInfo.class, "resource.id", resource.getId(), "Unable to delete resource referenced by layer");
    }

    @Override
    public void visit(LayerInfo layer) {
        // ensure no references to the layer

        WorkspaceInfo workspace = layer.getResource().getStore().getWorkspace();

        // layers can be referenced by global layer groups or ones on the same workspace
        Filter isGlobal = isNull("workspace.id");
        Filter isSameWorkspace = equal("workspace.id", workspace.getId());
        Filter globalOrSameWorkspace = or(isGlobal, isSameWorkspace);

        final @Cleanup var groups = query(LayerGroupInfo.class, globalOrSameWorkspace);
        groups.forEach(lg -> checkLayerGroupDoesNotContain(lg, layer));
    }

    @Override
    public void visit(LayerGroupInfo toDelete) {
        // ensure no references to the layer group, including nested layer groups
        final @Cleanup var groups = query(LayerGroupInfo.class, acceptAll());
        groups.forEach(lg -> checkLayerGroupDoesNotContain(lg, toDelete));
    }

    @Override
    public void visit(StyleInfo style) {
        // check for default style first, avoid extra computation
        checkArgument(
                !DefaultCatalogValidator.isDefaultStyle(style),
                "Unable to delete default style %s".formatted(style.getName()));

        // ensure no references to the style
        checkNoLayerReferencesStyle(style);

        checkNoLayerGroupReferencesStyle(style);
    }

    private void checkNoLayerReferencesStyle(StyleInfo style) {
        List<LayerInfo> layers = catalog.getLayers(style);
        if (!layers.isEmpty()) {
            String styleName = style.prefixedName();
            String layerNames = layers.stream().map(LayerInfo::prefixedName).collect(Collectors.joining(","));
            throw new IllegalArgumentException(
                    "Unable to delete style %s referenced by layers %s".formatted(styleName, layerNames));
        }
    }

    private void checkNoLayerGroupReferencesStyle(StyleInfo style) {
        final @Cleanup var groups = query(LayerGroupInfo.class, acceptAll());
        groups.forEach(lg -> checkLayerGroupDoesNotContain(lg, style));
    }

    private <T extends CatalogInfo> Stream<T> query(Class<T> type, Filter filter) {
        CloseableIterator<T> iterator = catalog.list(type, filter);
        return Streams.stream(iterator).onClose(iterator::close);
    }

    private void checkLayerGroupDoesNotContain(LayerGroupInfo lg, PublishedInfo toDelete) {

        if (layerGroupContains(lg, toDelete)) {
            String publishedType = toDelete instanceof LayerInfo ? "layer" : "layer group";
            String publishedName = toDelete.prefixedName();
            String lgName = lg.prefixedName();
            throw new IllegalArgumentException("Unable to delete %s %s referenced by layer group '%s'"
                    .formatted(publishedType, publishedName, lgName));
        }
    }

    /**
     * @return {@code true} if {@code lg} contains {@code toDelete}, checking recursively on layer
     *     groups of {@link LayerGroupInfo#getLayers() lg.getLayers()}
     */
    private boolean layerGroupContains(LayerGroupInfo lg, PublishedInfo toDelete) {
        final String publishedId = toDelete.getId();
        LayerInfo rootLayer = lg.getRootLayer();

        boolean referencedByRootLayer = null != rootLayer && rootLayer.getId().equals(publishedId);
        if (referencedByRootLayer) {
            return true;
        }

        for (PublishedInfo child : lg.getLayers()) {
            if (toDelete.getId().equals(child.getId())) {
                return true;
            }
            if (child instanceof LayerGroupInfo childGroup) {
                return layerGroupContains(childGroup, toDelete);
            }
        }
        return false;
    }

    private void checkLayerGroupDoesNotContain(LayerGroupInfo lg, StyleInfo toDelete) {
        String styleId = toDelete.getId();
        StyleInfo rootLayerStyle = lg.getRootLayerStyle();
        boolean rootStyleMatches = null != rootLayerStyle && styleId.equals(rootLayerStyle.getId());

        boolean referenced = rootStyleMatches
                || lg.getStyles().stream()
                        .filter(Objects::nonNull)
                        .map(StyleInfo::getId)
                        .anyMatch(styleId::equals);

        if (referenced) {
            throw new IllegalArgumentException("Unable to delete style %s referenced by layer group '%s'"
                    .formatted(toDelete.prefixedName(), lg.prefixedName()));
        }
    }
}
