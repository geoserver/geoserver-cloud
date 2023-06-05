/*
 * (c) 2023 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.backend.pgsql.gwc;

import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @since 1.4
 */
public class GeoServerTileLayerInfoRowMapper implements RowMapper<PgsqlTileLayerInfo> {

    @Override
    public PgsqlTileLayerInfo mapRow(ResultSet rs, int rowNum) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }
}
