/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.wfs.app;

import java.nio.file.Path;
import org.geoserver.cloud.geoserver.GeoServerApplication;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(
        classes = GeoServerApplication.class,
        webEnvironment = WebEnvironment.RANDOM_PORT,
        properties = {"geoserver.services=wfs"})
@ActiveProfiles("pgconfigjndi")
@Testcontainers(disabledWithoutDocker = true)
class WfsApplicationPgconfigIT extends WfsApplicationTest {

    @Container
    static PostgreSQLContainer container = new PostgreSQLContainer("postgres:15");

    static @TempDir Path gwcdir;

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
     *
     * <p>Also pins gwc's cache directory: the gwc service stays off in this role, but gwc is on the combined app's
     * classpath regardless, and with the data-directory backend disabled for this pgconfig profile there is no
     * data-directory location to derive gwc's default from.
     */
    @DynamicPropertySource
    static void setUpDataDir(DynamicPropertyRegistry registry) {
        registry.add("pgconfig.host", container::getHost);
        registry.add("pgconfig.port", () -> container.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT));
        registry.add("pgconfig.database", container::getDatabaseName);
        registry.add("pgconfig.schema", () -> "pgconfigtestschema");
        registry.add("pgconfig.username", container::getUsername);
        registry.add("pgconfig.password", container::getPassword);
        registry.add("gwc.cache-directory", gwcdir::toAbsolutePath);
    }
}
