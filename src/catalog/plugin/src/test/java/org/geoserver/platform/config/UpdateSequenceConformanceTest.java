/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.platform.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.IntStream;
import org.geoserver.config.GeoServer;
import org.junit.jupiter.api.Test;

/**
 * @since 1.2
 */
public interface UpdateSequenceConformanceTest {

    UpdateSequence getUpdataSequence();

    GeoServer getGeoSever();

    default @Test void testUpdateSequence() {
        UpdateSequence updateSequence = getUpdataSequence();
        GeoServer geoSever = getGeoSever();
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
        assertEquals(2 + initial, geoSever.getGlobal().getUpdateSequence());
    }

    default @Test void multiThreadedTest() {
        UpdateSequence updateSequence = getUpdataSequence();
        GeoServer geoSever = getGeoSever();
        final int incrementCount = 1_000;
        final long initial = updateSequence.currValue();
        final long expected = initial + incrementCount;

        IntStream.range(0, incrementCount).parallel().forEach(i -> updateSequence.nextValue());

        long v = updateSequence.currValue();
        assertEquals(expected, v);
        assertEquals(expected, geoSever.getGlobal().getUpdateSequence());
    }
}
