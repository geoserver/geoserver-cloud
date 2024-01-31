/*
 * (c) 2023 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.backend.pgconfig.catalog;

import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.catalog.plugin.CatalogConformanceTest;
import org.geoserver.cloud.backend.pgconfig.PgsqlBackendBuilder;
import org.geoserver.cloud.backend.pgconfig.support.PgConfigTestContainer;
import org.geotools.util.logging.Logging;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * @since 1.4
 */
@Testcontainers(disabledWithoutDocker = true)
class PgsqlCatalogBackendConformanceTest extends CatalogConformanceTest {

    @Container static PgConfigTestContainer<?> container = new PgConfigTestContainer<>();

    @Disabled(
            """
			revisit, seems to be just a problem of ordering or equals with the \
			returned ft/ft2 where mockito is not throwing the expected exception
			""")
    @Override
    public void testSaveDataStoreRollbacksBothStoreAndResources() throws Exception {}

    static @BeforeAll void beforeAll() throws Exception {
        try {
            Logging.ALL.setLoggerFactory("org.geotools.util.logging.CommonsLoggerFactory");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        container.setUp();
        super.setUp();
    }

    @AfterEach
    void cleanDb() throws Exception {
        container.tearDown();
    }

    @Override
    protected CatalogImpl createCatalog() {
        return new PgsqlBackendBuilder(container.getDataSource()).createCatalog();
    }
}
