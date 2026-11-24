/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.restconfig;

import java.nio.file.Path;
import org.geoserver.cloud.backend.pgconfig.support.PgConfigTestContainer;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@ActiveProfiles("pgconfigjndi")
@Testcontainers(disabledWithoutDocker = true)
class RestConfigApplicationPgconfigIT extends RestConfigApplicationTest {

    @Container
    static PgConfigTestContainer container = new PgConfigTestContainer();

    static @TempDir Path gwcCacheDir;

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
     * <p>Also pins gwc's cache directory: gwc is on the combined app's classpath whichever services are enabled, and
     * this profile disables the data-directory backend gwc would otherwise derive its default cache location from.
     */
    @DynamicPropertySource
    static void setUpDataDir(DynamicPropertyRegistry registry) {
        container.setupDynamicPropertySource(registry);
        registry.add("gwc.cache-directory", gwcCacheDir::toAbsolutePath);
    }
}
