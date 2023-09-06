/*
 * (c) 2023 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.catalog.backend.pgsql;

import static org.assertj.core.api.Assertions.assertThat;

import lombok.extern.slf4j.Slf4j;

import org.geoserver.cloud.config.catalog.backend.pgsql.PgsqlDataSourceConfiguration;
import org.geoserver.cloud.config.jndi.SimpleJNDIStaticContextInitializer;
import org.geoserver.cloud.config.jndidatasource.JNDIDataSourceAutoConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeansException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.containers.PostgreSQLContainer;
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

    @Container static PostgreSQLContainer<?> container = new PostgreSQLContainer<>("postgres:15");

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
     * org.geoserver.cloud.config.catalog.backend.pgsql.PgsqlDataSourceConfiguration#dataSource()}.
     */
    @Test
    void testDataSource() {
        runner.withPropertyValues( //
                        "geoserver.backend.pgconfig.enabled: true", //
                        "geoserver.backend.pgconfig.datasource.url: " + url, //
                        "geoserver.backend.pgconfig.datasource.username: " + username, //
                        "geoserver.backend.pgconfig.datasource.password: " + password //
                        )
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
     * org.geoserver.cloud.config.catalog.backend.pgsql.PgsqlDataSourceConfiguration#jndiDataSource()}.
     */
    @Test
    void testJndiDataSource() {
        runner
                // enable simplejndi
                .withInitializer(new SimpleJNDIStaticContextInitializer())
                .withConfiguration(AutoConfigurations.of(JNDIDataSourceAutoConfiguration.class))
                .withPropertyValues( //
                        "geoserver.backend.pgconfig.enabled: true", //
                        // java:comp/env/jdbc/testdb config properties
                        "jndi.datasources.testdb.url: " + url,
                        "jndi.datasources.testdb.username: " + username, //
                        "jndi.datasources.testdb.password: " + password, //
                        "jndi.datasources.testdb.enabled: true", //
                        // pgsql backend datasource config using jndi
                        "geoserver.backend.pgconfig.datasource.jndi-name: java:comp/env/jdbc/testdb")
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
