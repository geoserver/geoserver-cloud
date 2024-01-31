/*
 * (c) 2023 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.catalog.backend.pgconfig;

import static org.assertj.core.api.Assertions.assertThat;

import org.geoserver.GeoServerConfigurationLock;
import org.geoserver.catalog.plugin.CatalogPlugin;
import org.geoserver.catalog.plugin.ExtendedCatalogFacade;
import org.geoserver.cloud.backend.pgconfig.catalog.PgsqlCatalogFacade;
import org.geoserver.cloud.backend.pgconfig.config.PgsqlConfigRepository;
import org.geoserver.cloud.backend.pgconfig.config.PgsqlGeoServerFacade;
import org.geoserver.cloud.backend.pgconfig.config.PgsqlUpdateSequence;
import org.geoserver.cloud.backend.pgconfig.resource.PgsqlLockProvider;
import org.geoserver.cloud.backend.pgconfig.resource.PgsqlResourceStore;
import org.geoserver.cloud.backend.pgconfig.support.PgConfigTestContainer;
import org.geoserver.cloud.config.catalog.backend.pgconfig.PgsqlGeoServerLoader;
import org.geoserver.cloud.config.catalog.backend.pgconfig.PgsqlGeoServerResourceLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Test suite for {@link PgsqlBackendAutoConfiguration}
 *
 * @since 1.4
 */
@Testcontainers(disabledWithoutDocker = true)
class PgsqlBackendAutoConfigurationTest {

    @Container static PgConfigTestContainer<?> container = new PgConfigTestContainer<>();

    private ApplicationContextRunner runner =
            new ApplicationContextRunner()
                    .withConfiguration(
                            AutoConfigurations.of(
                                    PgsqlDataSourceAutoConfiguration.class,
                                    PgsqlMigrationAutoConfiguration.class,
                                    PgsqlBackendAutoConfiguration.class));

    @BeforeEach
    void setUp() throws Exception {
        runner = container.withJdbcUrlConfig(runner);
    }

    @Test
    void testCatalogAndConfigBeans() {
        runner.run(
                context -> {
                    assertThat(context)
                            .hasNotFailed()
                            .hasSingleBean(JdbcTemplate.class)
                            .hasSingleBean(GeoServerConfigurationLock.class)
                            .hasSingleBean(PgsqlUpdateSequence.class)
                            .hasSingleBean(PgsqlCatalogFacade.class)
                            .hasSingleBean(PgsqlGeoServerLoader.class)
                            .hasSingleBean(PgsqlConfigRepository.class)
                            .hasSingleBean(PgsqlGeoServerFacade.class)
                            .hasSingleBean(PgsqlResourceStore.class)
                            .hasSingleBean(PgsqlGeoServerResourceLoader.class)
                            .hasSingleBean(PgsqlLockProvider.class);

                    ExtendedCatalogFacade catalogFacade =
                            context.getBean("catalogFacade", ExtendedCatalogFacade.class);
                    assertThat(catalogFacade).isInstanceOf(PgsqlCatalogFacade.class);

                    CatalogPlugin catalog = context.getBean("rawCatalog", CatalogPlugin.class);
                    assertThat(catalog.getRawFacade()).isSameAs(catalogFacade);
                });
    }
}
