/*
 * (c) 2023 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.backend.pgsql.gwc;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.gwc.layer.GeoServerTileLayerInfo;
import org.geoserver.gwc.layer.TileLayerCatalog;
import org.geoserver.gwc.layer.TileLayerCatalogListener;
import org.geoserver.ows.LocalWorkspace;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * @since 1.4
 */
@RequiredArgsConstructor
public class PgsqlTileLayerCatalog implements TileLayerCatalog {

    private final @NonNull JdbcTemplate template;

    private GeoServerTileLayerInfoRowMapper rowMapper;

    @Override
    public void addListener(TileLayerCatalogListener listener) {
        throw new UnsupportedOperationException("implement");
    }

    @Override
    public boolean exists(String layerId) {
        return template.queryForObject(
                """
                SELECT exists(id) FROM tile_layer WHERE id = ?
                """,
                (rs, rn) -> rs.getBoolean(1),
                layerId);
    }

    @Override
    public Set<String> getLayerIds() {
        return template.queryForStream(
                        """
                SELECT id FROM tile_layers
                """,
                        (rs, rn) -> rs.getString(1))
                .collect(Collectors.toSet());
    }

    @Override
    public Set<String> getLayerNames() {
        return template.queryForStream(
                        """
                SELECT name FROM tile_layers ORDER BY name
                """,
                        (rs, rn) -> rs.getString(1))
                .collect(Collectors.toSet());
    }

    @Override
    public String getLayerId(String layerName) {
        final WorkspaceInfo ws = LocalWorkspace.get();
        final String workspace = workspace(layerName);
        final String name = name(layerName);
        if (ws != null && !layerName.startsWith(ws.getName() + ":")) {
            throw new IllegalArgumentException(
                    "Local workspace is %s, but requested layer %s"
                            .formatted(ws.getName(), layerName));
        }

        if (null == workspace) {
            return template.queryForObject(
                    """
                    SELECT id FROM tile_layers WHERE workspace IS NULL AND name = ?
                    """,
                    (rs, rn) -> rs.getString(1),
                    name);
        }
        return template.queryForObject(
                """
                SELECT id FROM tile_layers WHERE workspace = ? AND name = ?
                """,
                (rs, rn) -> rs.getString(1),
                workspace,
                name);
    }

    private String name(String layerName) {
        return layerName.indexOf(':') == -1
                ? layerName
                : layerName.substring(1 + layerName.indexOf(':'));
    }

    private String workspace(String layerName) {
        return layerName.indexOf(':') == -1 ? null : layerName.substring(0, layerName.indexOf(':'));
    }

    @Override
    public String getLayerName(String layerId) {
        return template.queryForObject(
                """
                SELECT workspace, name FROM tile_layers WHERE id = ?
                """,
                (rs, rn) -> {
                    String ws = rs.getString(1);
                    String name = rs.getString(2);
                    return ws == null ? name : (ws + ":" + name);
                },
                layerId);
    }

    @Override
    public PgsqlTileLayerInfo getLayerById(String id) {
        return template.queryForObject(
                """
                SELECT * FROM tile_layers WHERE id = ?
                """,
                rowMapper,
                id);
    }

    @Override
    public PgsqlTileLayerInfo getLayerByName(String layerName) {
        final WorkspaceInfo ws = LocalWorkspace.get();
        final String workspace = workspace(layerName);
        final String name = name(layerName);
        if (ws != null && !layerName.startsWith(ws.getName() + ":")) {
            throw new IllegalArgumentException(
                    "Local workspace is %s, but requested layer %s"
                            .formatted(ws.getName(), layerName));
        }
        if (null == workspace) {
            return template.queryForObject(
                    """
                    SELECT * FROM tile_layers WHERE workspace IS NULL AND name = ?
                    """,
                    rowMapper,
                    workspace,
                    name);
        }
        return template.queryForObject(
                """
                SELECT * FROM tile_layers WHERE workspace = ? AND name = ?
                """,
                rowMapper,
                workspace,
                name);
    }

    @Override
    public PgsqlTileLayerInfo delete(String tileLayerId) {
        PgsqlTileLayerInfo currValue = getLayerById(tileLayerId);
        if (null != currValue) {
            int updated =
                    template.update(
                            """
                    DELETE FROM tile_layer WHERE id = ?
                    """,
                            tileLayerId);
            if (0 == updated) currValue = null;
        }
        return currValue;
    }

    @Override
    public PgsqlTileLayerInfo save(GeoServerTileLayerInfo newValue) {
        // TODO: make sure name clashes throw a sql exception
        // if (oldValue == null) {
        // final String duplicateNameId = layersByName.get(newValue.getName());
        // if (null != duplicateNameId) {
        // throw new IllegalArgumentException(
        // "TileLayer with same name already exists: "
        // + newValue.getName()
        // + ": <"
        // + duplicateNameId
        // + ">");
        // }
        // }
        final String encoded = encode(newValue);
        final String tileLayerId = newValue.getId();

        int updated =
                template.update(
                        """
                UPDATE tile_layer SET info = to_json(?::json) WHERE id = ?
                """,
                        tileLayerId,
                        encoded);
        if (1 == updated) return getLayerById(tileLayerId);

        throw new IllegalArgumentException("TileLayer %s does not exist".formatted(tileLayerId));
    }

    /**
     * @param newValue
     * @return
     */
    private String encode(GeoServerTileLayerInfo newValue) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void initialize() {
        throw new UnsupportedOperationException("implement");
    }

    @Override
    public void reset() {
        throw new UnsupportedOperationException("implement");
    }

    @Override
    public String getPersistenceLocation() {
        throw new UnsupportedOperationException("implement");
    }
}
