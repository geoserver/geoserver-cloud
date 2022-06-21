/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.catalog.backend.jdbcconfig;

import static org.junit.Assert.assertEquals;

import org.geoserver.cloud.config.catalog.backend.jdbcconfig.JdbcConfigUpdateSequence;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest(
        classes = AutoConfigurationTestConfiguration.class,
        properties = "geoserver.backend.jdbcconfig.enabled=true")
@RunWith(SpringRunner.class)
public class JdbcConfigUpdateSequenceTest extends JDBCConfigTest {

    private @Autowired JdbcConfigUpdateSequence updateSequence;

    public @Test void testUpdateSequence() {
        long v = updateSequence.get();
        assertEquals(0, v);
        v = updateSequence.incrementAndGet();
        assertEquals(1, v);
        v = updateSequence.get();
        assertEquals(1, v);
        v = updateSequence.incrementAndGet();
        assertEquals(2, v);
        v = updateSequence.get();
        assertEquals(2, v);
    }
}
