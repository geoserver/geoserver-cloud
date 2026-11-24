/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
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

@SpringBootTest(
        classes = GeoServerApplication.class,
        webEnvironment = WebEnvironment.RANDOM_PORT,
        args = "-Ddebug=true",
        properties = {"geoserver.services=wfs"})
@ActiveProfiles("datadir")
class WfsApplicationDataDirectoryIT extends WfsApplicationTest {

    static @TempDir Path datadir;

    // the gwc service stays off in this role, but gwc is on the combined app's classpath
    // regardless; its default cache-directory location, derived from the data-directory
    // location, must be pinned explicitly here rather than left to resolve from the dynamic
    // datadir path
    @DynamicPropertySource
    static void setUpDataDir(DynamicPropertyRegistry registry) {
        registry.add("geoserver.backend.data-directory.location", datadir::toAbsolutePath);
        registry.add("gwc.cache-directory", () -> datadir.resolve("gwc").toAbsolutePath());
    }
}
