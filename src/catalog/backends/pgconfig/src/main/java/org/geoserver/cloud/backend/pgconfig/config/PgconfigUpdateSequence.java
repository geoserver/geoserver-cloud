/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.backend.pgconfig.config;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.impl.GeoServerInfoImpl;
import org.geoserver.platform.config.UpdateSequence;

/**
 * @since 1.4
 */
@RequiredArgsConstructor
public class PgconfigUpdateSequence implements UpdateSequence {

    private static final String SEQUENCE_NAME = "gs_update_sequence";

    // not using CURRVAL() to avoid the "currval of sequence "<name>" is not yet defined in this
    // session" error
    private static final String GET_QUERY = "SELECT last_value FROM %s".formatted(SEQUENCE_NAME);

    private static final String INCREMENT_AND_GET_QUERY = "SELECT NEXTVAL('%s')".formatted(SEQUENCE_NAME);

    private final @NonNull DataSource dataSource;
    private final @NonNull PgconfigGeoServerFacade geoServer;

    @Override
    public long currValue() {
        return runAndGetLong(GET_QUERY);
    }

    @Override
    public synchronized long nextValue() {
        long nextValue = runAndGetLong(INCREMENT_AND_GET_QUERY);
        GeoServerInfo global = geoServer.getGlobal();
        if (null == global) {
            global = new GeoServerInfoImpl();
            global.setUpdateSequence(nextValue);
            geoServer.setGlobal(global);
        } else {
            global.setUpdateSequence(nextValue);
            geoServer.save(global);
        }
        return nextValue;
    }

    protected long runAndGetLong(String query) {
        try (Connection c = dataSource.getConnection()) {
            c.setAutoCommit(false);
            c.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            try (Statement st = c.createStatement();
                    ResultSet rs = st.executeQuery(query)) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                throw new IllegalStateException("Query did not return a result: " + GET_QUERY);
            } finally {
                c.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }
}
