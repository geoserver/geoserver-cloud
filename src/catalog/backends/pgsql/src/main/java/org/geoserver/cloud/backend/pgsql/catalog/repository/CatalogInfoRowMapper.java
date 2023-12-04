/*
 * (c) 2023 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.backend.pgsql.catalog.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Setter;

import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.ClassMappings;
import org.geoserver.catalog.impl.LayerInfoImpl;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geotools.jackson.databind.util.ObjectMapperUtil;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * @since 1.4
 */
class CatalogInfoRowMapper {

    protected static final ObjectMapper infoMapper = ObjectMapperUtil.newObjectMapper();

    // TODO: limit the amount of cached objects
    protected Map<String, CatalogInfo> cache = new HashMap<>();

    protected @Setter Function<String, Optional<StyleInfo>> styleLoader;

    protected <T extends CatalogInfo> T resolveCached(
            String id, Class<T> clazz, ResultSet rs, Function<ResultSet, T> loader) {

        return resolveCached(id, clazz, idd -> loader.apply(rs));
    }

    protected <T extends CatalogInfo> T resolveCached(
            String id, Class<T> clazz, Function<String, T> loader) {
        if (null == id) return null;
        CatalogInfo info = cache.get(id);
        if (!clazz.isInstance(info)) {
            info = loader.apply(id);
            cache.put(id, info);
        }
        return clazz.cast(info);
    }

    /**
     * {@link RowMapper} function for {@link WorkspaceInfo}
     *
     * <p>Expects the following columns:
     *
     * <pre>{@code
     *    Column        |   Type   | Collation | Nullable | Default
     * ----------------------+----------+-----------+----------+---------
     * workspace        | jsonb    |           |          |
     * }</pre>
     */
    public WorkspaceInfo mapWorkspace(ResultSet rs, int rowNum) throws SQLException {
        try {
            return mapWorkspace(rs);
        } catch (UncheckedSqlException e) {
            throw e.getCause();
        }
    }

    protected WorkspaceInfo mapWorkspace(ResultSet rs) {
        try {
            return decode(rs, "workspace", WorkspaceInfo.class);
        } catch (SQLException e) {
            throw UncheckedSqlException.of(e);
        }
    }

    protected WorkspaceInfo mapWorkspace(String id, ResultSet rs) {
        return resolveCached(id, WorkspaceInfo.class, rs, this::mapWorkspace);
    }

    /**
     * Expects the following columns:
     *
     * <pre>{@code
     *    Column        |   Type   | Collation | Nullable | Default
     * ----------------------+----------+-----------+----------+---------
     * namespace        | jsonb    |           |          |
     * }</pre>
     */
    public NamespaceInfo mapNamespace(ResultSet rs, int rowNum) throws SQLException {
        try {
            return mapNamespace(rs);
        } catch (UncheckedSqlException e) {
            throw e.getCause();
        }
    }

    protected NamespaceInfo mapNamespace(ResultSet rs) {
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
     *    Column        |   Type   | Collation | Nullable | Default
     * ----------------------+----------+-----------+----------+---------
     * style            | jsonb    |           |          |
     * workspace        | jsonb    |           |          |
     * }</pre>
     */
    public StyleInfo mapStyle(ResultSet rs, int rowNum) throws SQLException {
        try {
            return loadStyle(rs);
        } catch (UncheckedSqlException e) {
            throw e.getCause();
        }
    }

    protected StyleInfo loadStyle(ResultSet rs) {
        return loadStyle(rs, "style");
    }

    protected StyleInfo loadStyle(ResultSet rs, String columnName) {
        StyleInfo style;
        try {
            style = decode(rs.getString(columnName), StyleInfo.class);
        } catch (SQLException e) {
            throw UncheckedSqlException.of(e);
        }
        WorkspaceInfo workspace = style.getWorkspace();
        if (null != workspace) {
            String wsid = workspace.getId();
            WorkspaceInfo ws = mapWorkspace(wsid, rs);
            style.setWorkspace(ModificationProxy.create(ws, WorkspaceInfo.class));
        }
        return style;
    }

    protected StyleInfo mapStyle(String id, ResultSet rs) {
        return resolveCached(id, StyleInfo.class, rs, this::loadStyle);
    }

    protected StyleInfo mapStyle(String id, String column, ResultSet rs) {
        return resolveCached(id, StyleInfo.class, rs, r -> loadStyle(r, column));
    }

    /**
     * Expects the following columns:
     *
     * <pre>{@code
     *    Column        |   Type   | Collation | Nullable | Default
     * ----------------------+----------+-----------+----------+---------
     * store            | jsonb    |           |          |
     * workspace        | jsonb    |           |          |
     * }</pre>
     */
    public StoreInfo mapStore(ResultSet rs, int rowNum) throws SQLException {
        try {
            return mapStore(rs);
        } catch (UncheckedSqlException e) {
            throw e.getCause();
        }
    }

    protected StoreInfo mapStore(ResultSet rs) {
        StoreInfo store;
        try {
            store = decode(rs.getString("store"), StoreInfo.class);
        } catch (SQLException e) {
            throw UncheckedSqlException.of(e);
        }
        String wsid = store.getWorkspace().getId();
        WorkspaceInfo ws = mapWorkspace(wsid, rs);
        store.setWorkspace(ModificationProxy.create(ws, WorkspaceInfo.class));
        return store;
    }

    protected StoreInfo mapStore(String id, ResultSet rs) {
        return resolveCached(id, StoreInfo.class, rs, this::mapStore);
    }

