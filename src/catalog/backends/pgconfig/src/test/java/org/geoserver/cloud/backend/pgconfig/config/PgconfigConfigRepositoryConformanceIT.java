/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/*
 * /* (c) 2014 Open Source Geospatial Foundation - all rights reserved (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.backend.pgconfig.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.cloud.backend.pgconfig.PgconfigBackendBuilder;
import org.geoserver.cloud.backend.pgconfig.support.PgConfigTestContainer;
import org.geoserver.cloud.backend.pgconfig.support.PgconfigTestDatabaseSupport;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerConfigConformanceTest;
import org.geoserver.config.ServiceInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** @since 1.4 */
@Testcontainers(disabledWithoutDocker = true)
@Execution(value = ExecutionMode.CONCURRENT)
class PgconfigConfigRepositoryConformanceIT extends GeoServerConfigConformanceTest {

    @Container
    static PgConfigTestContainer container = new PgConfigTestContainer();

    @RegisterExtension
    PgconfigTestDatabaseSupport db = new PgconfigTestDatabaseSupport(container);

    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();
    }

    protected @Override GeoServer createGeoServer() {
        PgconfigBackendBuilder builder = new PgconfigBackendBuilder(db.getDataSource());
        Catalog catalog = builder.createCatalog();
        return builder.createGeoServer(catalog);
    }

    @Test
    void testAddDuplicateServiceToWorkspace() {
        // Make a workspace
        WorkspaceInfo ws1 = geoServer.getCatalog().getFactory().createWorkspace();
        ws1.setName("TEST-WORKSPACE-1");
        geoServer.getCatalog().add(ws1);

        // Make a service for that workspace
        ServiceInfo service1 = createService();
        service1.setWorkspace(ws1);
        service1.setName("TEST-OWS");
        service1.setTitle("Service for WS1");
        geoServer.add(service1);

        ServiceInfo dupTypeAndWorkspace = createService();
        dupTypeAndWorkspace.setWorkspace(ws1);
        dupTypeAndWorkspace.setName("TEST-OWS");
        dupTypeAndWorkspace.setTitle("Service for WS1");

        IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> geoServer.add(dupTypeAndWorkspace));

        assertThat(ex.getMessage())
                .contains("service with name 'TEST-OWS' already exists in workspace 'TEST-WORKSPACE-1'");
    }
}
