/*
 * (c) 2023 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.backend.pgsql.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import org.geoserver.cloud.config.catalog.backend.pgsql.PgsqlDatabaseMigrations;
import org.geoserver.config.GeoServer;
import org.geoserver.config.plugin.GeoServerImpl;
import org.geoserver.platform.config.UpdateSequence;
import org.geoserver.platform.config.UpdateSequenceConformanceTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;

/**
 * @since 1.4
 */
@Testcontainers(disabledWithoutDocker = true)
class PgsqlUpdateSequenceTest implements UpdateSequenceConformanceTest {

    @Container static PostgreSQLContainer<?> container = new PostgreSQLContainer<>("postgres:15");

    static DataSource dataSource;
    static final String schema = "testschema";
    static PgsqlDatabaseMigrations databaseMigrations;

    static @BeforeAll void createDataSource() throws Exception {
        String url = container.getJdbcUrl();
        String username = container.getUsername();
        String password = container.getPassword();
        String driverClassName = container.getDriverClassName();

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(url);
        hikariConfig.setPassword(password);
        hikariConfig.setUsername(username);
        hikariConfig.setDriverClassName(driverClassName);
        hikariConfig.setSchema(schema);
        dataSource = new HikariDataSource(hikariConfig);
        databaseMigrations =
                new PgsqlDatabaseMigrations()
                        .setSchema(schema)
                        .setDataSource(dataSource)
                        .setCleanDisabled(false);
    }

    private UpdateSequence sequence;
    private PgsqlGeoServerFacade facade;
    private GeoServer geoserver;

    @BeforeEach
    public void init() throws Exception {
        databaseMigrations.migrate();
        facade = new PgsqlGeoServerFacade(new JdbcTemplate(dataSource));
        geoserver = new GeoServerImpl(facade);
        sequence = new PgsqlUpdateSequence(dataSource, facade);
    }

    @AfterEach
    void cleanDb() throws Exception {
        databaseMigrations.clean();
    }

    @Override
    public UpdateSequence getUpdataSequence() {
        return sequence;
    }

    @Override
    public GeoServer getGeoSever() {
        return geoserver;
    }
}
