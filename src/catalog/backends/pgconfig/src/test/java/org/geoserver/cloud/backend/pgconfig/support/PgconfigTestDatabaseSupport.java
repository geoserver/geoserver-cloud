/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.backend.pgconfig.support;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.Statement;
import javax.sql.DataSource;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.cloud.config.catalog.backend.pgconfig.PgconfigDatabaseMigrations;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.boot.test.context.runner.AbstractApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * JUnit 5 extension providing per-test PostgreSQL schema isolation for concurrent test execution.
 *
 * <p>Creates a unique schema, {@link DataSource}, {@link JdbcTemplate}, and runs Flyway migrations before each test,
 * and drops the schema after each test.
 *
 * <p>Usage with {@code @RegisterExtension}:
 *
 * <pre>{@code
 * @Container
 * static PgConfigTestContainer container = new PgConfigTestContainer();
 *
 * @RegisterExtension
 * PgconfigTestDatabaseSupport db = new PgconfigTestDatabaseSupport(container);
 * }</pre>
 *
 * <p>The extension's {@code beforeEach} runs before {@code @BeforeEach} methods, ensuring the database schema is
 * migrated when conformance test {@code setUp()} calls {@code super.setUp()}.
 *
 * @since 3.0.0
 */
@Slf4j
public class PgconfigTestDatabaseSupport implements BeforeEachCallback, AfterEachCallback {

    private final PostgreSQLContainer container;

    private @Getter DataSource dataSource;
    private @Getter JdbcTemplate template;
    private @Getter String schema;

    public PgconfigTestDatabaseSupport(PostgreSQLContainer container) {
        this.container = container;
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        this.schema = deriveSchemaName(context);
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(container.getJdbcUrl());
        config.setUsername(container.getUsername());
        config.setPassword(container.getPassword());
        config.setDriverClassName(container.getDriverClassName());
        config.setSchema(schema);
        config.setMaximumPoolSize(3);
        config.setMinimumIdle(1);
        config.setPoolName("test-" + schema.substring(0, Math.min(20, schema.length())));
        this.dataSource = new HikariDataSource(config);
        this.template = new JdbcTemplate(dataSource);

        PgconfigDatabaseMigrations migrations = new PgconfigDatabaseMigrations()
                .setSchema(schema)
                .setDataSource(dataSource)
                .setCleanDisabled(false);
        migrations.migrate();
    }

    @Override
    public void afterEach(ExtensionContext context) {
        // Close the pool first to release all connections
        if (dataSource instanceof HikariDataSource hds) {
            hds.close();
        }
        // Drop the schema with a direct JDBC connection (avoids pool search_path issues)
        if (schema != null) {
            try (Connection conn = java.sql.DriverManager.getConnection(
                            container.getJdbcUrl(), container.getUsername(), container.getPassword());
                    Statement stmt = conn.createStatement()) {
                stmt.execute("DROP SCHEMA IF EXISTS \"" + schema + "\" CASCADE");
            } catch (Exception e) {
                log.warn("Failed to drop test schema '{}', will be cleaned when container stops", schema, e);
            }
        }
        dataSource = null;
        template = null;
        schema = null;
    }

    /**
     * Configures an {@link AbstractApplicationContextRunner} with JDBC properties and the test-specific schema. Must be
     * called from {@code @BeforeEach} (after the extension's {@code beforeEach} has set the schema).
     */
    @SuppressWarnings("unchecked")
    public <R extends AbstractApplicationContextRunner<?, ?, ?>> R withJdbcUrlConfig(R runner) {
        String baseUrl = container.getJdbcUrl();
        String url = baseUrl + (baseUrl.contains("?") ? "&" : "?") + "currentSchema=" + schema;
        return (R) runner.withPropertyValues(
                "geoserver.backend.pgconfig.enabled=true",
                "geoserver.backend.pgconfig.schema=" + schema,
                "geoserver.backend.pgconfig.datasource.url=" + url,
                "geoserver.backend.pgconfig.datasource.username=" + container.getUsername(),
                "geoserver.backend.pgconfig.datasource.password=" + container.getPassword());
    }

    static String deriveSchemaName(ExtensionContext context) {
        String className = context.getRequiredTestClass().getSimpleName();
        String displayName = context.getDisplayName();
        String uniqueId = context.getUniqueId();
        // Readable prefix from class + display name
        String prefix = (className + "_" + displayName)
                .toLowerCase()
                .replaceAll("[^a-z0-9_]", "_")
                .replaceAll("_+", "_");
        // Hash of unique ID for guaranteed uniqueness (handles parameterized tests)
        String hash = String.format("%08x", uniqueId.hashCode());
        int maxPrefix = 63 - hash.length() - 1;
        if (prefix.length() > maxPrefix) {
            prefix = prefix.substring(0, maxPrefix);
        }
        return prefix + "_" + hash;
    }
}
