/*
 * (c) 2023 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.catalog.backend.pgconfig;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.geoserver.cloud.backend.pgconfig.support.PgConfigTestContainer;
import org.geoserver.cloud.config.catalog.backend.pgconfig.PgconfigBackendProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * @since 1.4
 */
@Testcontainers(disabledWithoutDocker = true)
class PgconfigMigrationAutoConfigurationTest {

    @Container
    static PgConfigTestContainer<?> container = new PgConfigTestContainer<>();

    private ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    PgconfigDataSourceAutoConfiguration.class, PgconfigMigrationAutoConfiguration.class));

    @Test
    void testMigration_enabledByDefault() {
        container.withJdbcUrlConfig(runner).run(context -> {
            assertThat(context)
                    .hasNotFailed()
                    .hasSingleBean(PgconfigBackendProperties.class)
                    .hasBean("pgconfigDataSource");

            PgconfigBackendProperties config = context.getBean(PgconfigBackendProperties.class);
            DataSource ds = context.getBean("pgconfigDataSource", DataSource.class);
            assertDbSchema(ds, config);
        });
    }

    private void assertDbSchema(DataSource ds, PgconfigBackendProperties config) throws SQLException {
        String schema = config.schema();
        Map<String, String> expected = buildExpected(schema);
        Map<String, String> actual = findTables(ds, schema);
        assertThat(actual).isEqualTo(expected);
    }

    /**
     * @param ds
     * @param schema
     * @return
     * @throws SQLException
     */
    private Map<String, String> findTables(DataSource ds, String schema) throws SQLException {
        Map<String, String> actual = new TreeMap<>();
        try (Connection c = ds.getConnection()) {
            try (ResultSet tables = c.getMetaData().getTables(null, schema, null, null)) {
                while (tables.next()) {
                    String schem = tables.getString("TABLE_SCHEM");
                    String name = tables.getString("TABLE_NAME");
                    String type = tables.getString("TABLE_TYPE");
                    if (Set.of("VIEW", "TABLE", "SEQUENCE").contains(type)) {
                        actual.put("%s.%s".formatted(schem, name), type);
                    }
                }
            }
        }
        return actual;
    }

    /**
     * @param schema
     * @return
     */
    private Map<String, String> buildExpected(String schema) {
        List<String> views = List.of(
                "workspaceinfos",
                "namespaceinfos",
                "storeinfos",
                "resourceinfos",
                "layerinfos",
                "layergroupinfos",
                "styleinfos",
                "settingsinfos",
                "serviceinfos",
                "publishedinfos",
                "tilelayers");
        List<String> tables = List.of(
                "flyway_schema_history",
                "cataloginfo",
                "layergroupinfo",
                "layerinfo",
                "namespaceinfo",
                "publishedinfo",
                "resourceinfo",
                "storeinfo",
                "styleinfo",
                "workspaceinfo",
                "geoserverinfo",
                "settingsinfo",
                "serviceinfo",
                "logginginfo",
                "resourcestore",
                "resource_lock");
        List<String> sequences = List.of("gs_update_sequence", "resourcestore_id_seq");

        Map<String, String> expected = new TreeMap<>();
        expected.putAll(buildExpected(schema, "VIEW", views));
        expected.putAll(buildExpected(schema, "TABLE", tables));
        expected.putAll(buildExpected(schema, "SEQUENCE", sequences));
        return expected;
    }

    private Map<String, String> buildExpected(String schema, String type, List<String> names) {
        return names.stream()
                .map(nname -> "%s.%s".formatted(schema, nname))
                .collect(Collectors.toMap(Function.identity(), s -> type));
    }
}
