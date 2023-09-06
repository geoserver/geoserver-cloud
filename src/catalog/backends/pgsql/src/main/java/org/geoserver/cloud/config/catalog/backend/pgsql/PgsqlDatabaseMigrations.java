/*
 * (c) 2023 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.catalog.backend.pgsql;

import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import org.flywaydb.core.Flyway;

import javax.sql.DataSource;

/**
 * @since 1.4
 */
@Slf4j(topic = "org.geoserver.cloud.config.catalog.backend.pgsql")
@Data
@Accessors(chain = true)
public class PgsqlDatabaseMigrations {

    private DataSource dataSource;
    private boolean initialize = true;
    private String schema = "public";
    private boolean createSchema = true;
    private boolean cleanDisabled = true;

    public void migrate() throws Exception {
        if (!isInitialize()) {
            log.warn("Not initializing pgsql backend database as defined in configuration");
            return;
        }
        log.info("Running pgsql backend database migrations...");

        buildFlyway().migrate();
    }

    /** */
    public void clean() {
        buildFlyway().clean();
    }

    protected Flyway buildFlyway() {
        Flyway flyway =
                Flyway.configure()
                        .dataSource(dataSource)
                        .schemas(schema)
                        .createSchemas(createSchema)
                        .cleanDisabled(cleanDisabled)
                        .locations("db/pgsqlcatalog/migration")
                        .load();
        return flyway;
    }
}
