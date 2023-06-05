/*
 * (c) 2023 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.catalog.backend.pgsql;

import static org.assertj.core.api.Assertions.assertThat;

import org.geoserver.GeoServerConfigurationLock;
import org.geoserver.cloud.backend.pgsql.catalog.PgsqlCatalogFacade;
import org.geoserver.cloud.backend.pgsql.config.PgsqlConfigRepository;
import org.geoserver.cloud.backend.pgsql.config.PgsqlGeoServerFacade;
import org.geoserver.cloud.backend.pgsql.config.PgsqlUpdateSequence;
import org.geoserver.cloud.backend.pgsql.resource.PgsqlLockProvider;
import org.geoserver.cloud.config.catalog.backend.core.CatalogProperties;
import org.geoserver.cloud.config.catalog.backend.pgsql.PgsqlGeoServerLoader;
import org.geoserver.cloud.config.catalog.backend.pgsql.PgsqlGeoServerResourceLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * @since 1.4
 */
@Testcontainers(disabledWithoutDocker = true)
class PgsqlBackendAutoConfigurationTest {

    @Container static PostgreSQLContainer<?> container = new PostgreSQLContainer<>("postgres:15");

    private ApplicationContextRunner runner =
            new ApplicationContextRunner()
                    .withBean(CatalogProperties.class, CatalogProperties::new)
                    .withConfiguration(
                            AutoConfigurations.of(
                                    PgsqlDataSourceAutoConfiguration.class,
                                    PgsqlMigrationAutoConfiguration.class,
                                    PgsqlBackendAutoConfiguration.class));

    @BeforeEach
    void setUp() throws Exception {
        String url = container.getJdbcUrl();
        String username = container.getUsername();
        String password = container.getPassword();
        runner =
                runner.withPropertyValues( //
                        "geoserver.backend.pgconfig.enabled=true", //
                        "geoserver.backend.pgconfig.datasource.url=" + url, //
                        "geoserver.backend.pgconfig.datasource.username=" + username, //
                        "geoserver.backend.pgconfig.datasource.password=" + password //
                        );
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
                            // .hasSingleBean(PgsqlResourceStore.class)
                            .hasSingleBean(PgsqlGeoServerResourceLoader.class)
                            .hasSingleBean(PgsqlLockProvider.class);
                });
    }
}
