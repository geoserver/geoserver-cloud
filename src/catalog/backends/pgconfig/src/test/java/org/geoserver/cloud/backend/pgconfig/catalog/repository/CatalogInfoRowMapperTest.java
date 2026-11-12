/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.backend.pgconfig.catalog.repository;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.geoserver.cloud.event.info.ConfigInfoType.DATASTORE;
import static org.geoserver.cloud.event.info.ConfigInfoType.FEATURETYPE;
import static org.geoserver.cloud.event.info.ConfigInfoType.LAYER;
import static org.geoserver.cloud.event.info.ConfigInfoType.LAYERGROUP;
import static org.geoserver.cloud.event.info.ConfigInfoType.NAMESPACE;
import static org.geoserver.cloud.event.info.ConfigInfoType.STYLE;
import static org.geoserver.cloud.event.info.ConfigInfoType.WORKSPACE;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.cloud.backend.pgconfig.cache.CacheEntry;
import org.geoserver.cloud.backend.pgconfig.cache.CacheKey;
import org.geoserver.cloud.backend.pgconfig.cache.CacheKey.Kind;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class CatalogInfoRowMapperTest {

    private static final ObjectMapper JSON = PgconfigObjectMapper.newObjectMapper();

    private static final String W06_ID = "WorkspaceInfo-w06";
    private static final String W07_ID = "WorkspaceInfo-w07";
    private static final String N06_ID = "NamespaceInfo-n06";
    private static final String DS06_ID = "DataStoreInfo-ds06";
    private static final String ROADS_ID = "FeatureTypeInfo-01M6MHWGZD1VRRKAPGC6NXCMWD";
    private static final String LAYER_ID = "LayerInfo-01M6MHWGZJH051H0ZNCN68FDMN";
    private static final String ROADS_STYLE_ID = "StyleInfo-01M6MHVZNXSV6SQ3QEBC2TNMEC";
    private static final String LINE_ID = "StyleInfo-01M6MHVZWHWE03NDVP9V8Y8GHN";
    private static final String DASHED_ID = "StyleInfo-01M6MHW002F2TVP9VH636RFR8T";
    private static final String RIVERS_ID = "LayerInfo-rivers";
    private static final String RIVERS_RESOURCE_ID = "FeatureTypeInfo-rivers";
    private static final String PARENT_ID = "LayerGroupInfo-parent";
    private static final String NESTED_ID = "LayerGroupInfo-nested";
    private static final String BASEMAP_ID = "LayerGroupInfo-basemap";

    private static final String W06_JSON =
            """
            {"id": "WorkspaceInfo-w06", "name": "w06", "@type": "WorkspaceInfo", "isolated": false}""";

    private static final String W07_JSON =
            """
            {"id": "WorkspaceInfo-w07", "name": "w07", "@type": "WorkspaceInfo", "isolated": false}""";

    // The prefix differs from the name of the workspace, to show which of the two a prefixed name uses
    private static final String N06_JSON =
            """
            {"id": "NamespaceInfo-n06", "uri": "http://n06.example.com", "name": "n06", "@type": "NamespaceInfo", \
            "isolated": false}""";

    private static final String DS06_JSON =
            """
            {"id": "DataStoreInfo-ds06", "name": "ds06", "type": "PostGIS", "@type": "DataStoreInfo", "enabled": true, \
            "workspace": "WorkspaceInfo-w06"}""";

    private static final String ROADS_JSON =
            """
            {"id": "FeatureTypeInfo-01M6MHWGZD1VRRKAPGC6NXCMWD", "name": "roads", "@type": "FeatureTypeInfo", \
            "store": "DataStoreInfo-ds06", "enabled": true, "namespace": "NamespaceInfo-n06", "nativeName": "roads"}""";

    private static final String ROADS_STYLE_JSON =
            """
            {"id": "StyleInfo-01M6MHVZNXSV6SQ3QEBC2TNMEC", "name": "roads_style", "@type": "StyleInfo", \
            "format": "sld", "filename": "roads_style.sld", "workspace": "WorkspaceInfo-w06"}""";

    private static final String ROADS_STYLE_W07_JSON =
            """
            {"id": "StyleInfo-01M6MHVZNXSV6SQ3QEBC2TNMEC", "name": "roads_style", "@type": "StyleInfo", \
            "format": "sld", "filename": "roads_style.sld", "workspace": "WorkspaceInfo-w07"}""";

    private static final String LINE_JSON =
            """
            {"id": "StyleInfo-01M6MHVZWHWE03NDVP9V8Y8GHN", "name": "line", "@type": "StyleInfo", "format": "sld", \
            "filename": "line.sld", "workspace": null}""";

    private static final String DASHED_JSON =
            """
            {"id": "StyleInfo-01M6MHW002F2TVP9VH636RFR8T", "name": "dashed", "@type": "StyleInfo", "format": "sld", \
            "filename": "dashed.sld", "workspace": "WorkspaceInfo-w07"}""";

    private static final String ALTERNATE_STYLES_JSON =
            """
            [{"style": %s, "workspace": null}, {"style": %s, "workspace": %s}]"""
                    .formatted(LINE_JSON, DASHED_JSON, W07_JSON);

    private static final String LAYER_JSON = readLayerSample();

    private static final String RIVERS_LAYER_JSON =
            """
            {"id": "LayerInfo-rivers", "name": "rivers", "type": "VECTOR", "@type": "LayerInfo", "styles": [], \
            "resource": "FeatureTypeInfo-rivers", "defaultStyle": "StyleInfo-01M6MHVZWHWE03NDVP9V8Y8GHN"}""";

    private static final String RIVERS_RESOURCE_JSON =
            """
            {"id": "FeatureTypeInfo-rivers", "name": "rivers", "@type": "FeatureTypeInfo", \
            "store": "DataStoreInfo-ds06", "enabled": true, "namespace": "NamespaceInfo-n06", "nativeName": "rivers"}""";

    private static final String PARENT_JSON =
            """
            {"id": "LayerGroupInfo-parent", "mode": "SINGLE", "name": "parent", "@type": "LayerGroupInfo", \
            "layers": ["LayerInfo-01M6MHWGZJH051H0ZNCN68FDMN", "LayerInfo-rivers", "LayerGroupInfo-nested"], \
            "styles": [null, null, null], "workspace": "WorkspaceInfo-w06"}""";

    private static final String NESTED_JSON =
            """
            {"id": "LayerGroupInfo-nested", "mode": "SINGLE", "name": "nested", "@type": "LayerGroupInfo", \
            "layers": ["LayerInfo-hidden"], "styles": [null], "workspace": "WorkspaceInfo-w06"}""";

    private static final String BASEMAP_JSON =
            """
            {"id": "LayerGroupInfo-basemap", "mode": "SINGLE", "name": "basemap", "@type": "LayerGroupInfo", \
            "layers": ["LayerInfo-01M6MHWGZJH051H0ZNCN68FDMN"], "styles": [null], "workspace": null}""";

    private static final String TILE_LAYER_JSON = "{\"enabled\":true}";

    private static final String MEMBER_TEMPLATE =
            """
            {"@type": "%s", "publishedinfo": %s, "workspace": %s, "namespace": %s, "store": %s, "resource": %s, \
            "defaultStyle": %s, "styles": %s}""";

    // The aggregate is one level deep: the member of the nested group, LayerInfo-hidden, is not in it
    private static final String MEMBERS_JSON = "[%s, %s, %s]"
            .formatted(
                    layerMember(LAYER_JSON, ROADS_JSON, ROADS_STYLE_JSON, ALTERNATE_STYLES_JSON),
                    layerMember(RIVERS_LAYER_JSON, RIVERS_RESOURCE_JSON, LINE_JSON, "null"),
                    groupMember(NESTED_JSON));

    private final CatalogInfoRowMapper<CatalogInfo> mapper = CatalogInfoRowMapper.newInstance();

    @Test
    void decomposesLayerRowIntoEntities() throws SQLException {
        List<CacheEntry> entries = mapper.decompose(resultSet(layerRow()));

        assertThat(entityJson(entries))
                .containsExactlyInAnyOrderEntriesOf(Map.of(
                        CacheKey.entity(LAYER, LAYER_ID), LAYER_JSON,
                        CacheKey.entity(FEATURETYPE, ROADS_ID), ROADS_JSON,
                        CacheKey.entity(DATASTORE, DS06_ID), DS06_JSON,
                        CacheKey.entity(WORKSPACE, W06_ID), W06_JSON,
                        CacheKey.entity(NAMESPACE, N06_ID), N06_JSON,
                        CacheKey.entity(STYLE, ROADS_STYLE_ID), ROADS_STYLE_JSON));
    }

    // the repositories decode the object of the row from the first entry
    @Test
    void rowObjectComesFirst() throws SQLException {
        List<CacheEntry> entries = mapper.decompose(resultSet(layerRow()));

        assertThat(entries.getFirst().key()).isEqualTo(CacheKey.entity(LAYER, LAYER_ID));
    }

    @Test
    void rowWithoutObjectIsEmpty() throws SQLException {
        Map<String, String> row = layerRow();
        row.put("publishedinfo", null);

        assertThat(mapper.decompose(resultSet(row))).isEmpty();
    }

    // the rows of a listing share their workspace, namespace, store and style
    @Test
    void sharedObjectsComeOnce() throws SQLException {
        Map<String, String> row = layerRow();
        ResultSet rs = resultSet(row);
        mapper.decompose(rs);
        row.put("publishedinfo", RIVERS_LAYER_JSON);
        row.put("resource", RIVERS_RESOURCE_JSON);

        List<CacheEntry> second = mapper.decompose(rs);

        assertThat(entityKeys(second))
                .containsExactlyInAnyOrder(
                        CacheKey.entity(LAYER, RIVERS_ID), CacheKey.entity(FEATURETYPE, RIVERS_RESOURCE_ID));
        assertThat(nameIds(second)).containsEntry(CacheKey.name(LAYER, "n06:rivers"), RIVERS_ID);
    }

    @Test
    void decomposesAlternateStyles() throws SQLException {
        Map<String, String> row = layerRow();
        row.put("styles", ALTERNATE_STYLES_JSON);

        List<CacheEntry> entries = mapper.decompose(resultSet(row));

        assertThat(entityKeys(entries))
                .contains(
                        CacheKey.entity(STYLE, LINE_ID),
                        CacheKey.entity(STYLE, DASHED_ID),
                        CacheKey.entity(WORKSPACE, W07_ID));
        assertThat(nameIds(entries))
                .containsEntry(CacheKey.name(STYLE, "line"), LINE_ID)
                .containsEntry(CacheKey.name(STYLE, "w07:dashed"), DASHED_ID)
                .containsEntry(CacheKey.name(WORKSPACE, "w07"), W07_ID);
        assertThat(jsonTree(entries, CacheKey.entity(STYLE, DASHED_ID))).isEqualTo(JSON.readTree(DASHED_JSON));
    }

    // the layer takes the name of its resource, not the l010_w06 written in its own JSON
    @Test
    void nameEntriesUsePrefixedNames() throws SQLException {
        List<CacheEntry> entries = mapper.decompose(resultSet(layerRow()));

        assertThat(nameIds(entries))
                .containsExactlyInAnyOrderEntriesOf(Map.of(
                        CacheKey.name(LAYER, "n06:roads"), LAYER_ID,
                        CacheKey.name(FEATURETYPE, "n06:roads"), ROADS_ID,
                        CacheKey.name(DATASTORE, "w06:ds06"), DS06_ID,
                        CacheKey.name(WORKSPACE, "w06"), W06_ID,
                        CacheKey.name(NAMESPACE, "n06"), N06_ID,
                        CacheKey.name(STYLE, "w06:roads_style"), ROADS_STYLE_ID));
    }

    @Test
    void nullColumnsYieldNoEntry() throws SQLException {
        Map<String, String> row = groupRow(BASEMAP_JSON, null);
        row.put("members", null);

        List<CacheEntry> entries = mapper.decompose(resultSet(row));

        assertThat(entries)
                .containsExactlyInAnyOrder(
                        CacheEntry.entity(LAYERGROUP, BASEMAP_ID, BASEMAP_JSON.getBytes(UTF_8)),
                        CacheEntry.name(LAYERGROUP, "basemap", BASEMAP_ID));
    }

    @Test
    void decomposesGroupMembers() throws SQLException {
        Map<String, String> row = groupRow(PARENT_JSON, W06_JSON);
        row.put("members", MEMBERS_JSON);

        List<CacheEntry> entries = mapper.decompose(resultSet(row));

        assertThat(entityKeys(entries))
                .containsExactlyInAnyOrder(
                        CacheKey.entity(LAYERGROUP, PARENT_ID),
                        CacheKey.entity(WORKSPACE, W06_ID),
                        CacheKey.entity(LAYER, LAYER_ID),
                        CacheKey.entity(FEATURETYPE, ROADS_ID),
                        CacheKey.entity(DATASTORE, DS06_ID),
                        CacheKey.entity(NAMESPACE, N06_ID),
                        CacheKey.entity(STYLE, ROADS_STYLE_ID),
                        CacheKey.entity(STYLE, LINE_ID),
                        CacheKey.entity(STYLE, DASHED_ID),
                        CacheKey.entity(WORKSPACE, W07_ID),
                        CacheKey.entity(LAYER, RIVERS_ID),
                        CacheKey.entity(FEATURETYPE, RIVERS_RESOURCE_ID),
                        CacheKey.entity(LAYERGROUP, NESTED_ID));
        assertThat(nameIds(entries))
                .containsEntry(CacheKey.name(LAYER, "n06:rivers"), RIVERS_ID)
                .containsEntry(CacheKey.name(LAYERGROUP, "w06:nested"), NESTED_ID);
        assertThat(jsonTree(entries, CacheKey.entity(LAYER, LAYER_ID))).isEqualTo(JSON.readTree(LAYER_JSON));
    }

    // listings do not select the members column, and the driver throws when asked for a missing column
    @Test
    void missingMembersColumnIsNoMembers() throws SQLException {
        List<CacheEntry> entries = mapper.decompose(resultSet(groupRow(PARENT_JSON, W06_JSON)));

        assertThat(entityKeys(entries))
                .containsExactlyInAnyOrder(CacheKey.entity(LAYERGROUP, PARENT_ID), CacheKey.entity(WORKSPACE, W06_ID));
    }

    @Test
    void columnsCheckedOncePerResultSet() throws SQLException {
        ResultSet rs = resultSet(groupRow(PARENT_JSON, W06_JSON));

        mapper.decompose(rs);
        mapper.decompose(rs);

        verify(rs, times(1)).getMetaData();
    }

    // NULL is a layer without tile layer, and a listing does not select the column
    @Test
    void decomposesTileLayerColumn() throws SQLException {
        Map<String, String> withTileLayer = layerRow();
        withTileLayer.put("tilelayer", TILE_LAYER_JSON);
        Map<String, String> withoutTileLayer = layerRow();
        withoutTileLayer.put("tilelayer", null);

        List<CacheEntry> json = tileLayerEntries(mapper.decompose(resultSet(withTileLayer)));
        List<CacheEntry> empty = tileLayerEntries(mapper.decompose(resultSet(withoutTileLayer)));
        List<CacheEntry> notSelected = tileLayerEntries(mapper.decompose(resultSet(layerRow())));

        assertThat(json).containsExactly(CacheEntry.tileLayer(LAYER_ID, TILE_LAYER_JSON.getBytes(UTF_8)));
        assertThat(empty).containsExactly(CacheEntry.tileLayer(LAYER_ID, new byte[0]));
        assertThat(notSelected).isEmpty();
    }

    // the row holds the workspace of the layer, not the one of its default style
    @Test
    void styleOfOtherWorkspaceIsUnnamed() throws SQLException {
        Map<String, String> row = layerRow();
        row.put("defaultStyle", ROADS_STYLE_W07_JSON);

        List<CacheEntry> entries = mapper.decompose(resultSet(row));

        assertThat(entityKeys(entries)).contains(CacheKey.entity(STYLE, ROADS_STYLE_ID));
        assertThat(nameKeys(entries)).noneMatch(key -> key.type() == STYLE);
    }

    // a:b is also the prefixed name of the group b of the workspace a
    @Test
    void globalGroupWithColonUnnamed() throws SQLException {
        String groupNamedAB = BASEMAP_JSON.replace("\"basemap\"", "\"a:b\"");
        Map<String, String> row = groupRow(groupNamedAB, null);
        row.put("members", null);

        List<CacheEntry> entries = mapper.decompose(resultSet(row));

        assertThat(entityKeys(entries)).containsExactly(CacheKey.entity(LAYERGROUP, BASEMAP_ID));
        assertThat(nameKeys(entries)).isEmpty();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("rowsOfOtherTypes")
    void decomposesRowsOfOtherTypes(String type, Map<String, String> columns, List<CacheKey> expected)
            throws SQLException {
        Map<String, String> row = new HashMap<>(columns);
        row.put("@type", type);

        List<CacheEntry> entries = mapper.decompose(resultSet(row));

        assertThat(entityKeys(entries)).containsExactlyInAnyOrderElementsOf(expected);
    }

    static Stream<Arguments> rowsOfOtherTypes() {
        return Stream.of(
                arguments("WorkspaceInfo", Map.of("workspace", W06_JSON), List.of(CacheKey.entity(WORKSPACE, W06_ID))),
                arguments("NamespaceInfo", Map.of("namespace", N06_JSON), List.of(CacheKey.entity(NAMESPACE, N06_ID))),
                arguments(
                        "DataStoreInfo",
                        Map.of("store", DS06_JSON, "workspace", W06_JSON),
                        List.of(CacheKey.entity(DATASTORE, DS06_ID), CacheKey.entity(WORKSPACE, W06_ID))),
                arguments(
                        "FeatureTypeInfo",
                        Map.of(
                                "resource",
                                ROADS_JSON,
                                "store",
                                DS06_JSON,
                                "workspace",
                                W06_JSON,
                                "namespace",
                                N06_JSON),
                        List.of(
                                CacheKey.entity(FEATURETYPE, ROADS_ID),
                                CacheKey.entity(DATASTORE, DS06_ID),
                                CacheKey.entity(WORKSPACE, W06_ID),
                                CacheKey.entity(NAMESPACE, N06_ID))),
                arguments(
                        "StyleInfo",
                        Map.of("style", ROADS_STYLE_JSON, "workspace", W06_JSON),
                        List.of(CacheKey.entity(STYLE, ROADS_STYLE_ID), CacheKey.entity(WORKSPACE, W06_ID))));
    }

    private static Map<String, String> layerRow() {
        Map<String, String> row = new LinkedHashMap<>();
        row.put("@type", "LayerInfo");
        row.put("publishedinfo", LAYER_JSON);
        row.put("resource", ROADS_JSON);
        row.put("store", DS06_JSON);
        row.put("workspace", W06_JSON);
        row.put("namespace", N06_JSON);
        row.put("defaultStyle", ROADS_STYLE_JSON);
        row.put("styles", null);
        return row;
    }

    private static Map<String, String> groupRow(String group, String workspace) {
        Map<String, String> row = new LinkedHashMap<>();
        row.put("@type", "LayerGroupInfo");
        row.put("publishedinfo", group);
        row.put("workspace", workspace);
        row.put("styles", null);
        return row;
    }

    /** A layer of the workspace w06 in a {@code members} aggregate. */
    private static String layerMember(String layer, String resource, String defaultStyle, String styles) {
        return MEMBER_TEMPLATE.formatted(
                "LayerInfo", layer, W06_JSON, N06_JSON, DS06_JSON, resource, defaultStyle, styles);
    }

    /** A group of the workspace w06 in a {@code members} aggregate. */
    private static String groupMember(String group) {
        return MEMBER_TEMPLATE.formatted("LayerGroupInfo", group, W06_JSON, "null", "null", "null", "null", "null");
    }

    /** A result set on one row. Like the driver, it throws when asked for a column missing from the row. */
    private static ResultSet resultSet(Map<String, String> row) throws SQLException {
        List<String> labels = List.copyOf(row.keySet());
        ResultSetMetaData metadata = mock(ResultSetMetaData.class);
        when(metadata.getColumnCount()).thenReturn(labels.size());
        when(metadata.getColumnLabel(anyInt())).thenAnswer(call -> labels.get(call.<Integer>getArgument(0) - 1));

        ResultSet rs = mock(ResultSet.class);
        when(rs.getMetaData()).thenReturn(metadata);
        when(rs.getString(anyString())).thenAnswer(call -> columnValue(row, call.getArgument(0)));
        return rs;
    }

    private static String columnValue(Map<String, String> row, String label) throws SQLException {
        if (!row.containsKey(label)) {
            throw new SQLException("The column name " + label + " was not found in this ResultSet.");
        }
        return row.get(label);
    }

    private static List<CacheKey> entityKeys(List<CacheEntry> entries) {
        return keysOfKind(entries, Kind.ENTITY);
    }

    private static List<CacheKey> nameKeys(List<CacheEntry> entries) {
        return keysOfKind(entries, Kind.NAME);
    }

    private static List<CacheKey> keysOfKind(List<CacheEntry> entries, Kind kind) {
        return entries.stream()
                .map(CacheEntry::key)
                .filter(key -> key.kind() == kind)
                .toList();
    }

    private static List<CacheEntry> tileLayerEntries(List<CacheEntry> entries) {
        return entries.stream()
                .filter(entry -> entry.key().kind() == Kind.TILE_LAYER)
                .toList();
    }

    /** The JSON of the ENTITY entries as text, by key. */
    private static Map<CacheKey, String> entityJson(List<CacheEntry> entries) {
        return entries.stream()
                .filter(entry -> entry.key().kind() == Kind.ENTITY)
                .collect(Collectors.toMap(CacheEntry::key, entry -> new String(entry.json(), UTF_8)));
    }

    /** The ids held by the NAME entries, by key. */
    private static Map<CacheKey, String> nameIds(List<CacheEntry> entries) {
        return entries.stream()
                .filter(entry -> entry.key().kind() == Kind.NAME)
                .collect(Collectors.toMap(CacheEntry::key, CacheEntry::id));
    }

    private static JsonNode jsonTree(List<CacheEntry> entries, CacheKey key) {
        CacheEntry found = entries.stream()
                .filter(entry -> entry.key().equals(key))
                .findFirst()
                .orElseThrow();
        return JSON.readTree(found.json());
    }

    /** The layer sample as the driver returns it, without the final newline of the file. */
    private static String readLayerSample() {
        String path = "/org/geoserver/cloud/backend/pgconfig/cache/layer.json";
        try (InputStream in = CatalogInfoRowMapperTest.class.getResourceAsStream(path)) {
            return new String(in.readAllBytes(), UTF_8).strip();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
