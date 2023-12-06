/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.catalog.backend.jdbcconfig;

import org.geoserver.cloud.config.catalog.backend.jdbcconfig.JdbcConfigUpdateSequence;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.config.UpdateSequence;
import org.geoserver.platform.config.UpdateSequenceConformanceTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        classes = AutoConfigurationTestConfiguration.class,
        properties = "geoserver.backend.jdbcconfig.enabled=true")
class JdbcConfigUpdateSequenceTest extends JDBCConfigTest implements UpdateSequenceConformanceTest {

    private @Autowired JdbcConfigUpdateSequence updateSequence;

    @Override
    public UpdateSequence getUpdataSequence() {
        return updateSequence;
    }

    @Override
    public GeoServer getGeoSever() {
        return super.geoServer;
    }

    @Disabled(
            "Couldn't get rid of the DB closed error if running more than one test, so better just run the parallel one")
    @Override
    public @Test void testUpdateSequence() {
        // no-op
    }
}
