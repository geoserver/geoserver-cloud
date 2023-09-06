/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.catalog.backend.datadir;

import org.geoserver.cloud.config.catalog.backend.datadirectory.DataDirectoryBackendConfiguration;
import org.geoserver.cloud.config.catalog.backend.datadirectory.DataDirectoryUpdateSequence;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.config.UpdateSequence;
import org.geoserver.platform.config.UpdateSequenceConformanceTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Test {@link DataDirectoryBackendConfiguration} through {@link DataDirectoryAutoConfiguration}
 * when {@code geoserver.backend.data-directory.enabled=true}
 */
@SpringBootTest(
        classes = DataDirectoryTestConfiguration.class, //
        properties = {
            "geoserver.backend.dataDirectory.enabled=true",
            "geoserver.backend.dataDirectory.location=/tmp/data_dir_autoconfiguration_test"
        })
@ActiveProfiles("test")
public class DataDirectoryUpdateSequenceTest implements UpdateSequenceConformanceTest {

    private @Autowired DataDirectoryUpdateSequence updateSequence;
    private @Autowired GeoServer geoserver;

    @Override
    public UpdateSequence getUpdataSequence() {
        return updateSequence;
    }

    @Override
    public GeoServer getGeoSever() {
        return geoserver;
    }
}
