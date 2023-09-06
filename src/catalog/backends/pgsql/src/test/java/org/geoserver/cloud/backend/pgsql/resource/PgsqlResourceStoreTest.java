/*
 * (c) 2023 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.backend.pgsql.resource;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.integration.support.locks.LockRegistry;

/**
 * @since 1.4
 */
@Disabled
class PgsqlResourceStoreTest {

    private PgsqlResourceStore store;

    /**
     * @throws java.lang.Exception
     */
    @BeforeEach
    void setUp() throws Exception {
        LockRegistry registry;
        //        PgsqlLockProvider lockProvider = new PgsqlLockProvider(registry);
        //        store = new PgsqlResourceStore(new JdbcTemplate(dataSource), lockProvider);
    }

    /**
     * Test method for {@link
     * org.geoserver.cloud.backend.pgsql.resource.PgsqlResourceStore#get(java.lang.String)}.
     */
    @Test
    void testGet() {
        fail("Not yet implemented");
    }

    /**
     * Test method for {@link
     * org.geoserver.cloud.backend.pgsql.resource.PgsqlResourceStore#save(org.geoserver.cloud.backend.pgsql.resource.PgsqlResource,
     * byte[])}.
     */
    @Test
    void testSave() {
        fail("Not yet implemented");
    }

    /**
     * Test method for {@link
     * org.geoserver.cloud.backend.pgsql.resource.PgsqlResourceStore#remove(java.lang.String)}.
     */
    @Test
    void testRemove() {
        fail("Not yet implemented");
    }

    /**
     * Test method for {@link
     * org.geoserver.cloud.backend.pgsql.resource.PgsqlResourceStore#move(java.lang.String,
     * java.lang.String)}.
     */
    @Test
    void testMove() {
        fail("Not yet implemented");
    }

    /**
     * Test method for {@link
     * org.geoserver.cloud.backend.pgsql.resource.PgsqlResourceStore#getResourceNotificationDispatcher()}.
     */
    @Test
    void testGetResourceNotificationDispatcher() {
        fail("Not yet implemented");
    }

    /**
     * Test method for {@link
     * org.geoserver.cloud.backend.pgsql.resource.PgsqlResourceStore#contents(org.geoserver.cloud.backend.pgsql.resource.PgsqlResource)}.
     */
    @Test
    void testContents() {
        fail("Not yet implemented");
    }

    /**
     * Test method for {@link
     * org.geoserver.cloud.backend.pgsql.resource.PgsqlResourceStore#delete(org.geoserver.cloud.backend.pgsql.resource.PgsqlResource)}.
     */
    @Test
    void testDelete() {
        fail("Not yet implemented");
    }

    /**
     * Test method for {@link
     * org.geoserver.cloud.backend.pgsql.resource.PgsqlResourceStore#list(org.geoserver.cloud.backend.pgsql.resource.PgsqlResource)}.
     */
    @Test
    void testList() {
        fail("Not yet implemented");
    }

    /**
     * Test method for {@link
     * org.geoserver.cloud.backend.pgsql.resource.PgsqlResourceStore#move(org.geoserver.cloud.backend.pgsql.resource.PgsqlResource,
     * org.geoserver.platform.resource.Resource)}.
     */
    @Test
    void testRename() {
        fail("Not yet implemented");
    }

    /**
     * Test method for {@link
     * org.geoserver.cloud.backend.pgsql.resource.PgsqlResourceStore#asFile(org.geoserver.cloud.backend.pgsql.resource.PgsqlResource)}.
     */
    @Test
    void testAsFile() {
        fail("Not yet implemented");
    }

    /**
     * Test method for {@link
     * org.geoserver.cloud.backend.pgsql.resource.PgsqlResourceStore#asDir(org.geoserver.cloud.backend.pgsql.resource.PgsqlResource)}.
     */
    @Test
    void testAsDir() {
        fail("Not yet implemented");
    }

    /**
     * Test method for {@link
     * org.geoserver.cloud.backend.pgsql.resource.PgsqlResourceStore#getLockProvider()}.
     */
    @Test
    void testGetLockProvider() {
        fail("Not yet implemented");
    }
}
