/*
 * (c) 2023 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.backend.pgsql.catalog.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geoserver.cloud.config.catalog.backend.pgsql.PgsqlDatabaseMigrations;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import javax.sql.DataSource;

/**
 * @since 1.4
 */
@Testcontainers(disabledWithoutDocker = true)
class PgsqlWorkspaceRepositoryTest {

    @Container static PostgreSQLContainer<?> container = new PostgreSQLContainer<>("postgres:15");

    static DataSource dataSource;

    PgsqlWorkspaceRepository repo;

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
        dataSource = new HikariDataSource(hikariConfig);

        new PgsqlDatabaseMigrations().setDataSource(dataSource).migrate();
    }

    @BeforeEach
    void setUp() {
        repo = new PgsqlWorkspaceRepository(new JdbcTemplate(dataSource));
    }

    @Test
    void testAdd() {
        WorkspaceInfoImpl info = new WorkspaceInfoImpl();
        info.setId("ws1");
        info.setName("ws1");
        repo.add(info);
        Optional<WorkspaceInfo> found = repo.findById(info.getId(), repo.getContentType());
        assertThat(found.isPresent()).isTrue();
    }
}
