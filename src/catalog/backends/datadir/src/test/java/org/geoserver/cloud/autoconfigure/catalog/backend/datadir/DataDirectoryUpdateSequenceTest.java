/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.catalog.backend.datadir;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.geoserver.cloud.config.catalog.backend.datadirectory.DataDirectoryBackendConfiguration;
import org.geoserver.cloud.config.catalog.backend.datadirectory.DataDirectoryUpdateSequence;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.stream.IntStream;

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
public class DataDirectoryUpdateSequenceTest {

    private @Autowired DataDirectoryUpdateSequence updateSequence;

    public @Test void sequentialTest() {
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
