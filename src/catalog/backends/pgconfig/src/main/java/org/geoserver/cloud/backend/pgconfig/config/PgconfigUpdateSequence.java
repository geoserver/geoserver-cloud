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
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.impl.GeoServerInfoImpl;
import org.geoserver.platform.config.UpdateSequence;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The update sequence of GeoServer in a PostgreSQL sequence. Each new value is also saved in the global settings.
 *
 * @since 1.4
 */
public class PgconfigUpdateSequence implements UpdateSequence {

    private static final String SEQUENCE_NAME = "gs_update_sequence";

    // not using CURRVAL() to avoid the "currval of sequence "<name>" is not yet defined in this
    // session" error
    private static final String GET_QUERY = "SELECT last_value FROM %s".formatted(SEQUENCE_NAME);

    private static final String INCREMENT_AND_GET_QUERY = "SELECT NEXTVAL('%s')".formatted(SEQUENCE_NAME);

    private final DataSource dataSource;
    private final PgconfigGeoServerFacade geoServer;
    private final TransactionTemplate transaction;

    /**
     * @param dataSource the data source of the configuration repository of the facade: the global settings are saved
     *     within a transaction on it
     */
    public PgconfigUpdateSequence(@NonNull DataSource dataSource, @NonNull PgconfigGeoServerFacade geoServer) {
        this.dataSource = dataSource;
        this.geoServer = geoServer;
        this.transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
    }

    @Override
    public long currValue() {
        return runAndGetLong(GET_QUERY);
    }

    /**
     * Increments the sequence, and saves the new value in the global settings. The save writes the whole global
     * settings back. A transaction makes the configuration repository read them from the database rather than from its
     * cache: a cached copy could miss a change made by another service instance, and the save would undo it.
     */
    @Override
    public synchronized long nextValue() {
        long nextValue = runAndGetLong(INCREMENT_AND_GET_QUERY);
        transaction.executeWithoutResult(status -> saveInGlobalSettings(nextValue));
        return nextValue;
    }

    private void saveInGlobalSettings(long updateSequence) {
        GeoServerInfo global = geoServer.getGlobal();
        if (null == global) {
            global = new GeoServerInfoImpl();
            global.setUpdateSequence(updateSequence);
            geoServer.setGlobal(global);
        } else {
            global.setUpdateSequence(updateSequence);
            geoServer.save(global);
        }
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
