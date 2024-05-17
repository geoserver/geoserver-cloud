/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.gwc.blobstore;

import static org.assertj.core.api.Assertions.assertThat;

import org.geoserver.cloud.autoconfigure.gwc.GeoWebCacheContextRunner;
import org.geoserver.gwc.web.GWCSettingsPage;
import org.geoserver.gwc.web.blob.AzureBlobStoreType;
import org.geowebcache.azure.AzureBlobStoreConfigProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import java.io.File;

/**
 * {@link AzureBlobstoreAutoConfiguration} tests
 *
 * @since 1.0
 */
class AzureBlobstoreAutoConfigurationTest {

    WebApplicationContextRunner runner;

    @TempDir File tmpDir;

    @BeforeEach
    void setUp() {
        runner =
                GeoWebCacheContextRunner.newMinimalGeoWebCacheContextRunner(tmpDir)
                        .withConfiguration(
                                AutoConfigurations.of(AzureBlobstoreAutoConfiguration.class));
    }

    @Test
    void disabledByDefault() {
        runner.run(
                context -> {
                    assertThat(context).doesNotHaveBean(AzureBlobStoreConfigProvider.class);
                    assertThat(context).doesNotHaveBean(AzureBlobStoreType.class);
                    assertThat(context).doesNotHaveBean("gwcAzureExtension");
                });
    }

    @Test
    void blobstoreEnabledGeoServerWebUiDisabled() {
        runner.withPropertyValues("gwc.blobstores.azure=true", "geoserver.web-ui.gwc.enabled=false")
                .run(
                        context -> {
                            assertThat(context).hasSingleBean(AzureBlobStoreConfigProvider.class);
                            assertThat(context).doesNotHaveBean(AzureBlobStoreType.class);
                            assertThat(context).doesNotHaveBean("gwcAzureExtension");
                        });
    }

    @Test
    void blobstoreEnabledGeoServerWebUiEnabled() {
        runner.withPropertyValues("gwc.blobstores.azure=true", "geoserver.web-ui.gwc.enabled=true")
                .run(
                        context -> {
                            assertThat(context).hasSingleBean(AzureBlobStoreConfigProvider.class);
                            assertThat(context).hasSingleBean(AzureBlobStoreType.class);
                            assertThat(context).hasBean("gwcAzureExtension");
                        });
    }

    @Test
    void blobstoreEnabledGeoServerWebUiEnabledGsWebGwcNotInClassPath() {
        runner.withClassLoader(new FilteredClassLoader(GWCSettingsPage.class))
                .withPropertyValues(
                        "gwc.blobstores.azure=true", "geoserver.web-ui.gwc.enabled=true")
                .run(
                        context -> {
                            assertThat(context).hasSingleBean(AzureBlobStoreConfigProvider.class);
                            assertThat(context).doesNotHaveBean(AzureBlobStoreType.class);
                            assertThat(context).doesNotHaveBean("gwcAzureExtension");
                        });
    }
}
