/*
 * /* (c) 2014 Open Source Geospatial Foundation - all rights reserved (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.backend.pgsql.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import org.geoserver.catalog.Catalog;
import org.geoserver.cloud.backend.pgsql.PgsqlBackendBuilder;
import org.geoserver.cloud.config.catalog.backend.pgsql.PgsqlDatabaseMigrations;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerConfigConformanceTest;
import org.geoserver.config.plugin.GeoServerImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;

/**
 * @since 1.4
 */
@Testcontainers(disabledWithoutDocker = true)
class PgsqlConfigRepositoryConformanceTest extends GeoServerConfigConformanceTest {

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

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        databaseMigrations.migrate();
        super.setUp();
    }

    @AfterEach
    void cleanDb() throws Exception {
        databaseMigrations.clean();
    }

    protected @Override GeoServer createGeoServer() {
        PgsqlBackendBuilder builder = new PgsqlBackendBuilder(dataSource);
        Catalog catalog = builder.createCatalog();
        GeoServerImpl gs = builder.createGeoServer(catalog);
        return gs;
    }
}
