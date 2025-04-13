/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.backend.pgconfig.catalog.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.UncheckedIOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.ClassMappings;
import org.geoserver.catalog.impl.LayerInfoImpl;
import org.geoserver.catalog.impl.ModificationProxy;
import org.springframework.jdbc.core.RowMapper;

/**
 * @since 1.4
 */
@Slf4j(topic = "org.geoserver.cloud.backend.pgconfig.catalog.repository.rowmapper")
public final class CatalogInfoRowMapper<T extends CatalogInfo> implements RowMapper<T> {

    protected static final ObjectMapper objectMapper = PgconfigObjectMapper.newObjectMapper();

    /**
     * Columns required by {@link #mapNamespace(ResultSet)}
     *
     * <pre>{@code
     *    Column        |   Type   |
     * -----------------+----------+
     * namespace        | jsonb    |
     * }</pre>
     */
    static final String NAMESPACE_BUILD_COLUMNS = "\"@type\", namespace";

    /**
     * Columns required by {@link #mapWorkspace(ResultSet)}
     *
     * <pre>{@code
     *    Column        |   Type   |
     * ----------------------+------
     * workspace        | jsonb    |
     * }</pre>
     */
    static final String WORKSPACE_BUILD_COLUMNS = "\"@type\", workspace";

    /**
     * Columns required by {@link #mapStore(ResultSet)}
     *
     * <pre>{@code
     *    Column        |   Type   |
     * -----------------+----------+
     * store            | jsonb    |
     * workspace        | jsonb    |
     * }</pre>
     */
    static final String STOREINFO_BUILD_COLUMNS = "\"@type\", store, workspace";

    /**
     * Columns required by {@link #mapStyle(ResultSet)}
     *
     * <pre>{@code
     *    Column        |   Type   |
     * -----------------+----------+
     * style            | jsonb    |
     * workspace        | jsonb    |
     * }</pre>
     */
    static final String STYLEINFO_BUILD_COLUMNS = "\"@type\", style, workspace";

    /**
     * Columns required by {@link #mapResource(ResultSet)}
     *
     * <pre>{@code
     *    Column        |   Type   |
     * -----------------+----------+
     * resource         | jsonb    |
     * store            | jsonb    |
     * workspace        | jsonb    |
     * namespace        | jsonb    |
     * }</pre>
     */
    static final String RESOURCEINFO_BUILD_COLUMNS = "\"@type\", resource, store, workspace, namespace";

    /**
     * Columns required by {@link #mapPublishedInfo(ResultSet)}
     *
     * <pre>{@code
     *    Column        |   Type   |
     * ----------------------+------
     * &#64;type        | text     |
     * publishedinfo    | jsonb    |
     * workspace        | jsonb    |
     * resource         | jsonb    |
     * store            | jsonb    |
     * namespace        | jsonb    |
     * defaultStyle     | jsonb    |
     * }</pre>
     */
    static final String PUBLISHEDINFO_BUILD_COLUMNS =
            "\"@type\", publishedinfo, workspace, resource, store, namespace, \"defaultStyle\"";

    /**
     * Columns required by {@link #mapLayer(ResultSet)}
     *
     * <pre>{@code
     *    Column        |   Type   |
     * ----------------------+------
     * publishedinfo    | jsonb    |
     * resource         | jsonb    |
     * store            | jsonb    |
     * workspace        | jsonb    |
     * namespace        | jsonb    |
     * defaultStyle     | jsonb    |
     * }</pre>
     */
    static final String LAYERINFO_BUILD_COLUMNS =
            "\"@type\", publishedinfo, resource, store, workspace, namespace, \"defaultStyle\"";

    /**
     * Columns required by {@link #mapLayerGroup(ResultSet)}
     *
     * <pre>{@code
     *    Column        |   Type   |
     * ----------------------+------
     * publishedinfo    | jsonb    |
     * workspace        | jsonb    |
     * }</pre>
     */
    static final String LAYERGROUPINFO_BUILD_COLUMNS = "\"@type\", publishedinfo, workspace";

