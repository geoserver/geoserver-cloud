/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.gwc.blobstore;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import org.geoserver.cloud.autoconfigure.gwc.GeoWebCacheContextRunner;
import org.geoserver.gwc.web.blob.S3BlobStoreType;
import org.geoserver.platform.ModuleStatusImpl;
import org.geowebcache.s3.S3BlobStoreConfigProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

/**
 * {@link S3BlobstoreAutoConfiguration} tests
 *
 * @since 1.0
 */
class S3BlobstoreAutoConfigurationTest {

    WebApplicationContextRunner runner;

    @TempDir File tmpDir;

    @BeforeEach
    void setUp() throws Exception {
        runner =
                GeoWebCacheContextRunner.newMinimalGeoWebCacheContextRunner(tmpDir)
                        .withConfiguration(
                                AutoConfigurations.of(S3BlobstoreAutoConfiguration.class));
    }

    public @Test void disabledByDefault() {
        runner.run(
                context -> {
                    assertFalse(context.containsBean("S3BlobStoreConfigProvider"));
                    assertFalse(context.containsBean("S3BlobStoreType"));
                    assertFalse(context.containsBean("GWC-S3Extension"));
                });
    }

    public @Test void blobstoreEnabledWebUiDisabled() {
        runner.withPropertyValues("gwc.blobstores.s3=true", "geoserver.web-ui.gwc.enabled=false")
                .run(
                        context -> {
                            assertTrue(
                                    context.isTypeMatch(
                                            "S3BlobStoreConfigProvider",
                                            S3BlobStoreConfigProvider.class));
                            assertFalse(context.containsBean("S3BlobStoreType"));
                            assertFalse(context.containsBean("GWC-S3Extension"));
                        });
    }

    public @Test void enabled() {
        runner.withPropertyValues("gwc.blobstores.s3=true", "geoserver.web-ui.gwc.enabled=true")
                .run(
                        context -> {
                            assertTrue(
                                    context.isTypeMatch(
                                            "S3BlobStoreConfigProvider",
                                            S3BlobStoreConfigProvider.class));
                            assertTrue(
                                    context.isTypeMatch("S3BlobStoreType", S3BlobStoreType.class));
                            assertTrue(
                                    context.isTypeMatch("GWC-S3Extension", ModuleStatusImpl.class));
                        });
    }
}
