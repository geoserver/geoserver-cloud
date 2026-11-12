/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.backend.pgconfig.catalog.repository;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.catalog.AttributeTypeInfo;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.LayerInfoImpl;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.cloud.backend.pgconfig.cache.CacheEntry;
import org.geoserver.cloud.backend.pgconfig.cache.CacheKey;
import org.geoserver.cloud.backend.pgconfig.cache.CatalogInfoKeys;
import org.geoserver.cloud.event.info.ConfigInfoType;
import org.geoserver.cloud.event.info.InfoEvent;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.RowMapper;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** @since 1.4 */
@Slf4j(topic = "org.geoserver.cloud.backend.pgconfig.catalog.repository.rowmapper")
@SuppressWarnings("java:S1192")
public final class CatalogInfoRowMapper<T extends CatalogInfo> implements RowMapper<T> {

    protected static final ObjectMapper objectMapper = PgconfigObjectMapper.newObjectMapper();

    /**
     * The styles of a {@code publishedinfos_mat} row as a JSON array of {@code {"style": ..., "workspace": ...}}, or
     * NULL when the row has none. The format argument names the {@code "styles.id"} array of the row. Each style is
     * found by its primary key.
     */
    private static final String STYLES_AGGREGATE =
            """
            CASE WHEN cardinality(%1$s) > 0 THEN (\
            SELECT jsonb_agg(jsonb_build_object('style', s.style, 'workspace', s.workspace)) \
            FROM styleinfos s WHERE s.id = ANY(%1$s)\
            ) END\
            """;

    /**
     * Selects the styles of a layer or group, as described in {@link #STYLES_AGGREGATE}. The styles of a layer are its
     * alternate styles, and the styles of a group are its explicit styles. Returning them in the row saves one query
     * per style.
     */
    static final String STYLES_COLUMN = STYLES_AGGREGATE.formatted("\"styles.id\"") + " AS styles";

    /**
     * Selects the alternate styles of a layer like {@link #STYLES_COLUMN}, reading the layer JSON because
     * {@code tilelayers_mat} has no {@code "styles.id"} column.
     */
    public static final String ALTERNATE_STYLES_FROM_JSON_COLUMN =
            """
            CASE WHEN "@type" = 'LayerInfo' AND jsonb_array_length(publishedinfo -> 'styles') > 0 THEN (\
            SELECT jsonb_agg(jsonb_build_object('style', s.style, 'workspace', s.workspace)) \
            FROM styleinfos s WHERE s.id IN (SELECT jsonb_array_elements_text(publishedinfo -> 'styles'))\
            ) END AS styles\
            """;

    /** The styles of a member row in {@link #MEMBERS_COLUMN}, as described in {@link #STYLES_AGGREGATE}. */
    private static final String MEMBER_STYLES = STYLES_AGGREGATE.formatted("m.\"styles.id\"");

    /**
     * Selects the members of a group as a JSON array, or NULL when the row is not a group or has no members. Each
     * element holds the {@code @type} of a member and the JSON objects of its row: {@code publishedinfo},
     * {@code workspace}, {@code namespace}, {@code store}, {@code resource}, {@code defaultStyle} and {@code styles}.
     * The elements come in no particular order: the {@code layers} of the group give the order of its members. Listings
     * do not select it, because building it for each listed group makes a listing of a few hundred groups take close to
     * a second.
     *
     * <p>The subquery names the outer table in full. A bare {@code "layers.id"} would read the column of the member
     * row, and no group would get members.
     */
    static final String MEMBERS_COLUMN =
            """
            CASE WHEN "@type" = 'LayerGroupInfo' AND cardinality(publishedinfos_mat."layers.id") > 0 THEN (\
            SELECT jsonb_agg(jsonb_build_object(\
            '@type', m."@type", 'publishedinfo', m.publishedinfo, 'workspace', m.workspace, \
            'namespace', m.namespace, 'store', m.store, 'resource', m.resource, \
            'defaultStyle', m."defaultStyle", 'styles', %s)) \
            FROM publishedinfos_mat m WHERE m.id = ANY(publishedinfos_mat."layers.id")\
            ) END AS members\
            """
                    .formatted(MEMBER_STYLES);

    /** The label of {@link #TILE_LAYER_COLUMN} in a result set. */
    private static final String TILE_LAYER_LABEL = "tilelayer";

