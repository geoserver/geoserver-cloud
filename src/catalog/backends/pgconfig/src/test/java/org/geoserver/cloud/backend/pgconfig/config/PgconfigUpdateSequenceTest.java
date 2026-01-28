/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.backend.pgconfig.config;

import org.geoserver.cloud.backend.pgconfig.support.PgConfigTestContainer;
import org.geoserver.config.GeoServer;
import org.geoserver.config.plugin.GeoServerImpl;
import org.geoserver.platform.config.UpdateSequence;
import org.geoserver.platform.config.UpdateSequenceConformanceTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * @since 1.4
 */
@Testcontainers(disabledWithoutDocker = true)
@SuppressWarnings("java:S2187")
class PgconfigUpdateSequenceTest implements UpdateSequenceConformanceTest {

    @Container
    static PgConfigTestContainer container = new PgConfigTestContainer();

    private UpdateSequence sequence;
    private PgconfigGeoServerFacade facade;
    private GeoServer geoserver;

    @BeforeEach
    void init() {
        container.setUp();
        facade = new PgconfigGeoServerFacade(container.getTemplate());
        geoserver = new GeoServerImpl(facade);
        sequence = new PgconfigUpdateSequence(container.getDataSource(), facade);
    }

    @AfterEach
    void cleanDb() {
        container.tearDown();
    }

    @Override
    public UpdateSequence getUpdataSequence() {
        return sequence;
    }

    @Override
    public GeoServer getGeoSever() {
        return geoserver;
    }
}
