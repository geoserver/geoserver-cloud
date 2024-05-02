/*
 * (c) 2023 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.catalog.backend.pgconfig;

import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;

import javax.sql.DataSource;

/**
 * @since 1.4
 */
@Slf4j(topic = "org.geoserver.cloud.config.catalog.backend.pgconfig")
@Data
@Accessors(chain = true)
public class PgsqlDatabaseMigrations {

    private DataSource dataSource;
    private boolean initialize = true;
    private String schema = "public";
    private boolean createSchema = true;
    private boolean cleanDisabled = true;
    private MigrateResult result;

    public void migrate() throws Exception {
        if (!isInitialize()) {
            log.warn("Not initializing pgsql backend database as defined in configuration");
            return;
        }
        log.info("Running pgsql backend database migrations...");
        Flyway flyway = buildFlyway();
        result = flyway.migrate();
        log.debug(
                "pgsql backend database migration: success: {}, flyway version: {}, database: {}, schema:{}, migrations: {}",
                result.success,
                result.flywayVersion,
                result.database,
                result.schemaName,
                result.migrations == null ? 0 : result.migrations.size());
    }

    /**
     * Drops all objects (tables, views, procedures, triggers, ...) in the configured schemas.
     *
     * @see Flyway#clean()
     */
    public void clean() {
        Flyway flyway = buildFlyway();
        flyway.clean();
        result = null;
    }

    protected Flyway buildFlyway() {
        return Flyway.configure()
                .dataSource(dataSource)
                .schemas(schema)
                .createSchemas(createSchema)
                .cleanDisabled(cleanDisabled)
                .locations("db/pgsqlcatalog/migration")
                .load();
    }

    @Override
    public String toString() {
        MigrateResult r = this.result;
        if (null == r) {
            return "pgsql backend database migration: not run";
        }
        return "pgsql backend database migration: success: %s, flyway version: %s, database: %s, schema: %s, migrations: %s"
                .formatted(
                        r.success,
                        r.flywayVersion,
                        r.database,
                        r.schemaName,
                        r.migrations == null ? 0 : r.migrations.size());
    }
}
