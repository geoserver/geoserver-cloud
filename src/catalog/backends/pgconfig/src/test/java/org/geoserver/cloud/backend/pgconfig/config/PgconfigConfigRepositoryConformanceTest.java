/*
 * /* (c) 2014 Open Source Geospatial Foundation - all rights reserved (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.backend.pgconfig.config;

import org.geoserver.catalog.Catalog;
import org.geoserver.cloud.backend.pgconfig.PgconfigBackendBuilder;
import org.geoserver.cloud.backend.pgconfig.support.PgConfigTestContainer;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerConfigConformanceTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * @since 1.4
 */
@Testcontainers(disabledWithoutDocker = true)
@SuppressWarnings("java:S2187")
class PgconfigConfigRepositoryConformanceTest extends GeoServerConfigConformanceTest {

    @Container static PgConfigTestContainer<?> container = new PgConfigTestContainer<>();

    @Override
    @BeforeEach
    public void setUp() {
        container.setUp();
        super.setUp();
    }

    @AfterEach
    void cleanDb() {
        container.tearDown();
    }

    protected @Override GeoServer createGeoServer() {
        PgconfigBackendBuilder builder = new PgconfigBackendBuilder(container.getDataSource());
        Catalog catalog = builder.createCatalog();
        return builder.createGeoServer(catalog);
    }
}
