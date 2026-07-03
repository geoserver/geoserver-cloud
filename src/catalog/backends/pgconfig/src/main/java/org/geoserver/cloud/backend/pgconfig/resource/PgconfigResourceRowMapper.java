/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.backend.pgconfig.resource;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.TimeZone;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.geoserver.platform.resource.Resource;
import org.springframework.jdbc.core.RowMapper;

/**
 * @since 1.4
 */
@RequiredArgsConstructor
public class PgconfigResourceRowMapper implements RowMapper<PgconfigResource> {

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    private final @NonNull PgconfigResourceStore store;

    /**
     * Expects the following columns:
     *
     * <pre>{@code
     * id         BIGINT
     * parentid   BIGINT
     * "type"     resourcetype
     * path       TEXT
     * mtime      timestamp
     * }</pre>
     */
    @Override
    public PgconfigResource mapRow(ResultSet rs, int rowNum) throws SQLException {
        long id = rs.getLong("id");
        long parentId = rs.getLong("parentid");
        Resource.Type type = Resource.Type.valueOf(rs.getString("type"));
        String path = rs.getString("path");
        // the mtime column is a timestamp without time zone holding UTC wall-clock values
        // (written as timezone('UTC', now())); without an explicit UTC calendar here, the
        // driver would interpret those UTC values as being in the JVM's default time zone,
        // shifting the resulting epoch millis by the JVM's UTC offset
        long mtime = rs.getTimestamp("mtime", Calendar.getInstance(UTC)).getTime();
        return new PgconfigResource(store, id, parentId, type, path, mtime);
    }

    public PgconfigResource undefined(String path) {
        return PgconfigResource.undefined(store, path);
    }
}
