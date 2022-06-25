/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.catalog.backend.jdbcconfig;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.geoserver.cloud.config.catalog.backend.jdbcconfig.JdbcConfigUpdateSequence;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.stream.IntStream;

@SpringBootTest(
        classes = AutoConfigurationTestConfiguration.class,
        properties = "geoserver.backend.jdbcconfig.enabled=true")
public class JdbcConfigUpdateSequenceTest extends JDBCConfigTest {

    private @Autowired JdbcConfigUpdateSequence updateSequence;

    @Disabled(
            "Couldn't get rid of the DB closed error if running more than one test, so better run the parallel one")
    public @Test void testUpdateSequence() {
        final long initial = updateSequence.currValue();
        long v = updateSequence.currValue();
        assertEquals(initial, v);
        v = updateSequence.nextValue();
        assertEquals(1 + initial, v);
        v = updateSequence.currValue();
        assertEquals(1 + initial, v);
        v = updateSequence.nextValue();
        assertEquals(2 + initial, v);
        v = updateSequence.currValue();
        assertEquals(2 + initial, v);
    }

    public @Test void multiThreadedTest() {
        final int incrementCount = 10_000;
        final long initial = updateSequence.currValue();
        final long expected = initial + incrementCount;

        IntStream.range(0, incrementCount).parallel().forEach(i -> updateSequence.nextValue());

        long v = updateSequence.currValue();
        assertEquals(expected, v);
    }
}
