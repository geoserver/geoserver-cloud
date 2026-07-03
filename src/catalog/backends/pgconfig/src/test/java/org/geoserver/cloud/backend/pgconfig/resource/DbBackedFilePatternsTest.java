/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.backend.pgconfig.resource;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.function.Predicate;
import org.geoserver.cloud.config.catalog.backend.pgconfig.PgconfigBackendProperties;
import org.junit.jupiter.api.Test;

class DbBackedFilePatternsTest {

    @Test
    void defaultPatternsTargetMosaicConfigFiles() {
        List<String> defaults = PgconfigBackendProperties.defaultDbBackedFilePatterns();
        assertThat(defaults).containsExactly("data/**/*.properties", "data/**/sample_image.dat");
        assertThat(new PgconfigBackendProperties().getDbBackedFilePatterns()).isEqualTo(defaults);
    }

    @Test
    void antPathMatcherMatchesWhitelistedFiles() {
        Predicate<String> matcher =
                PgconfigResourceStore.antPathMatcher(PgconfigBackendProperties.defaultDbBackedFilePatterns());

        assertThat(matcher.test("data/ws/store/indexer.properties")).isTrue();
        assertThat(matcher.test("data/ws/store/datastore.properties")).isTrue();
        assertThat(matcher.test("data/ws/store/timeregex.properties")).isTrue();
        assertThat(matcher.test("data/ws/store/sample_image.dat")).isTrue();
        assertThat(matcher.test("data/direct-child.properties")).isTrue();

        assertThat(matcher.test("data/ws/store/granule.tif")).isFalse();
        assertThat(matcher.test("data/ws/store")).isFalse();
        assertThat(matcher.test("data")).isFalse();
        assertThat(matcher.test("workspaces/ws/store.xml")).isFalse();
        assertThat(matcher.test("tmp/anything.properties")).isFalse();
    }
}
