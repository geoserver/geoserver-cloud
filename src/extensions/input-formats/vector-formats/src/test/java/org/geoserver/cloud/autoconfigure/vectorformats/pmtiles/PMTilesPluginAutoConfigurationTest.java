/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.vectorformats.pmtiles;

import static org.assertj.core.api.Assertions.assertThat;

import io.tileverse.cache.CacheManager;
import org.geoserver.pmtiles.data.PMTilesWmsIntegrationConfiguration;
import org.geoserver.pmtiles.web.data.PMTilesWebUIConfiguration;
import org.geoserver.web.GeoServerApplication;
import org.geotools.autoconfigure.vectorformats.DataAccessFactoryFilteringAutoConfiguration;
import org.geotools.pmtiles.store.PMTilesDataStoreFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Test suite for {@link PMTilesPluginAutoConfiguration}
 *
 * @since 2.28
 */
class PMTilesPluginAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    DataAccessFactoryFilteringAutoConfiguration.class, PMTilesPluginAutoConfiguration.class));

    private CacheManager originalCacheManager;

    @BeforeEach
    void setUp() {
        originalCacheManager = CacheManager.getDefault();
    }

    @AfterEach
    void tearDown() {
        CacheManager.setDefault(originalCacheManager);
    }

    @Test
    void testAutoConfigurationLoadsAllBeans() {
        contextRunner.run(context -> assertThat(context)
                .hasNotFailed()
                .hasBean("pmTilesCacheInvalidator")
                .getBean("pmtilesStoreExtension")
                .hasFieldOrPropertyWithValue("enabled", true));
    }

    @Test
    void testConditionalOnPMTilesExtensionDisabled() {
        contextRunner
                .withPropertyValues("geoserver.extension.pmtiles.enabled=false")
                .run(context -> assertThat(context)
                        .hasNotFailed()
                        .doesNotHaveBean("pmTilesCacheInvalidator")
                        .getBean("pmtilesStoreExtension")
                        .hasFieldOrPropertyWithValue("enabled", false));
    }

    @Test
    void testConditionalOnPMTilesDataStoreFactoryClass() {
        contextRunner
                .withClassLoader(new FilteredClassLoader(PMTilesDataStoreFactory.class))
                .run(context -> assertThat(context).hasNotFailed().doesNotHaveBean("pmTilesCacheInvalidator"));
    }

    @Test
    void testCacheManagerIntegrationWithCaffeineCacheManager() {
        contextRunner
                .withBean(
                        org.springframework.cache.CacheManager.class,
                        org.springframework.cache.caffeine.CaffeineCacheManager::new,
                        bd -> bd.setPrimary(true))
                .run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(PMTilesPluginAutoConfiguration.class);

                    CacheManager cacheManager = io.tileverse.cache.CacheManager.getDefault();
                    assertThat(cacheManager)
                            .isInstanceOf(SpringCaffeineCacheManagerAdapter.class)
                            .isNotNull();
                });
    }

    @Test
    void testCacheManagerIntegrationWithoutCaffeineCacheManager() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(PMTilesPluginAutoConfiguration.class);

            CacheManager cacheManager = io.tileverse.cache.CacheManager.getDefault();
            assertThat(cacheManager).isNotInstanceOf(SpringCaffeineCacheManagerAdapter.class);
        });
    }

    @Nested
    class WebuiIntegration {

        @Test
        void testConditionalOnGeoServerWebUI() {
            contextRunner.run(context -> assertThat(context)
                    .hasNotFailed()
                    .doesNotHaveBean(PMTilesWebUIConfiguration.class)
                    .doesNotHaveBean("pmtilesDataStorePanel")
                    .doesNotHaveBean("radioGroupParamPanelCssContribution"));

            contextRunner
                    .withPropertyValues("geoserver.service.webui.enabled=true")
                    .withClassLoader(new FilteredClassLoader(GeoServerApplication.class))
                    .run(context -> assertThat(context)
                            .hasNotFailed()
                            .doesNotHaveBean(PMTilesWebUIConfiguration.class)
                            .doesNotHaveBean("pmtilesDataStorePanel")
                            .doesNotHaveBean("radioGroupParamPanelCssContribution"));

            contextRunner
                    .withPropertyValues("geoserver.service.webui.enabled=true")
                    .run(context -> assertThat(context)
                            .hasNotFailed()
                            .hasSingleBean(PMTilesWebUIConfiguration.class)
                            .hasBean("pmtilesDataStorePanel")
                            .hasBean("radioGroupParamPanelCssContribution"));
        }
    }

    @Nested
    class WmsIntegration {
        @Test
        void testConditionalOnGeoServerWMS() {
            contextRunner.run(context -> assertThat(context)
                    .hasNotFailed()
                    .doesNotHaveBean(PMTilesWmsIntegrationConfiguration.class)
                    .doesNotHaveBean("pmTilesScaleSetter"));

            contextRunner
                    .withPropertyValues("geoserver.service.wms.enabled=true")
                    .run(context -> assertThat(context)
                            .hasNotFailed()
                            .hasSingleBean(PMTilesWmsIntegrationConfiguration.class)
                            .hasBean("pmTilesScaleSetter"));
        }
    }
}
