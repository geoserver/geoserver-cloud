/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.wms.app;

import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.File;

/** See {@code src/test/resources/bootstrap-testdatadir.yml} */
@SpringBootTest
@ActiveProfiles({"test", "testdatadir"})
public class WmsApplicationDataDirectoryTest extends WmsApplicationTest {

    private static @TempDir File tmpDataDir;

    @DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry registry) {
        String datadir = tmpDataDir.getAbsolutePath();
        registry.add("data_directory", () -> datadir);
    }
}
