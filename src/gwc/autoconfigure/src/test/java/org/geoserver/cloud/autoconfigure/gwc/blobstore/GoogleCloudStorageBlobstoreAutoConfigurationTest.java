/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.gwc.blobstore;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import org.geoserver.cloud.autoconfigure.gwc.GeoWebCacheContextRunner;
import org.geoserver.gwc.blob.gcs.web.GcsBlobStoreType;
import org.geoserver.gwc.web.GWCSettingsPage;
import org.geowebcache.storage.blobstore.gcs.GoogleCloudStorageConfigProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

/**
 * {@link GoogleCloudStorageBlobstoreAutoConfiguration} tests
 *
 * @since 1.0
 */
class GoogleCloudStorageBlobstoreAutoConfigurationTest {

    WebApplicationContextRunner runner;

    @TempDir
    File tmpDir;

    @BeforeEach
    void setUp() {
        runner = GeoWebCacheContextRunner.newMinimalGeoWebCacheContextRunner(tmpDir)
                .withConfiguration(AutoConfigurations.of(GoogleCloudStorageBlobstoreAutoConfiguration.class));
    }

    @Test
    void disabledByDefault() {
        runner.run(context -> {
            assertThat(context).doesNotHaveBean(GoogleCloudStorageConfigProvider.class);
            assertThat(context).doesNotHaveBean(GcsBlobStoreType.class);
            assertThat(context).doesNotHaveBean("gwcGcsExtension");
        });
    }

    @Test
    void blobstoreEnabledGeoServerWebUiDisabled() {
        runner.withPropertyValues("gwc.blobstores.gcs=true", "geoserver.web-ui.gwc.enabled=false")
                .run(context -> {
                    assertThat(context).hasSingleBean(GoogleCloudStorageConfigProvider.class);
                    assertThat(context).doesNotHaveBean(GcsBlobStoreType.class);
                    assertThat(context).doesNotHaveBean("gwcGcsExtension");
                });
    }

    @Test
    void blobstoreEnabledGeoServerWebUiEnabled() {
        runner.withPropertyValues("gwc.blobstores.gcs=true", "geoserver.web-ui.gwc.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(GoogleCloudStorageConfigProvider.class);
                    assertThat(context).hasSingleBean(GcsBlobStoreType.class);
                    assertThat(context).hasBean("gwcGcsExtension");
                });
    }

    @Test
    void blobstoreEnabledGeoServerWebUiEnabledGsWebGwcNotInClassPath() {
        runner.withClassLoader(new FilteredClassLoader(GWCSettingsPage.class))
                .withPropertyValues("gwc.blobstores.gcs=true", "geoserver.web-ui.gwc.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(GoogleCloudStorageConfigProvider.class);
                    assertThat(context).doesNotHaveBean(GcsBlobStoreType.class);
                    assertThat(context).doesNotHaveBean("gwcGcsExtension");
                });
    }
}
