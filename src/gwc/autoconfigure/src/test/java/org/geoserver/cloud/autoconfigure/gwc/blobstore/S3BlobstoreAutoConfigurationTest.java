/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.gwc.blobstore;

import static org.assertj.core.api.Assertions.assertThat;

import org.geoserver.cloud.autoconfigure.gwc.GeoWebCacheContextRunner;
import org.geoserver.gwc.web.GWCSettingsPage;
import org.geoserver.gwc.web.blob.S3BlobStoreType;
import org.geowebcache.s3.S3BlobStoreConfigProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import java.io.File;

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
                    assertThat(context).doesNotHaveBean(S3BlobStoreConfigProvider.class);
                    assertThat(context).doesNotHaveBean(S3BlobStoreType.class);
                    assertThat(context).doesNotHaveBean("GWC-S3Extension");
                });
    }

    public @Test void blobstoreEnabledGeoServerWebUiDisabled() {
        runner.withPropertyValues("gwc.blobstores.s3=true", "geoserver.web-ui.gwc.enabled=false")
                .run(
                        context -> {
                            assertThat(context).hasSingleBean(S3BlobStoreConfigProvider.class);
                            assertThat(context).doesNotHaveBean(S3BlobStoreType.class);
                            assertThat(context).doesNotHaveBean("GWC-S3Extension");
                        });
    }

    public @Test void blobstoreEnabledGeoServerWebUiEnabled() {
        runner.withPropertyValues("gwc.blobstores.s3=true", "geoserver.web-ui.gwc.enabled=true")
                .run(
                        context -> {
                            assertThat(context).hasSingleBean(S3BlobStoreConfigProvider.class);
                            assertThat(context).hasSingleBean(S3BlobStoreType.class);
                            assertThat(context).hasBean("GWC-S3Extension");
                        });
    }

    public @Test void blobstoreEnabledGeoServerWebUiEnabledGsWebGwcNotInClassPath() {
        runner.withClassLoader(new FilteredClassLoader(GWCSettingsPage.class))
                .withPropertyValues("gwc.blobstores.s3=true", "geoserver.web-ui.gwc.enabled=true")
                .run(
                        context -> {
                            assertThat(context).hasSingleBean(S3BlobStoreConfigProvider.class);
                            assertThat(context).doesNotHaveBean("S3BlobStoreType");
                            assertThat(context).doesNotHaveBean("GWC-S3Extension");
                        });
    }
}
