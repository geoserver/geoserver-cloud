/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.catalog.backend.jdbcconfig;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.geoserver.cloud.config.catalog.backend.jdbcconfig.JdbcConfigUpdateSequence;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        classes = AutoConfigurationTestConfiguration.class,
        properties = "geoserver.backend.jdbcconfig.enabled=true")
public class JdbcConfigUpdateSequenceTest extends JDBCConfigTest {

    private @Autowired JdbcConfigUpdateSequence updateSequence;

    public @Test void testUpdateSequence() {
        final long initial = updateSequence.get();
        long v = updateSequence.get();
        assertEquals(initial, v);
        v = updateSequence.incrementAndGet();
        assertEquals(1 + initial, v);
        v = updateSequence.get();
        assertEquals(1 + initial, v);
        v = updateSequence.incrementAndGet();
        assertEquals(2 + initial, v);
        v = updateSequence.get();
        assertEquals(2 + initial, v);
    }
}
