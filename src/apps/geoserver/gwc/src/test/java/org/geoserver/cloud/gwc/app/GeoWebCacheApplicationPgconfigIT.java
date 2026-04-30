/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.gwc.app;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers(disabledWithoutDocker = true)
class GeoWebCacheApplicationPgconfigIT extends GeoWebCacheApplicationTest {

    @Container
    static PostgreSQLContainer container = new PostgreSQLContainer("postgres:15");

    static @TempDir Path datadir;

    /**
     * Contribute the following properties defined in the {@literal pgconfigjndi} spring profile
     *
     * <ul>
     *   <li>pgconfig.host
     *   <li>pgconfig.port
     *   <li>pgconfig.database
     *   <li>pgconfig.schema
     *   <li>pgconfig.username
     *   <li>pgconfig.password
     * </ul>
     */
    @DynamicPropertySource
    static void setUpDataDir(DynamicPropertyRegistry registry) throws IOException {
        Path gwcdir = datadir.resolve("gwc");
        if (!Files.exists(gwcdir)) {
            Files.createDirectory(gwcdir);
        }
        registry.add("gwc.cache-directory", gwcdir::toAbsolutePath);
        registry.add("geoserver.backend.pgconfig.enabled", () -> "true");
        registry.add("geoserver.backend.pgconfig.jndi-name", () -> "pgconfig");
        registry.add("pgconfig.host", container::getHost);
        registry.add("pgconfig.port", () -> container.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT));
        registry.add("pgconfig.database", container::getDatabaseName);
        registry.add("pgconfig.schema", () -> "pgconfigtestschema");
        registry.add("pgconfig.username", container::getUsername);
        registry.add("pgconfig.password", container::getPassword);
    }
}
