/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.restconfig;

import org.geoserver.cloud.backend.pgconfig.support.PgConfigTestContainer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"test", "pgconfigjndi"})
@Testcontainers(disabledWithoutDocker = true)
class RestConfigApplicationPgconfigIT extends RestConfigApplicationTest {

    @Container
    static PgConfigTestContainer container = new PgConfigTestContainer();

    /**
     * Contribute the following properties defined in the {@literal pgconfigjndi}
     * spring profile
     *
     * <ul>
     * <li>pgconfig.host
     * <li>pgconfig.port
     * <li>pgconfig.database
     * <li>pgconfig.schema
     * <li>pgconfig.username
     * <li>pgconfig.password
     * </ul>
     */
    @DynamicPropertySource
    static void setUpDataDir(DynamicPropertyRegistry registry) {
        container.setupDynamicPropertySource(registry);
    }
}
