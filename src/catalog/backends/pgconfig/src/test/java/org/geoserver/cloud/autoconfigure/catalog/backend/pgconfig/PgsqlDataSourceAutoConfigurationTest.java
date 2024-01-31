/*
 * (c) 2023 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.catalog.backend.pgconfig;

import static org.assertj.core.api.Assertions.assertThat;

import lombok.extern.slf4j.Slf4j;

import org.geoserver.cloud.backend.pgconfig.support.PgConfigTestContainer;
import org.geoserver.cloud.config.catalog.backend.pgconfig.PgsqlDataSourceConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeansException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

/**
 * Tests for {@link PgsqlDataSourceConfiguration}
 *
 * @since 1.4
 */
@Testcontainers(disabledWithoutDocker = true)
@Slf4j
class PgsqlDataSourceAutoConfigurationTest {

    @Container static PgConfigTestContainer<?> container = new PgConfigTestContainer<>();

    private ApplicationContextRunner runner =
            new ApplicationContextRunner()
                    .withConfiguration(
                            AutoConfigurations.of(PgsqlDataSourceAutoConfiguration.class));

    String url;
    String username;
    String password;

    @BeforeEach
    void setUp() throws Exception {
        url = container.getJdbcUrl();
        username = container.getUsername();
        password = container.getPassword();
    }

    /**
     * Test method for {@link
     * org.geoserver.cloud.config.catalog.backend.pgconfig.PgsqlDataSourceConfiguration#dataSource()}.
     */
    @Test
    void testDataSource() {
        container
                .withJdbcUrlConfig(runner)
                .run(
                        context -> {
                            assertThat(context)
                                    .hasNotFailed()
                                    .hasBean("pgsqlConfigDatasource")
                                    .getBean("pgsqlConfigDatasource")
                                    .isInstanceOf(DataSource.class);

                            assertIsPostgresql(context);
                        });
    }

    /**
     * Test method for {@link
     * org.geoserver.cloud.config.catalog.backend.pgconfig.PgsqlDataSourceConfiguration#jndiDataSource()}.
     */
    @Test
    void testJndiDataSource() {
        container
                .withJndiConfig(runner)
                .run(
                        context -> {
                            assertThat(context)
                                    .hasNotFailed()
                                    .hasBean("pgsqlConfigDatasource")
                                    .getBean("pgsqlConfigDatasource")
                                    .isInstanceOf(DataSource.class);
                            assertIsPostgresql(context);
                        });
    }

    private void assertIsPostgresql(AssertableApplicationContext context)
            throws BeansException, SQLException {
        try (Connection c =
                context.getBean("pgsqlConfigDatasource", DataSource.class).getConnection()) {
            assertThat(c.isValid(2)).isTrue();

            try (Statement st = c.createStatement();
                    ResultSet rs = st.executeQuery("SELECT version()")) {
                rs.next();
                String version = rs.getString(1);
                log.info("database version: " + version);
                assertThat(version).contains("PostgreSQL");
            }
        }
    }
}
