/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.gwc.backend.pgconfig;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import org.geoserver.catalog.PublishedInfo;
import org.geoserver.cloud.backend.pgconfig.catalog.repository.LoggingTemplate;
import org.geoserver.gwc.layer.CatalogConfiguration;
import org.geoserver.gwc.layer.GeoServerTileLayerInfo;
import org.geoserver.gwc.layer.TileLayerCatalog;
import org.geoserver.platform.resource.ResourceStore;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implementation of {@link TileLayerCatalog} for {@link CatalogConfiguration} to manage {@link
 * GeoServerTileLayerInfo}s directly from the database instead of going through {@link
 * ResourceStore}.
 *
 * @since 1.7
 */
@Slf4j
public class PgsqlTileLayerInfoRepository implements TileLayerInfoRepository {

    private final LoggingTemplate template;

    public PgsqlTileLayerInfoRepository(@NonNull JdbcTemplate template) {
        this.template = new LoggingTemplate(template);
    }

    @Override
    public void add(TileLayerInfo tli) throws DataAccessException {
        save(tli);
    }

    @Override
    public boolean save(@NonNull TileLayerInfo tli) throws DataAccessException {
        final PublishedInfo published = tli.getPublished();
        return save(tli, published);
    }

    private boolean save(TileLayerInfo tli, final PublishedInfo published) {
        Objects.requireNonNull(published, "PgsqlTileLayerInfo.published is null");
        Objects.requireNonNull(published.getId(), "PgsqlTileLayerInfo.published.id is null");

        final String encoded = PgsqlTileLayerInfoRowMapper.encode(tli);
        log.debug("Saving TileLayer {}: {}", published.prefixedName(), encoded);
        int updated =
                template.update(
                        """
                    UPDATE publishedinfo SET tilelayer = to_json(?::json) WHERE id = ?
                    """,
                        encoded,
                        published.getId());
        return updated == 1;
    }

    @Override
    public boolean delete(@Nullable String workspaceName, @NonNull String localName)
            throws DataAccessException {
        int updateCount;
        if (null == workspaceName) {
            String query =
                    """
                    UPDATE publishedinfo SET tilelayer = NULL \
                    WHERE id = (SELECT id FROM tilelayers WHERE "workspace.name" IS NULL AND "published.name" = ?)
                    """;
            updateCount = template.update(query, localName);
        } else {
            String query =
                    """
                    UPDATE publishedinfo SET tilelayer = NULL \
                    WHERE id = (SELECT id FROM tilelayers WHERE "workspace.name" = ? AND "published.name" = ?)
                    """;
            updateCount = template.update(query, workspaceName, localName);
        }
        return updateCount == 1;
    }

    @Override
    public Stream<TileLayerInfo> findAll() throws DataAccessException {
        return template.queryForStream("SELECT * FROM tilelayers", mapper());
    }

    @Override
    public Optional<TileLayerInfo> find(String workspaceName, @NonNull String localName)
            throws DataAccessException {
        String sql;
        Object[] args;
        if (workspaceName == null) {
            sql =
                    """
                    SELECT * FROM tilelayers WHERE "workspace.name" IS NULL AND "published.name" = ?
                    """;
            args = new Object[] {localName};
        } else {
            sql =
                    """
                    SELECT * FROM tilelayers WHERE "workspace.name" = ? AND "published.name" = ?
                    """;
            args = new Object[] {workspaceName, localName};
        }
        try {
            TileLayerInfo info = template.queryForObject(sql, mapper(), args);
            return Optional.of(info);
        } catch (EmptyResultDataAccessException empty) {
            return Optional.empty();
        }
    }

    @Override
    public int count() throws DataAccessException {
        Integer count = template.queryForObject("SELECT count(*) FROM tilelayers", Integer.class);
        return count == null ? 0 : count;
    }

    @Override
    public Set<String> findAllNames() throws DataAccessException {
        try (var stream =
                template.queryForStream(
                        "SELECT name FROM tilelayers", (rs, rn) -> rs.getString(1))) {
            return stream.collect(Collectors.toCollection(TreeSet::new));
        }
    }

    @Override
    public boolean exists(String workspaceName, @NonNull String localName)
            throws DataAccessException {
        String query;
        Object[] args;
        if (null == workspaceName) {
            query =
                    """
                    SELECT exists(SELECT 1 FROM tilelayers WHERE "workspace.name" IS NULL AND "published.name" = ?)
                    """;
            args = new Object[] {localName};
        } else {
            query =
                    """
                    SELECT exists(SELECT 1 FROM tilelayers WHERE "workspace.name" = ? AND "published.name" = ?)
                    """;
            args = new Object[] {workspaceName, localName};
        }
        Boolean exists = template.queryForObject(query, Boolean.class, args);
        return exists != null && exists.booleanValue();
    }

    private PgsqlTileLayerInfoRowMapper mapper() {
        return PgsqlTileLayerInfoRowMapper.newInstance(template.getJdbcTemplate());
    }
}