    /**
     * Expects the following columns:
     *
     * <pre>{@code
     *    Column        |   Type   | Collation | Nullable | Default
     * ----------------------+----------+-----------+----------+---------
     * resource         | jsonb    |           |          |
     * store            | jsonb    |           |          |
     * workspace        | jsonb    |           |          |
     * namespace        | jsonb    |           |          |
     * }</pre>
     */
    public ResourceInfo mapResource(ResultSet rs, int rowNum) throws SQLException {
        try {
            return mapResource(rs);
        } catch (UncheckedSqlException e) {
            throw e.getCause();
        }
    }

    public ResourceInfo mapResource(ResultSet rs) {
        ResourceInfo resource;
        try {
            resource = decode(rs.getString("resource"), ResourceInfo.class);
        } catch (SQLException e) {
            throw UncheckedSqlException.of(e);
        }
        setStore(resource, rs);
        setNamespace(rs, resource);
        return resource;
    }

    public ResourceInfo mapResource(String id, ResultSet rs) {
        return resolveCached(id, ResourceInfo.class, rs, this::mapResource);
    }

    protected void setStore(ResourceInfo resource, ResultSet rs) {
        String storeId = resource.getStore().getId();
        StoreInfo store = mapStore(storeId, rs);
        @SuppressWarnings("unchecked")
        Class<? extends StoreInfo> storeType =
                (Class<? extends StoreInfo>)
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
     *    Column        |   Type   | Collation | Nullable | Default
     * ----------------------+----------+-----------+----------+---------
     * publishedinfo    | jsonb    |           |          |
     * resource         | jsonb    |           |          |
     * store            | jsonb    |           |          |
     * workspace        | jsonb    |           |          |
     * namespace        | jsonb    |           |          |
     * defaultStyle     | jsonb    |           |          |
     * }</pre>
     */
    public LayerInfo mapLayer(ResultSet rs, int rowNum) throws SQLException {
        try {
            return mapLayer(rs);
        } catch (UncheckedSqlException e) {
            throw e.getCause();
        }
    }

    protected LayerInfo mapLayer(ResultSet rs) {
        LayerInfo layer;
        try {
            layer = decode(rs.getString("publishedinfo"), LayerInfo.class);
        } catch (SQLException e) {
            throw UncheckedSqlException.of(e);
        }
        setResource(layer, rs);
        setDefaultStyle(layer, rs);
        setStyles(layer);
        return layer;
    }

    private void setStyles(LayerInfo layer) {
        LayerInfoImpl li = (LayerInfoImpl) ModificationProxy.unwrap(layer);
        List<StyleInfo> styles =
                li.getStyles().stream()
                        .map(StyleInfo::getId)
                        .map(this::loadStyle)
                        .map(s -> ModificationProxy.create(s, StyleInfo.class))
                        .toList();
        li.setStyles(new HashSet<>(styles));
    }

    private StyleInfo loadStyle(String id) {
        Function<String, StyleInfo> function = styleLoader.andThen(opt -> opt.orElse(null));
        return resolveCached(id, StyleInfo.class, function);
    }

    protected LayerInfo mapLayer(String id, ResultSet rs) {
        return resolveCached(id, LayerInfo.class, rs, this::mapLayer);
    }

    private void setResource(LayerInfo layer, ResultSet rs) {
        String resourceId = layer.getResource().getId();
        ResourceInfo resource = mapResource(resourceId, rs);
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

    /**
     * Expects the following columns:
     *
     * <pre>{@code
     *    Column        |   Type   | Collation | Nullable | Default
     * ----------------------+----------+-----------+----------+---------
     * publishedinfo    | jsonb    |           |          |
     * workspace        | jsonb    |           |          |
     * }</pre>
     */
    public LayerGroupInfo mapLayerGroup(ResultSet rs, int rowNum) throws SQLException {
        try {
            return mapLayerGroup(rs);
        } catch (UncheckedSqlException e) {
            throw e.getCause();
        }
    }

    protected LayerGroupInfo mapLayerGroup(ResultSet rs) {
        LayerGroupInfo layergroup;
        try {
            layergroup = decode(rs.getString("publishedinfo"), LayerGroupInfo.class);
        } catch (SQLException e) {
            throw UncheckedSqlException.of(e);
        }
        WorkspaceInfo workspace = layergroup.getWorkspace();
        if (null != workspace) {
            String wsid = workspace.getId();
            WorkspaceInfo ws = mapWorkspace(wsid, rs);
            layergroup.setWorkspace(ModificationProxy.create(ws, WorkspaceInfo.class));
        }
        return layergroup;
    }

    protected <V> V decode(ResultSet rs, String column, Class<V> valueType) throws SQLException {
        return decode(rs.getString(column), valueType);
    }

    protected <V> V decode(String encoded, Class<V> valueType) {
        try {
            return null == encoded ? null : infoMapper.readValue(encoded, valueType);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static RowMapper<WorkspaceInfo> workspace() {
        return new CatalogInfoRowMapper()::mapWorkspace;
    }

    public static RowMapper<NamespaceInfo> namespace() {
        return new CatalogInfoRowMapper()::mapNamespace;
    }

    public static RowMapper<StoreInfo> store() {
        return new CatalogInfoRowMapper()::mapStore;
    }

    public static RowMapper<ResourceInfo> resource() {
        return new CatalogInfoRowMapper()::mapResource;
    }

    public static RowMapper<StyleInfo> style() {
        return new CatalogInfoRowMapper()::mapStyle;
    }

    public static RowMapper<LayerInfo> layer(Function<String, Optional<StyleInfo>> styleLoader) {
        CatalogInfoRowMapper mapper = new CatalogInfoRowMapper();
        mapper.setStyleLoader(styleLoader);
        return mapper::mapLayer;
    }

    public static RowMapper<LayerGroupInfo> layerGroup() {
        return new CatalogInfoRowMapper()::mapLayerGroup;
    }
}