    /** Lazily created by {@link #cache()} */
    private Map<Class<?>, Map<String, CatalogInfo>> cache;

    private Function<String, Optional<StyleInfo>> styleLoader;

    private CatalogInfoRowMapper() {
        // private constructor
    }

    public CatalogInfoRowMapper<T> setStyleLoader(Function<String, Optional<StyleInfo>> styleLoader) {
        this.styleLoader = styleLoader;
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T mapRow(ResultSet rs, int rowNum) throws SQLException {
        final String type = rs.getString("@type");
        return switch (type) {
            case "NamespaceInfo" -> (T) mapNamespace(rs);
            case "WorkspaceInfo" -> (T) mapWorkspace(rs);
            case "DataStoreInfo", "CoverageStoreInfo", "WMSStoreInfo", "WMTSStoreInfo" -> (T) mapStore(rs);
            case "FeatureTypeInfo", "CoverageInfo", "WMSLayerInfo", "WMTSLayerInfo" -> (T) mapResource(rs);
            case "LayerInfo" -> (T) mapLayer(rs);
            case "LayerGroupInfo" -> (T) mapLayerGroup(rs);
            case "StyleInfo" -> (T) mapStyle(rs);
            default -> throw new IllegalArgumentException("Unexpected value: " + type);
        };
    }

    @SuppressWarnings("unchecked")
    private <C extends CatalogInfo> Map<String, C> cache(Class<C> clazz) {
        if (cache == null) {
            cache = new HashMap<>();
        }
        return (Map<String, C>) cache.computeIfAbsent(clazz, c -> new LRUCache<>(100));
    }

    protected <C extends CatalogInfo> C resolveCached(
            String id, Class<C> clazz, ResultSet rs, Function<ResultSet, C> loader) {

        return resolveCached(id, clazz, idd -> loader.apply(rs));
    }

    protected <C extends CatalogInfo> C resolveCached(String id, Class<C> clazz, Function<String, C> loader) {
        if (null == id) {
            return null;
        }
        var infoCache = cache(clazz);
        C info = infoCache.get(id);
        if (clazz.isInstance(info)) {
            log.trace("loaded from RowMapper cache: {}", info);
        } else {
            info = loader.apply(id);
            infoCache.put(id, info);
            log.trace("RowMapper cached {}", info);
        }
        return info;
    }

    /**
     * {@link RowMapper} function for {@link WorkspaceInfo}
     *
     * <p>
     * Expects the following columns:
     *
     * <pre>{@code
     *    Column        |   Type   |
     * ----------------------+------
     * workspace        | jsonb    |
     * }</pre>
     */
    WorkspaceInfo mapWorkspace(ResultSet rs) {
        try {
            return decode(rs, "workspace", WorkspaceInfo.class);
        } catch (SQLException e) {
            throw UncheckedSqlException.of(e);
        }
    }

    protected WorkspaceInfo mapWorkspace(String id, ResultSet rs) {
        return resolveCached(id, WorkspaceInfo.class, rs, k -> mapWorkspace(rs));
    }

    /**
     * Expects the following columns:
     *
     * <pre>{@code
     *    Column        |   Type   |
     * -----------------+----------+
     * namespace        | jsonb    |
     * }</pre>
     */
    NamespaceInfo mapNamespace(ResultSet rs) {
        try {
            return decode(rs, "namespace", NamespaceInfo.class);
        } catch (SQLException e) {
            throw UncheckedSqlException.of(e);
        }
    }

    protected NamespaceInfo mapNamespace(String id, ResultSet rs) {
        return resolveCached(id, NamespaceInfo.class, rs, this::mapNamespace);
    }

    /**
     * Expects the following columns:
     *
     * <pre>{@code
     *    Column        |   Type   |
     * -----------------+----------+
     * style            | jsonb    |
     * workspace        | jsonb    |
     * }</pre>
     */
    StyleInfo mapStyle(ResultSet rs) {
        return loadStyle(rs, "style");
    }

    protected StyleInfo loadStyle(ResultSet rs, String columnName) {
        StyleInfo style;
        try {
            style = decode(rs.getString(columnName), StyleInfo.class);
        } catch (SQLException e) {
            throw UncheckedSqlException.of(e);
        }
        WorkspaceInfo workspace = null == style ? null : style.getWorkspace();
        if (null != workspace) {
            String wsid = workspace.getId();
            WorkspaceInfo ws = mapWorkspace(wsid, rs);
            style.setWorkspace(ModificationProxy.create(ws, WorkspaceInfo.class));
        }
        return style;
    }

    protected StyleInfo mapStyle(String id, ResultSet rs) {
        return resolveCached(id, StyleInfo.class, rs, this::mapStyle);
    }

    protected StyleInfo mapStyle(String id, String column, ResultSet rs) {
        return resolveCached(id, StyleInfo.class, rs, r -> loadStyle(r, column));
    }

    /**
     * Expects the following columns:
     *
     * <pre>{@code
     *    Column        |   Type   |
     * -----------------+----------+
     * store            | jsonb    |
     * workspace        | jsonb    |
     * }</pre>
     */
    StoreInfo mapStore(ResultSet rs) {
        StoreInfo store;
        try {
            store = decode(rs.getString("store"), StoreInfo.class);
        } catch (SQLException e) {
            throw UncheckedSqlException.of(e);
        }
        if (null != store) {
            String wsid = store.getWorkspace().getId();
            WorkspaceInfo ws = mapWorkspace(wsid, rs);
            store.setWorkspace(ModificationProxy.create(ws, WorkspaceInfo.class));
        }
        return store;
    }

    protected StoreInfo mapStore(String id, ResultSet rs) {
        return resolveCached(id, StoreInfo.class, rs, k -> mapStore(rs));
    }

    /**
     * Expects the following columns:
     *
     * <pre>{@code
     *    Column        |   Type   |
     * -----------------+----------+
     * resource         | jsonb    |
     * store            | jsonb    |
     * workspace        | jsonb    |
     * namespace        | jsonb    |
     * }</pre>
     */
    ResourceInfo mapResource(ResultSet rs) {
        ResourceInfo resource;
        try {
            resource = decode(rs.getString("resource"), ResourceInfo.class);
        } catch (SQLException e) {
            throw UncheckedSqlException.of(e);
        }
        if (null != resource) {
            setStore(resource, rs);
            setNamespace(rs, resource);
        }
        return resource;
    }

    protected void setStore(ResourceInfo resource, ResultSet rs) {
        String storeId = resource.getStore().getId();
        StoreInfo store = mapStore(storeId, rs);
        @SuppressWarnings("unchecked")
        Class<? extends StoreInfo> storeType = (Class<? extends StoreInfo>)
                ClassMappings.fromImpl(store.getClass()).getInterface();
        resource.setStore(ModificationProxy.create(store, storeType));
    }

    protected void setNamespace(ResultSet rs, ResourceInfo resource) {
        String nsid = resource.getNamespace().getId();
        NamespaceInfo ns = mapNamespace(nsid, rs);
        resource.setNamespace(ModificationProxy.create(ns, NamespaceInfo.class));
    }

    /**
     * Expects the following columns:
     *
     * <pre>{@code
     *    Column        |   Type   |
     * ----------------------+------
     * &#64;type            | text     |
     * publishedinfo    | jsonb    |
     * workspace        | jsonb    |
     * resource         | jsonb    |
     * store            | jsonb    |
     * namespace        | jsonb    |
     * defaultStyle     | jsonb    |
     * }</pre>
     *
     * @see #mapLayer(ResultSet)
     * @see #mapLayerGroup(ResultSet)
     */
    @SuppressWarnings("unchecked")
    <P extends PublishedInfo> P mapPublishedInfo(ResultSet rs) throws SQLException {
        final String type = rs.getString("@type");
        return switch (type) {
            case "LayerInfo" -> (P) mapLayer(rs);
            case "LayerGroupInfo" -> (P) mapLayerGroup(rs);
            default -> throw new IllegalArgumentException("Unexpected value: " + type);
        };
    }

    /**
     * Expects the following columns:
     *
     * <pre>{@code
     *    Column        |   Type   |
     * ----------------------+------
     * publishedinfo    | jsonb    |
     * resource         | jsonb    |
     * store            | jsonb    |
     * workspace        | jsonb    |
     * namespace        | jsonb    |
     * defaultStyle     | jsonb    |
     * }</pre>
     */
    LayerInfo mapLayer(ResultSet rs) {
        LayerInfo layer;
        try {
            layer = decode(rs.getString("publishedinfo"), LayerInfo.class);
        } catch (SQLException e) {
            throw UncheckedSqlException.of(e);
        }
        if (null != layer) {
            setResource(layer, rs);
            setDefaultStyle(layer, rs);
            setStyles(layer);
        }
        return layer;
    }

    private void setStyles(LayerInfo layer) {
        LayerInfoImpl li = (LayerInfoImpl) ModificationProxy.unwrap(layer);
        List<StyleInfo> styles = li.getStyles().stream()
                .map(StyleInfo::getId)
                .map(this::loadStyle)
                .map(s -> ModificationProxy.create(s, StyleInfo.class))
                .toList();
        li.setStyles(new HashSet<>(styles));
    }

    /**
     * Expects the following columns:
     *
     * <pre>{@code
     *    Column        |   Type   |
     * ----------------------+------
     * publishedinfo    | jsonb    |
     * workspace        | jsonb    |
     * }</pre>
     */
    LayerGroupInfo mapLayerGroup(ResultSet rs) {
        LayerGroupInfo layergroup;
        try {
            layergroup = decode(rs.getString("publishedinfo"), LayerGroupInfo.class);
        } catch (SQLException e) {
            throw UncheckedSqlException.of(e);
        }
        if (null != layergroup) {
            WorkspaceInfo workspace = layergroup.getWorkspace();
            if (null != workspace) {
                String wsid = workspace.getId();
                WorkspaceInfo ws = mapWorkspace(wsid, rs);
                layergroup.setWorkspace(ModificationProxy.create(ws, WorkspaceInfo.class));
            }
        }
        return layergroup;
    }

    private StyleInfo loadStyle(String id) {
        Objects.requireNonNull(styleLoader, "styleLoader is null");
        Function<String, StyleInfo> function = styleLoader.andThen(opt -> opt.orElse(null));
        return resolveCached(id, StyleInfo.class, function);
    }

    protected LayerInfo mapLayer(String id, ResultSet rs) {
        return resolveCached(id, LayerInfo.class, rs, this::mapLayer);
    }

    private void setResource(LayerInfo layer, ResultSet rs) {
        // ResourceInfos are not cached, they can only be mapped directly or when
        // resolving a layerinfo. In the later,
        // the relationship is 1:1 so caching them would be vane
        ResourceInfo resource = mapResource(rs);
        layer.setResource(ModificationProxy.create(resource, ResourceInfo.class));
    }

    private void setDefaultStyle(LayerInfo layer, ResultSet rs) {
        StyleInfo defaultStyle = layer.getDefaultStyle();
        if (null != defaultStyle) {
            String styleId = defaultStyle.getId();
            defaultStyle = mapStyle(styleId, "defaultStyle", rs);
            if (null == defaultStyle) {
                layer.setDefaultStyle(null);
            } else {
                layer.setDefaultStyle(ModificationProxy.create(defaultStyle, StyleInfo.class));
            }
        }
    }

    protected <V> V decode(ResultSet rs, String column, Class<V> valueType) throws SQLException {
        return decode(rs.getString(column), valueType);
    }

    protected <V> V decode(String encoded, Class<V> valueType) {
        if (null == encoded) {
            return null;
        }
        try {
            return objectMapper.readValue(encoded, valueType);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static <I extends CatalogInfo> CatalogInfoRowMapper<I> newInstance() {
        return new CatalogInfoRowMapper<>();
    }
}