    /**
     * Selects the GWC settings of a layer or group: the JSON of its tile layer, or NULL if it has none. The lookups of
     * layers and groups select it, by id, by name and in bulk, and {@link #decompose(ResultSet)} keeps it next to the
     * object. Listings do not select it.
     *
     * <p>The settings come from {@code tilelayers_mat}, a plain table with one row per tile layer. Its primary key
     * answers each row with one index probe. The same subquery on {@code publishedinfo} would probe the parent table
     * and both of its child tables. Like {@link #MEMBERS_COLUMN}, the subquery names the outer table in full.
     */
    public static final String TILE_LAYER_COLUMN =
            "(SELECT t.tilelayer FROM tilelayers_mat t WHERE t.id = publishedinfos_mat.id) AS " + TILE_LAYER_LABEL;

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
     * styles           | jsonb    |
     * }</pre>
     */
    static final String PUBLISHEDINFO_BUILD_COLUMNS =
            "\"@type\", publishedinfo, workspace, resource, store, namespace, \"defaultStyle\", " + STYLES_COLUMN;

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
     * styles           | jsonb    |
     * }</pre>
     */
    static final String LAYERINFO_BUILD_COLUMNS =
            "\"@type\", publishedinfo, resource, store, workspace, namespace, \"defaultStyle\", " + STYLES_COLUMN;

    /**
     * Columns of a group row: the ones required by {@link #mapLayerGroup(ResultSet)}, and the styles of the group read
     * by {@link #decompose(ResultSet)}.
     *
     * <pre>{@code
     *    Column        |   Type   |
     * ----------------------+------
     * publishedinfo    | jsonb    |
     * workspace        | jsonb    |
     * styles           | jsonb    |
     * }</pre>
     */
    static final String LAYERGROUPINFO_BUILD_COLUMNS = "\"@type\", publishedinfo, workspace, " + STYLES_COLUMN;

    /** Columns of a group row with its members, selected by the lookups of one group. */
    static final String LAYERGROUPINFO_WITH_MEMBERS_COLUMNS = LAYERGROUPINFO_BUILD_COLUMNS + ", " + MEMBERS_COLUMN;

    /**
     * The JSON object columns of a layer row. They are also the fields of the elements of a {@code members} aggregate.
     */
    private static final String[] PUBLISHED_OBJECTS = {
        "publishedinfo", "workspace", "namespace", "store", "resource", "defaultStyle"
    };

    /** Catalog object types by the {@code @type} of their JSON, which is the simple name of their interface. */
    private static final Map<String, ConfigInfoType> TYPES_BY_JSON_TYPE = Arrays.stream(ConfigInfoType.values())
            .collect(Collectors.toMap(type -> type.getType().getSimpleName(), type -> type));

    /** Lazily created by {@link #cache()} */
    private Map<Class<?>, Map<String, CatalogInfo>> cache;

    private Function<String, Optional<StyleInfo>> styleLoader;

    /** The rows split by {@link #decompose(ResultSet)} from the last result set. */
    private @Nullable RowsRead rowsRead;

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
        Map<String, C> infoCache = cache(clazz);
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
     * <p>Expects the following columns:
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
            // the workspace column holds the queried object's workspace, which is not the style's when a layer
            // references a style from another workspace
            WorkspaceInfo rowWorkspace = mapWorkspace(rs);
            if (null != rowWorkspace && Objects.equals(workspace.getId(), rowWorkspace.getId())) {
                style.setWorkspace(rowWorkspace);
            } else if (null != styleLoader) {
                StyleInfo resolved = loadStyle(style.getId());
                return null == resolved ? style : resolved;
            }
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
            store.setWorkspace(ws);
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
        linkAttributes(resource);
        return resource;
    }

    /**
     * Points the attributes of a feature type decoded from JSON to the feature type. The JSON of an attribute holds the
     * id of its feature type, and the id decodes to a proxy.
     */
    static void linkAttributes(@Nullable CatalogInfo info) {
        if (info instanceof FeatureTypeInfo ft && null != ft.getAttributes()) {
            /*
             * Raw back-reference, never a ModificationProxy: materialized graphs may be shared across threads by
             * the caching facade, and a proxy in the graph gets its unsynchronized state maps Java-serialized by
             * ModificationProxyCloner when attributes are deep-cloned, corrupting the stream under concurrency
             */
            for (AttributeTypeInfo att : ft.getAttributes()) {
                att.setFeatureType(ft);
            }
        }
    }

    protected void setStore(ResourceInfo resource, ResultSet rs) {
        String storeId = resource.getStore().getId();
        StoreInfo store = mapStore(storeId, rs);
        resource.setStore(store);
    }

    protected void setNamespace(ResultSet rs, ResourceInfo resource) {
        String nsid = resource.getNamespace().getId();
        NamespaceInfo ns = mapNamespace(nsid, rs);
        resource.setNamespace(ns);
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
     * styles           | jsonb    |
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
     * styles           | jsonb    |
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
            setStyles(layer, rs);
        }
        return layer;
    }

    private void setStyles(LayerInfo layer, ResultSet rs) {
        LayerInfoImpl li = (LayerInfoImpl) ModificationProxy.unwrap(layer);
        Set<StyleInfo> styles = new HashSet<>();
        for (JsonNode entry : readStyles(rs)) {
            styles.add(resolveAlternateStyle(entry));
        }
        li.setStyles(styles);
    }

    /** Reads the {@code styles} column, as an empty array when it is NULL. */
    private JsonNode readStyles(ResultSet rs) {
        String encoded;
        try {
            encoded = rs.getString("styles");
        } catch (SQLException e) {
            throw UncheckedSqlException.of(e);
        }
        if (null == encoded) {
            return objectMapper.createArrayNode();
        }
        return objectMapper.readTree(encoded);
    }

    /** Decodes the style of an entry only when the row mapper has not cached it yet. */
    private StyleInfo resolveAlternateStyle(JsonNode entry) {
        String id = entry.path("style").path("id").asString();
        return resolveCached(id, StyleInfo.class, styleId -> decodeAlternateStyle(entry));
    }

    private StyleInfo decodeAlternateStyle(JsonNode entry) {
        StyleInfo style = decodeTree(entry.get("style"), StyleInfo.class);
        JsonNode workspace = entry.get("workspace");
        if (null != workspace && !workspace.isNull()) {
            style.setWorkspace(decodeTree(workspace, WorkspaceInfo.class));
        }
        return style;
    }

    private static <V> V decodeTree(JsonNode tree, Class<V> type) {
        CatalogInfoDecodes.increment();
        return objectMapper.treeToValue(tree, type);
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
                layergroup.setWorkspace(ws);
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
        layer.setResource(resource);
    }

    private void setDefaultStyle(LayerInfo layer, ResultSet rs) {
        StyleInfo defaultStyle = layer.getDefaultStyle();
        if (null != defaultStyle) {
            String styleId = defaultStyle.getId();
            defaultStyle = mapStyle(styleId, "defaultStyle", rs);
            layer.setDefaultStyle(defaultStyle);
        }
    }

    /**
     * Splits the row at the cursor into cache entries, without decoding catalog objects.
     *
     * <p>Each object of the row gives an {@code ENTITY} entry with its JSON, and a {@code NAME} entry with its id under
     * its prefixed name. The objects are the object of the row and the objects embedded in it: workspace, namespace,
     * store, resource, default style and styles. A group row adds its members with their embedded objects, but not the
     * members of a nested group. An entry takes its type from the {@code @type} of the JSON. Its JSON is the column
     * text returned by the driver, or the parsed tree written back for an object nested in an aggregate column.
     *
     * <p>Names follow {@link InfoEvent#prefixedName(Info)} and are read from the JSON of the row: a layer takes the
     * name of its resource, and an object without a workspace has its bare name. An object gets no {@code NAME} entry,
     * rather than a wrong one, if the row does not hold its workspace, namespace or resource. For example, the default
     * style of a layer may belong to another workspace than the one in the row. An object without a workspace gets none
     * if its name holds a colon, see {@link CatalogInfoKeys#globalName(String)}.
     *
     * <p>A layer or group row also gives a {@code TILE_LAYER} entry with the GWC settings of its object, from the
     * {@link #TILE_LAYER_COLUMN}: the JSON of its tile layer, or an empty entry if it has none. The entry holds the
     * JSON as the row returns it: the catalog does not parse the settings, only GWC reads them.
     *
     * <p>An embedded object gives its entries once per result set: the rows of a listing share their workspace,
     * namespace, store and styles. The {@code members} aggregate and the GWC settings are read only if the result set
     * has their columns: listings do not select them.
     *
     * @return the entries of the row, one per key, the {@code ENTITY} entry of the object of the row first. Empty if
     *     the row does not hold its object.
     */
    public List<CacheEntry> decompose(ResultSet rs) throws SQLException {
        final String type = rs.getString("@type");
        String[] columns = objectColumns(type);
        Map<String, RowObject> objects = readObjects(rs, columns);
        RowObject rowObject = objects.get(columns[0]);
        if (null == rowObject) {
            return List.of();
        }
        RowEntries entries = new RowEntries(rowsRead(rs).returnedKeys);
        entries.add(objects);
        switch (type) {
            case "LayerInfo" -> {
                entries.addStyles(readStyles(rs));
                addTileLayer(entries, rowObject, rs);
            }
            case "LayerGroupInfo" -> {
                entries.addStyles(readStyles(rs));
                entries.addMembers(readMembers(rs));
                addTileLayer(entries, rowObject, rs);
            }
            default -> {
                // the other types have no styles, members or GWC settings in their rows
            }
        }
        return entries.toList();
    }

    /** The JSON object columns of a row of the type, the column of the object of the row first. */
    private static String[] objectColumns(String type) {
        return switch (type) {
            case "NamespaceInfo" -> new String[] {"namespace"};
            case "WorkspaceInfo" -> new String[] {"workspace"};
            case "DataStoreInfo", "CoverageStoreInfo", "WMSStoreInfo", "WMTSStoreInfo" ->
                new String[] {"store", "workspace"};
            case "FeatureTypeInfo", "CoverageInfo", "WMSLayerInfo", "WMTSLayerInfo" ->
                new String[] {"resource", "store", "workspace", "namespace"};
            case "StyleInfo" -> new String[] {"style", "workspace"};
            case "LayerInfo" -> PUBLISHED_OBJECTS;
            case "LayerGroupInfo" -> new String[] {"publishedinfo", "workspace"};
            default -> throw new IllegalArgumentException("Unexpected value: " + type);
        };
    }

    /**
     * Returns the {@code ENTITY} key of the object of the row at the cursor, read from its own column without splitting
     * the row. Empty if the row does not hold its object.
     */
    Optional<CacheKey> rowKey(ResultSet rs) throws SQLException {
        String type = rs.getString("@type");
        String json = rs.getString(objectColumns(type)[0]);
        if (null == json) {
            return Optional.empty();
        }
        RowObject object = RowObject.parse(json);
        return Optional.of(CacheKey.entity(object.type(), object.id()));
    }

    /** Reads the JSON objects of the row in the given columns, by column name. NULL columns are skipped. */
    private static Map<String, RowObject> readObjects(ResultSet rs, String... columns) throws SQLException {
        Map<String, RowObject> objects = new LinkedHashMap<>();
        for (String column : columns) {
            String encoded = rs.getString(column);
            if (null != encoded) {
                objects.put(column, RowObject.parse(encoded));
            }
        }
        return objects;
    }

    /** Reads the {@code members} aggregate of a group row, as an empty array if it is NULL or not selected. */
    private JsonNode readMembers(ResultSet rs) throws SQLException {
        String encoded = hasColumn(rs, "members") ? rs.getString("members") : null;
        if (null == encoded) {
            return objectMapper.createArrayNode();
        }
        return objectMapper.readTree(encoded);
    }

    /** Adds the GWC settings of the layer or group of the row, if the result set has their column. */
    private void addTileLayer(RowEntries entries, RowObject published, ResultSet rs) throws SQLException {
        if (!hasColumn(rs, TILE_LAYER_LABEL)) {
            return;
        }
        String json = rs.getString(TILE_LAYER_LABEL);
        byte[] utf8 = null == json ? null : json.getBytes(UTF_8);
        entries.addTileLayer(published.id(), utf8);
    }

    /**
     * Whether the result set has a column, read from its metadata once per result set. The check runs for the rows of a
     * listing, and asking the driver for a missing column throws.
     */
    private boolean hasColumn(ResultSet rs, String label) throws SQLException {
        return rowsRead(rs).hasColumn(label);
    }

    /** What the mapper knows of the rows split so far from the result set, starting over with a new result set. */
    private RowsRead rowsRead(ResultSet rs) {
        if (null == rowsRead || rowsRead.resultSet != rs) {
            rowsRead = new RowsRead(rs);
        }
        return rowsRead;
    }

    /** The rows split from one result set by {@link #decompose(ResultSet)}. */
    private static final class RowsRead {

        private final ResultSet resultSet;

        /** The {@code ENTITY} keys of the objects returned for the rows split so far. */
        private final Set<CacheKey> returnedKeys = new HashSet<>();

        /** The column labels of the result set in lower case, null until a row asks for one. */
        private @Nullable Set<String> columnLabels;

        RowsRead(ResultSet resultSet) {
            this.resultSet = resultSet;
        }

        boolean hasColumn(String label) throws SQLException {
            if (null == columnLabels) {
                columnLabels = readLabels(resultSet.getMetaData());
            }
            return columnLabels.contains(label.toLowerCase(Locale.ROOT));
        }

        private static Set<String> readLabels(ResultSetMetaData metadata) throws SQLException {
            Set<String> labels = new HashSet<>();
            for (int column = 1; column <= metadata.getColumnCount(); column++) {
                String label = metadata.getColumnLabel(column);
                labels.add(label.toLowerCase(Locale.ROOT));
            }
            return labels;
        }
    }

    /**
     * The cache entries of a row, one per key, the object of the row first. An embedded object gives its entries once
     * per result set: it is left out if it was returned with an earlier row, or earlier in this row, such as the
     * workspace of the members of a group.
     */
    private static final class RowEntries {

        private final Map<CacheKey, CacheEntry> byKey = new LinkedHashMap<>();

        /** The {@code ENTITY} keys returned for the result set so far, this row included. */
        private final Set<CacheKey> returnedKeys;

        RowEntries(Set<CacheKey> returnedKeys) {
            this.returnedKeys = returnedKeys;
        }

        /** Adds the entries of the styles of a published object. */
        void addStyles(JsonNode styles) {
            for (JsonNode style : styles) {
                add(nestedObjects(style, "style", "workspace"));
            }
        }

        /** Adds the entries of the members of a group, read as published rows. */
        void addMembers(JsonNode members) {
            for (JsonNode member : members) {
                add(nestedObjects(member, PUBLISHED_OBJECTS));
                addStyles(member.path("styles"));
            }
        }

        /** Adds the GWC settings of a layer or group, see {@link CacheEntry#tileLayer(String, byte[])}. */
        void addTileLayer(String publishedId, byte @Nullable [] json) {
            CacheEntry entry = CacheEntry.tileLayer(publishedId, json);
            byKey.put(entry.key(), entry);
        }

        /**
         * Adds an {@code ENTITY} entry and a {@code NAME} entry for the objects. The first object added is the object
         * of the row. An object gets no {@code NAME} entry if its prefix is not among them.
         */
        void add(Map<String, RowObject> objects) {
            for (RowObject object : objects.values()) {
                addObject(object, objects);
            }
        }

        List<CacheEntry> toList() {
            return List.copyOf(byKey.values());
        }

        // Checking the key first builds the JSON of an embedded object only once per result set
        private void addObject(RowObject object, Map<String, RowObject> objects) {
            CacheKey key = CacheKey.entity(object.type(), object.id());
            boolean rowObject = byKey.isEmpty();
            boolean newToResultSet = returnedKeys.add(key);
            if (rowObject || newToResultSet) {
                byKey.put(key, CacheEntry.entity(object.type(), object.id(), object.json()));
                addName(object, objects);
            }
        }

        private void addName(RowObject object, Map<String, RowObject> objects) {
            Optional<String> name = prefixedName(object, objects);
            if (name.isPresent()) {
                CacheEntry entry = CacheEntry.name(object.type(), name.get(), object.id());
                byKey.putIfAbsent(entry.key(), entry);
            }
        }

        /**
         * Reads the JSON objects of an aggregate element in the given fields, by field name. Null fields are skipped.
         */
        private Map<String, RowObject> nestedObjects(JsonNode element, String... fields) {
            Map<String, RowObject> objects = new LinkedHashMap<>();
            for (String field : fields) {
                JsonNode object = element.path(field);
                if (object.isObject()) {
                    objects.put(field, RowObject.nested(object));
                }
            }
            return objects;
        }

        /**
         * The prefixed name of an object, as {@link InfoEvent#prefixedName(Info)} computes it. It is read from the JSON
         * of the object and of the objects referenced by it, and is empty if {@code objects} does not hold them.
         */
        private Optional<String> prefixedName(RowObject object, Map<String, RowObject> objects) {
            return switch (object.type()) {
                case WORKSPACE, NAMESPACE -> Optional.of(object.name());
                case DATASTORE, COVERAGESTORE, WMSSTORE, WMTSSTORE, STYLE, LAYERGROUP ->
                    prefixedBy(object, "workspace", objects);
                case FEATURETYPE, COVERAGE, WMSLAYER, WMTSLAYER -> prefixedBy(object, "namespace", objects);
                case LAYER -> layerName(object, objects);
                default -> throw new IllegalArgumentException("Unexpected type: " + object.type());
            };
        }

        /** A layer takes the prefixed name of its resource. */
        private Optional<String> layerName(RowObject layer, Map<String, RowObject> objects) {
            Optional<RowObject> resource = referenced(layer, "resource", objects);
            return resource.flatMap(found -> prefixedBy(found, "namespace", objects));
        }
        /**
         * The name of an object, prefixed by the name of the workspace or namespace referenced by its {@code property}.
         * The JSON of a namespace holds its prefix as {@code name}. If the reference is null, the name is the one given
         * by {@link CatalogInfoKeys#globalName(String)}: bare, or empty if it holds a colon.
         */
        private Optional<String> prefixedBy(RowObject object, String property, Map<String, RowObject> objects) {
            if (object.reference(property).isEmpty()) {
                String globalName = CatalogInfoKeys.globalName(object.name());
                return Optional.ofNullable(globalName);
            }
            Optional<RowObject> prefix = referenced(object, property, objects);
            return prefix.map(found -> InfoEvent.prefixedName(Optional.of(found.name()), object.name()));
        }

        /** The object referenced by the {@code property} of an object, if {@code objects} holds it under that name. */
        private Optional<RowObject> referenced(RowObject object, String property, Map<String, RowObject> objects) {
            Optional<String> id = object.reference(property);
            RowObject held = objects.get(property);
            if (id.isEmpty() || null == held || !id.get().equals(held.id())) {
                return Optional.empty();
            }
            return Optional.of(held);
        }
    }

    /**
     * A catalog object read from a row: its JSON text if the driver returned one, and its parsed tree, read for its id,
     * type, name and references.
     */
    private record RowObject(@Nullable String text, JsonNode tree) {

        static RowObject parse(String text) {
            return new RowObject(text, objectMapper.readTree(text));
        }

        static RowObject nested(JsonNode tree) {
            return new RowObject(null, tree);
        }

        /** The JSON in UTF-8: the column text, or the tree written back for an object nested in an aggregate. */
        byte[] json() {
            if (null == text) {
                return objectMapper.writeValueAsBytes(tree);
            }
            return text.getBytes(UTF_8);
        }

        String id() {
            return tree.path("id").asString();
        }

        ConfigInfoType type() {
            return typeOf(tree.path("@type").asString());
        }

        String name() {
            return tree.path("name").asString();
        }

        /** The id held by a reference property, empty if the reference is null. */
        Optional<String> reference(String property) {
            return tree.path(property).stringValueOpt();
        }
    }

    private static ConfigInfoType typeOf(String jsonType) {
        ConfigInfoType type = TYPES_BY_JSON_TYPE.get(jsonType);
        if (null == type) {
            throw new IllegalArgumentException("Unexpected @type: " + jsonType);
        }
        return type;
    }

    protected <V> V decode(ResultSet rs, String column, Class<V> valueType) throws SQLException {
        return decode(rs.getString(column), valueType);
    }

    protected <V> V decode(String encoded, Class<V> valueType) {
        if (null == encoded) {
            return null;
        }
        CatalogInfoDecodes.increment();
        return objectMapper.readValue(encoded, valueType);
    }

    /**
     * Decodes a catalog object from its own JSON, as held by the cache. Its references are proxies holding ids, and its
     * attributes point to it, as for a mapped row.
     */
    static <V extends CatalogInfo> V decodeObject(byte[] json, Class<V> type) {
        CatalogInfoDecodes.increment();
        V info = objectMapper.readValue(json, type);
        linkAttributes(info);
        return info;
    }

    public static <I extends CatalogInfo> CatalogInfoRowMapper<I> newInstance() {
        return new CatalogInfoRowMapper<>();
    }
}
