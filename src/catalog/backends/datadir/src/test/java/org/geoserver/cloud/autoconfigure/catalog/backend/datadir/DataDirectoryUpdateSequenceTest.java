/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.catalog.backend.datadir;

import java.io.IOException;
import java.nio.file.Path;
import org.geoserver.cloud.config.catalog.backend.datadirectory.DataDirectoryBackendConfiguration;
import org.geoserver.cloud.config.catalog.backend.datadirectory.DataDirectoryUpdateSequence;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.config.UpdateSequence;
import org.geoserver.platform.config.UpdateSequenceConformanceTest;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Test {@link DataDirectoryBackendConfiguration} through {@link DataDirectoryAutoConfiguration}
 * when {@code geoserver.backend.data-directory.enabled=true}
 */
@SpringBootTest(
        classes = DataDirectoryTestConfiguration.class, //
        properties = {
            "geoserver.backend.dataDirectory.enabled=true",
        })
@ActiveProfiles("test")
class DataDirectoryUpdateSequenceTest implements UpdateSequenceConformanceTest {

    private @Autowired DataDirectoryUpdateSequence updateSequence;
    private @Autowired GeoServer geoserver;
    static @TempDir Path datadir;

    @DynamicPropertySource
    static void setUpDataDir(DynamicPropertyRegistry registry) throws IOException {
        registry.add("geoserver.backend.data-directory.location", datadir::toAbsolutePath);
    }

    @Override
    public UpdateSequence getUpdataSequence() {
        return updateSequence;
    }

    @Override
    public GeoServer getGeoSever() {
        return geoserver;
    }
}
