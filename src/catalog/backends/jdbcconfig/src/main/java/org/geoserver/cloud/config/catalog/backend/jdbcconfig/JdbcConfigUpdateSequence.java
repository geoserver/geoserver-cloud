/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.catalog.backend.jdbcconfig;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.geoserver.config.GeoServerFacade;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.jdbcconfig.internal.ConfigDatabase;
import org.geoserver.platform.config.UpdateSequence;
import org.springframework.beans.factory.InitializingBean;

/**
 * @since 1.0
 */
@RequiredArgsConstructor
public class JdbcConfigUpdateSequence implements UpdateSequence, InitializingBean {

    private static final String SEQUENCE_NAME = "gs_update_sequence";

    private final @NonNull DataSource dataSource;
    private final @NonNull CloudJdbcConfigProperties props;
    private final @NonNull GeoServerFacade geoServer;
    private final @NonNull ConfigDatabase db;

    private String incrementAndGetQuery;
    private String getQuery;

    @Override
    public long currValue() {
        return runAndGetLong(this.getQuery);
    }

    @Override
    public synchronized long nextValue() {
        long nextValue = runAndGetLong(this.incrementAndGetQuery);
        GeoServerInfo global = geoServer.getGlobal();
        if (global != null) {
            global.setUpdateSequence(nextValue);
            db.save(global);
        }
        return nextValue;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        String createSequenceStatement;
        if (props.isPostgreSQL()) {
            createSequenceStatement = "CREATE SEQUENCE IF NOT EXISTS %s AS BIGINT CYCLE".formatted(SEQUENCE_NAME);
            // not using CURRVAL() to avoid the "currval of sequence "<name>" is not yet defined in
            // this session" error
            getQuery = "SELECT last_value FROM %s".formatted(SEQUENCE_NAME);
            incrementAndGetQuery = "SELECT NEXTVAL('%s')".formatted(SEQUENCE_NAME);
        } else if (props.isH2()) {
            createSequenceStatement = "CREATE SEQUENCE IF NOT EXISTS %s".formatted(SEQUENCE_NAME);
            getQuery =
                    """
                    SELECT CURRENT_VALUE  \
                    FROM INFORMATION_SCHEMA.SEQUENCES \
                    WHERE SEQUENCE_NAME = '%s'
                    """
                            .formatted(SEQUENCE_NAME.toUpperCase());
            incrementAndGetQuery = "SELECT NEXTVAL('%s')".formatted(SEQUENCE_NAME);
        } else {
            throw new IllegalStateException("Db is not PostgreSQL nor H2");
        }
        try (Connection c = dataSource.getConnection();
                Statement st = c.createStatement()) {
            st.execute(createSequenceStatement);
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
                throw new IllegalStateException("Query did not return a result: %s".formatted(getQuery));
            } finally {
                c.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }
}
