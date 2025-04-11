/*
 * (c) 2025 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.restconfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"test", "datadir"})
public class RestConfigApplicationDataDirectoryIT extends RestConfigApplicationTest {

    static @TempDir Path datadir;

    @DynamicPropertySource
    static void setUpDataDir(DynamicPropertyRegistry registry) throws IOException {
        var gwcdir = datadir.resolve("gwc");
        if (!Files.exists(gwcdir)) {
            Files.createDirectory(gwcdir);
        }
        registry.add("geoserver.backend.data-directory.location", datadir::toAbsolutePath);
        registry.add("gwc.cache-directory", gwcdir::toAbsolutePath);
    }
}
