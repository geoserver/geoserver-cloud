/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.ogcapi.features;

import static org.assertj.core.api.Assertions.assertThat;

import org.geoserver.ogcapi.v1.features.FeatureConformance;
import org.geoserver.ogcapi.v1.features.FeatureServiceXStreamPersisterInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * The conformance objects stored in {@code WFSInfo}'s metadata map are read and written by every service, so the
 * initializer that teaches {@code XStreamPersister} about them must not depend on which GeoServer service the
 * application runs. See issue #872.
 */
class OgcApiFeaturesXStreamPersisterAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(OgcApiFeaturesXStreamPersisterAutoConfiguration.class));

    @Test
    void contributedWithoutAnyServiceEnabled() {
        runner.run(context -> assertThat(context)
                .hasNotFailed()
                .hasSingleBean(FeatureServiceXStreamPersisterInitializer.class)
                .hasBean("featureServiceXStreamInitializer"));
    }

    @Test
    void contributedWhenTheExtensionIsDisabled() {
        runner.withPropertyValues("geoserver.extension.ogcapi.features.enabled=false")
                .run(context -> assertThat(context)
                        .as("disabling the extension must not make the service corrupt configuration it wrote")
                        .hasNotFailed()
                        .hasSingleBean(FeatureServiceXStreamPersisterInitializer.class));
    }

    @Test
    void absentWithoutTheOgcApiFeaturesClasses() {
        runner.withClassLoader(new FilteredClassLoader(FeatureConformance.class))
                .run(context -> assertThat(context)
                        .hasNotFailed()
                        .doesNotHaveBean(FeatureServiceXStreamPersisterInitializer.class));
    }
}
