/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.gwc.backend.pgconfig;

import static org.geoserver.cloud.gwc.backend.pgconfig.PgconfigTileLayerInfoRowMapper.MAPPED_COLUMNS;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.cloud.backend.pgconfig.catalog.repository.LoggingTemplate;
import org.geoserver.gwc.layer.CatalogConfiguration;
import org.geoserver.gwc.layer.GeoServerTileLayerInfo;
import org.geoserver.platform.resource.ResourceStore;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.Nullable;

/**
 * Implementation of {@link TileLayerInfoRepository} for {@link CatalogConfiguration} to manage
 * {@link GeoServerTileLayerInfo}s directly from the database instead of going through {@link
 * ResourceStore}.
 *
 * @since 1.7
 */
@Slf4j
public class PgconfigTileLayerInfoRepository implements TileLayerInfoRepository {

    private final LoggingTemplate template;

    private final String tileLayersQueryTable;

    public PgconfigTileLayerInfoRepository(@NonNull JdbcTemplate template) {
        this.template = new LoggingTemplate(template);
        this.tileLayersQueryTable = "tilelayers_mat";
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
        Objects.requireNonNull(published, "TileLayerInfo.published is null");
        Objects.requireNonNull(published.getId(), "TileLayerInfo.published.id is null");

        final String encoded = PgconfigTileLayerInfoRowMapper.encode(tli);
        log.debug("Saving TileLayer {}: {}", published.prefixedName(), encoded);
        int updated = template.update(
                """
                    UPDATE publishedinfo SET tilelayer = to_json(?::json) WHERE id = ?
                    """,
                encoded,
                published.getId());
        return updated == 1;
    }

    @Override
    public boolean delete(@Nullable String workspaceName, @NonNull String localName) throws DataAccessException {
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
        String query = """
                        SELECT %s FROM "%s"
                        """
                .formatted(MAPPED_COLUMNS, tileLayersQueryTable);
        return template.queryForStream(query, mapper());
    }

    @Override
    public Optional<TileLayerInfo> find(String workspaceName, @NonNull String localName) throws DataAccessException {
        String sql;
        Object[] args;
        if (workspaceName == null) {
            sql =
                    """
                    SELECT %s FROM "%s" WHERE "workspace.name" IS NULL AND "published.name" = ?
                    """
                            .formatted(MAPPED_COLUMNS, tileLayersQueryTable);
            args = new Object[] {localName};
        } else {
            sql =
                    """
                    SELECT %s FROM "%s" WHERE "workspace.name" = ? AND "published.name" = ?
                    """
                            .formatted(MAPPED_COLUMNS, tileLayersQueryTable);
            args = new Object[] {workspaceName, localName};
        }
        try {
            TileLayerInfo info = template.queryForObject(sql, mapper(), args);
            return Optional.of(info);
        } catch (EmptyResultDataAccessException empty) {
            log.trace("returning empty for {}", sql);
            return Optional.empty();
        }
    }

    @Override
    public int count() throws DataAccessException {
        String query = "SELECT count(*) FROM \"%s\"".formatted(tileLayersQueryTable);
        Integer count = template.queryForObject(query, Integer.class);
        return count == null ? 0 : count;
    }

    @Override
    public Set<String> findAllNames() throws DataAccessException {
        String query = "SELECT name FROM \"%s\"".formatted(tileLayersQueryTable);
        try (var stream = template.queryForStream(query, (rs, _) -> rs.getString(1))) {
            return stream.collect(Collectors.toCollection(TreeSet::new));
        }
    }

    @Override
    public boolean exists(String workspaceName, @NonNull String localName) throws DataAccessException {
        String query;
        Object[] args;
        if (null == workspaceName) {
            query =
                    """
                    SELECT exists(SELECT 1 FROM "%s" WHERE "workspace.name" IS NULL AND "published.name" = ?)
                    """
                            .formatted(tileLayersQueryTable);
            args = new Object[] {localName};
        } else {
            query =
                    """
                    SELECT exists(SELECT 1 FROM "%s" WHERE "workspace.name" = ? AND "published.name" = ?)
                    """
                            .formatted(tileLayersQueryTable);
            args = new Object[] {workspaceName, localName};
        }
        Boolean exists = template.queryForObject(query, Boolean.class, args);
        return exists != null && exists.booleanValue();
    }

    private PgconfigTileLayerInfoRowMapper mapper() {
        return PgconfigTileLayerInfoRowMapper.newInstance(template.getJdbcTemplate());
    }
}
