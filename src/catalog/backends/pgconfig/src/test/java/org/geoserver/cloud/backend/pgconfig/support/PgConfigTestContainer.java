/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.backend.pgconfig.support;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import lombok.Getter;
import lombok.SneakyThrows;
import org.geoserver.cloud.autoconfigure.jndi.SimpleJNDIStaticContextInitializer;
import org.geoserver.cloud.config.catalog.backend.pgconfig.PgconfigDatabaseMigrations;
import org.geoserver.cloud.config.jndi.JNDIDataSourceConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.AbstractApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * A {@link Testcontainers test container} based on {@link PostgreSQLContainer} using PostgreSQL 15
 * to aid in setting up the {@code DataSource}, {@code JdbcTemplate}, and {@link
 * PgconfigDatabaseMigrations Flyway} database migrations for the {@literal pgconfig} catalog
 * backend.
 *
 * @since 1.6
 */
@SuppressWarnings("java:S119")
public class PgConfigTestContainer<SELF extends PostgreSQLContainer<SELF>> extends PostgreSQLContainer<SELF> {

    private @Getter DataSource dataSource;
    private @Getter JdbcTemplate template;
    private @Getter String schema = "pgconfigtest";
    private @Getter PgconfigDatabaseMigrations databaseMigrations;

    public PgConfigTestContainer() {
        super("postgres:15");
    }

    @SneakyThrows(Exception.class)
    public PgConfigTestContainer<SELF> setUp() {
        String url = getJdbcUrl();
        String username = getUsername();
        String password = getPassword();
        String driverClassName = getDriverClassName();

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(url);
        hikariConfig.setPassword(password);
        hikariConfig.setUsername(username);
        hikariConfig.setDriverClassName(driverClassName);
        hikariConfig.setSchema(schema);
        dataSource = new HikariDataSource(hikariConfig);
        template = new JdbcTemplate(dataSource);
        databaseMigrations = new PgconfigDatabaseMigrations()
                .setSchema(schema)
                .setDataSource(dataSource)
                .setCleanDisabled(false);
        databaseMigrations.migrate();
        return this;
    }

    public void tearDown() {
        if (null != databaseMigrations) {
            databaseMigrations.clean();
        }
        if (null != dataSource) {
            ((HikariDataSource) dataSource).close();
        }
    }

    @SuppressWarnings("unchecked")
    public <R extends AbstractApplicationContextRunner<?, ?, ?>> R withJdbcUrlConfig(R runner) {
        String url = getJdbcUrl();
        String username = getUsername();
        String password = getPassword();
        return (R) runner.withPropertyValues( //
                "geoserver.backend.pgconfig.enabled=true", //
                "geoserver.backend.pgconfig.datasource.url=" + url, //
                "geoserver.backend.pgconfig.datasource.username=" + username, //
                "geoserver.backend.pgconfig.datasource.password=" + password //
                );
    }

    @SuppressWarnings("unchecked")
    public <R extends AbstractApplicationContextRunner<?, ?, ?>> R withJndiConfig(R runner) {
        String url = getJdbcUrl();
        String username = getUsername();
        String password = getPassword();
        return (R) runner
                // enable simplejndi
                .withInitializer(new SimpleJNDIStaticContextInitializer())
                .withConfiguration(AutoConfigurations.of(JNDIDataSourceConfiguration.class))
                .withPropertyValues(
                        "geoserver.backend.pgconfig.enabled: true", //
                        // java:comp/env/jdbc/testdb config properties
                        "jndi.datasources.testdb.enabled: true", //
                        "jndi.datasources.testdb.url: " + url,
                        "jndi.datasources.testdb.username: " + username, //
                        "jndi.datasources.testdb.password: " + password, //
                        // pgconfig backend datasource config using jndi
                        "geoserver.backend.pgconfig.datasource.jndi-name: java:comp/env/jdbc/testdb");
    }
}
