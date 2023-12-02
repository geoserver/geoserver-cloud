/*
 * (c) 2023 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.backend.pgsql.catalog.repository;

import java.sql.SQLException;

/**
 * @since 1.4
 */
@SuppressWarnings("serial")
class UncheckedSqlException extends RuntimeException {

    UncheckedSqlException(SQLException cause) {
        super(cause);
    }

    @Override
    public synchronized SQLException getCause() {
        return (SQLException) super.getCause();
    }

    static UncheckedSqlException of(SQLException e) {
        return new UncheckedSqlException(e);
    }
}
