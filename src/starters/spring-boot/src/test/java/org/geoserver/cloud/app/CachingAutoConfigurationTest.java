/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.app;

import static org.assertj.core.api.Assertions.assertThat;

import org.geoserver.platform.GeoServerExtensions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.cache.autoconfigure.CacheAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.CacheManager;

class CachingAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(CachingAutoConfiguration.class, CacheAutoConfiguration.class));

    @Test
    void createsCacheManager() {
        runner.run(context -> assertThat(context).hasNotFailed().hasSingleBean(CacheManager.class));
    }

    @Test
    void noCacheManagerWithoutIt() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(CacheAutoConfiguration.class))
                .run(context -> assertThat(context).hasNotFailed().doesNotHaveBean(CacheManager.class));
    }

    @Test
    void skippedWithoutGeoServer() {
        runner.withClassLoader(new FilteredClassLoader(GeoServerExtensions.class))
                .run(context -> assertThat(context).hasNotFailed().doesNotHaveBean(CacheManager.class));
    }
}
