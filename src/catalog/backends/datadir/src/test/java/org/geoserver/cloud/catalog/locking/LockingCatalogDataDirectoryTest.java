/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.locking;

import org.geoserver.GeoServerConfigurationLock;
import org.geoserver.cloud.config.catalog.backend.datadirectory.NoServletContextFileLockProvider;
import org.geoserver.platform.resource.LockProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

/**
 * @since 1.0
 */
public class LockingCatalogDataDirectoryTest extends LockingCatalogTest {

    @TempDir File tmpDir;

    static @BeforeAll void beforeAll() {
        System.setProperty("CONFIGURATION_TRYLOCK_TIMEOUT", "100");
    }

    static @AfterAll void afterAll() {
        System.clearProperty("CONFIGURATION_TRYLOCK_TIMEOUT");
    }

    protected @Override GeoServerConfigurationLock createConfigLock() {
        LockProvider lockProvider = new NoServletContextFileLockProvider(tmpDir);
        return new LockProviderGeoServerConfigurationLock(lockProvider);
    }
}
