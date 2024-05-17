/*
 * (c) 2023 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.catalog.backend.pgconfig;

import static org.assertj.core.api.Assertions.assertThat;

import org.geoserver.GeoServerConfigurationLock;
import org.geoserver.catalog.plugin.ExtendedCatalogFacade;
import org.geoserver.cloud.backend.pgconfig.catalog.PgconfigCatalogFacade;
import org.geoserver.cloud.backend.pgconfig.config.PgconfigConfigRepository;
import org.geoserver.cloud.backend.pgconfig.config.PgconfigGeoServerFacade;
import org.geoserver.cloud.backend.pgconfig.config.PgconfigUpdateSequence;
import org.geoserver.cloud.backend.pgconfig.resource.PgconfigLockProvider;
import org.geoserver.cloud.backend.pgconfig.support.PgConfigTestContainer;
import org.geoserver.cloud.config.catalog.backend.pgconfig.PgconfigGeoServerLoader;
import org.geoserver.cloud.config.catalog.backend.pgconfig.PgconfigGeoServerResourceLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Test suite for {@link PgconfigBackendAutoConfiguration}
 *
 * @since 1.4
 */
@Testcontainers(disabledWithoutDocker = true)
class PgconfigBackendAutoConfigurationTest {

    @Container static PgConfigTestContainer<?> container = new PgConfigTestContainer<>();

    private ApplicationContextRunner runner =
            new ApplicationContextRunner()
                    .withConfiguration(
                            AutoConfigurations.of(
                                    PgconfigDataSourceAutoConfiguration.class,
                                    PgconfigTransactionManagerAutoConfiguration.class,
                                    PgconfigMigrationAutoConfiguration.class,
                                    PgconfigBackendAutoConfiguration.class));

    @BeforeEach
    void setUp() {
        runner = container.withJdbcUrlConfig(runner);
    }

    @Test
    void testCatalogAndConfigBeans() {
        runner.run(
                context -> {
                    assertThat(context)
                            .hasNotFailed()
                            .hasBean("pgconfigTransactionManager")
                            .hasSingleBean(JdbcTemplate.class)
                            .hasSingleBean(GeoServerConfigurationLock.class)
                            .hasSingleBean(PgconfigUpdateSequence.class)
                            .hasSingleBean(PgconfigCatalogFacade.class)
                            .hasSingleBean(PgconfigGeoServerLoader.class)
                            .hasSingleBean(PgconfigConfigRepository.class)
                            .hasSingleBean(PgconfigGeoServerFacade.class)
                            .hasBean("resourceStoreImpl")
                            .hasSingleBean(PgconfigGeoServerResourceLoader.class)
                            .hasSingleBean(PgconfigLockProvider.class);

                    ExtendedCatalogFacade catalogFacade =
                            context.getBean("catalogFacade", ExtendedCatalogFacade.class);
                    assertThat(catalogFacade).isInstanceOf(PgconfigCatalogFacade.class);
                });
    }
}
