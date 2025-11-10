/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.catalog.backend.pgconfig;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.cloud.backend.pgconfig.support.PgConfigTestContainer;
import org.geoserver.cloud.config.catalog.backend.pgconfig.PconfigDataSourceConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeansException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Tests for {@link PconfigDataSourceConfiguration}
 *
 * @since 1.4
 */
@Testcontainers(disabledWithoutDocker = true)
@Slf4j
class PgconfigDataSourceAutoConfigurationTest {

    @Container
    static PgConfigTestContainer container = new PgConfigTestContainer();

    private ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PgconfigDataSourceAutoConfiguration.class));

    String url;
    String username;
    String password;

    @BeforeEach
    void setUp() {
        url = container.getJdbcUrl();
        username = container.getUsername();
        password = container.getPassword();
    }

    /**
     * Test method for {@link
     * org.geoserver.cloud.config.catalog.backend.pgconfig.PconfigDataSourceConfiguration#dataSource()}.
     */
    @Test
    void testDataSource() {
        container.withJdbcUrlConfig(runner).run(context -> {
            assertThat(context)
                    .hasNotFailed()
                    .hasBean("pgconfigDataSource")
                    .getBean("pgconfigDataSource")
                    .isInstanceOf(DataSource.class);

            assertIsPostgresql(context);
        });
    }

    /**
     * Test method for {@link
     * org.geoserver.cloud.config.catalog.backend.pgconfig.PconfigDataSourceConfiguration#jndiDataSource()}.
     */
    @Test
    void testJndiDataSource() {
        container.withJndiConfig(runner).run(context -> {
            assertThat(context)
                    .hasNotFailed()
                    .hasBean("pgconfigDataSource")
                    .getBean("pgconfigDataSource")
                    .isInstanceOf(DataSource.class);
            assertIsPostgresql(context);
        });
    }

    private void assertIsPostgresql(AssertableApplicationContext context) throws BeansException, SQLException {
        try (Connection c =
                context.getBean("pgconfigDataSource", DataSource.class).getConnection()) {
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
