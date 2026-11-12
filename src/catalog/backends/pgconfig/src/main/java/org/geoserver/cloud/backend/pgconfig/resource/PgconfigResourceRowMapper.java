/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.backend.pgconfig.resource;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.TimeZone;
import org.geoserver.platform.resource.Resource;
import org.springframework.jdbc.core.RowMapper;

/** @since 1.4 */
public class PgconfigResourceRowMapper implements RowMapper<ResourceRow> {

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    /**
     * Expects the following columns:
     *
     * <pre>{@code
     * id         BIGINT
     * parentid   BIGINT
     * "type"     resourcetype
     * path       TEXT
     * mtime      timestamp
     * size       INTEGER, the octet_length of the content, null for a directory
     * }</pre>
     */
    @Override
    public ResourceRow mapRow(ResultSet rs, int rowNum) throws SQLException {
        long id = rs.getLong("id");
        long parentId = rs.getLong("parentid");
        Resource.Type type = Resource.Type.valueOf(rs.getString("type"));
        String path = rs.getString("path");
        long mtime = mtime(rs);
        // getLong reads the null size of a directory as 0
        long size = rs.getLong("size");
        return new ResourceRow(id, parentId, type, path, mtime, size);
    }

    /** Reads the {@code mtime} column, in milliseconds since the epoch. */
    static long mtime(ResultSet rs) throws SQLException {
        // the mtime column is a timestamp without time zone holding UTC wall-clock values
        // (written as timezone('UTC', now())); without an explicit UTC calendar here, the
        // driver would interpret those UTC values as being in the JVM's default time zone,
        // shifting the resulting epoch millis by the JVM's UTC offset
        return rs.getTimestamp("mtime", Calendar.getInstance(UTC)).getTime();
    }
}
