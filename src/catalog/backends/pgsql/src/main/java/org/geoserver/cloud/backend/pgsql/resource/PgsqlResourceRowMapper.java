/*
 * (c) 2023 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.backend.pgsql.resource;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.geoserver.platform.resource.Resource;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @since 1.4
 */
@RequiredArgsConstructor
public class PgsqlResourceRowMapper implements RowMapper<PgsqlResource> {

    private final @NonNull PgsqlResourceStore store;

    /**
     * Expects the following columns:
     *
     * <pre>{@code
     * id         BIGINT
     * parentid   BIGINT
     * "type"     resourcetype
     * path 	  TEXT
     * mtime	  timestamp
     * }</pre>
     */
    @Override
    public PgsqlResource mapRow(ResultSet rs, int rowNum) throws SQLException {
        long id = rs.getLong("id");
        long parentId = rs.getLong("parentid");
        Resource.Type type = Resource.Type.valueOf(rs.getString("type"));
        String path = rs.getString("path");
        long mtime = rs.getTimestamp("mtime").getTime();
        return new PgsqlResource(store, id, parentId, type, path, mtime);
    }

    public PgsqlResource undefined(String path) {
        return new PgsqlResource(store, path);
    }
}
