/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.gwc.blobstore;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import org.geoserver.cloud.autoconfigure.gwc.GeoWebCacheContextRunner;
import org.geoserver.gwc.web.blob.AzureBlobStoreType;
import org.geowebcache.azure.AzureBlobStoreConfigProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

/**
 * {@link AzureBlobstoreAutoConfiguration} tests
 *
 * @since 1.0
 */
class AzureBlobstoreAutoConfigurationTest {

    WebApplicationContextRunner runner;

    @TempDir File tmpDir;

    @BeforeEach
    void setUp() throws Exception {
        runner =
                GeoWebCacheContextRunner.newMinimalGeoWebCacheContextRunner(tmpDir)
                        .withConfiguration(
                                AutoConfigurations.of(AzureBlobstoreAutoConfiguration.class));
    }

    public @Test void disabledByDefault() {
        runner.run(
                context -> {
                    assertFalse(context.containsBean("AzureBlobStoreConfigProvider"));
                    assertFalse(context.containsBean("AzureBlobStoreType"));
                });
    }

    public @Test void blobstoreEnabledWebUiDisabled() {
        runner.withPropertyValues("gwc.blobstores.azure=true", "geoserver.web-ui.gwc.enabled=false")
                .run(
                        context -> {
                            assertTrue(
                                    context.isTypeMatch(
                                            "AzureBlobStoreConfigProvider",
                                            AzureBlobStoreConfigProvider.class));
                            assertFalse(context.containsBean("AzureBlobStoreType"));
                        });
    }

    public @Test void enabled() {
        runner.withPropertyValues("gwc.blobstores.azure=true", "geoserver.web-ui.gwc.enabled=true")
                .run(
                        context -> {
                            assertTrue(
                                    context.isTypeMatch(
                                            "AzureBlobStoreConfigProvider",
                                            AzureBlobStoreConfigProvider.class));
                            assertTrue(
                                    context.isTypeMatch(
                                            "AzureBlobStoreType", AzureBlobStoreType.class));
                        });
    }
}
