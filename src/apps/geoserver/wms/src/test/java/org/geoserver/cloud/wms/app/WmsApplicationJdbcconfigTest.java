/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.wms.app;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** See {@code src/test/resources/bootstrap-testjdbcconfig.yml} */
@SpringBootTest(properties = "gwc.wms-integration=true")
@ActiveProfiles({"test", "testjdbcconfig"})
class WmsApplicationJdbcconfigTest extends WmsApplicationTest {

    static @TempDir Path tmpdir;
    static Path datadir;

    @DynamicPropertySource
    static void setUpDataDir(DynamicPropertyRegistry registry) throws IOException {
        datadir = Files.createDirectory(tmpdir.resolve("jdbc-temp-datadir"));
        var gwcdir = Files.createDirectory(datadir.resolve("gwc"));
        registry.add("geoserver.backend.jdbcconfig.cache-directory", datadir.toAbsolutePath()::toString);
        registry.add("gwc.cache-directory", gwcdir::toAbsolutePath);
    }
}
