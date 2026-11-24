/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

class GeoServerServiceConditionTest {

    @Configuration
    static class WmsConditionalConfig {
        @Bean
        @ConditionalOnGeoServerService("wms")
        String wmsMarker() {
            return "wms";
        }
    }

    /**
     * Composed annotation meta-annotated with {@link ConditionalOnGeoServerService}, following the same pattern
     * {@code ConditionalOnGeoServerWMS} and its siblings use to combine the service condition with other conditions.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    @ConditionalOnGeoServerService("wms")
    @interface RequiresWms {}

    /** Composed annotation meta-annotated with {@link ConditionalOnGeoServerService}, see {@link RequiresWms}. */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    @ConditionalOnGeoServerService("wfs")
    @interface RequiresWfs {}

    /**
     * Stacks two composed annotations, each meta-annotated with {@link ConditionalOnGeoServerService} for a different
     * service, on the same bean method. Exercises the code path {@link GeoServerServiceCondition} documents: Spring
     * collects both occurrences of the meta-annotation into a single {@code getAllAnnotationAttributes} call, and the
     * condition must AND every requested service together rather than only seeing one of them.
     */
    @Configuration
    static class WmsAndWfsConditionalConfig {
        @Bean
        @RequiresWms
        @RequiresWfs
        String wmsAndWfsMarker() {
            return "wms-and-wfs";
        }
    }

    private final ApplicationContextRunner runner =
            new ApplicationContextRunner().withUserConfiguration(WmsConditionalConfig.class);

    @Test
    void disabledWhenNeitherFlagNorListPresent() {
        runner.run(context -> assertThat(context).doesNotHaveBean("wmsMarker"));
    }

    @Test
    void enabledByExplicitFlag() {
        runner.withPropertyValues("geoserver.service.wms.enabled=true")
                .run(context -> assertThat(context).hasBean("wmsMarker"));
    }

    @Test
    void enabledByListMembership() {
        runner.withPropertyValues("geoserver.services=wfs,wms")
                .run(context -> assertThat(context).hasBean("wmsMarker"));
    }

    @Test
    void disabledWhenNotInList() {
        runner.withPropertyValues("geoserver.services=wfs,wcs")
                .run(context -> assertThat(context).doesNotHaveBean("wmsMarker"));
    }

    @Test
    void allEnablesEveryService() {
        runner.withPropertyValues("geoserver.services=all")
                .run(context -> assertThat(context).hasBean("wmsMarker"));
    }

    @Test
    void explicitFlagWinsOverList() {
        runner.withPropertyValues("geoserver.services=all", "geoserver.service.wms.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean("wmsMarker"));
    }

    @Test
    void listIsCaseInsensitiveAndTrimmed() {
        runner.withPropertyValues("geoserver.services= WMS , wfs")
                .run(context -> assertThat(context).hasBean("wmsMarker"));
    }

    @Test
    void explicitEmptyListDisablesEverything() {
        runner.withPropertyValues("geoserver.services=")
                .run(context -> assertThat(context).doesNotHaveBean("wmsMarker"));
    }

    private final ApplicationContextRunner wmsAndWfsRunner =
            new ApplicationContextRunner().withUserConfiguration(WmsAndWfsConditionalConfig.class);

    @Test
    void bothServiceAnnotationsOnSameElementMustAllMatch() {
        wmsAndWfsRunner.withPropertyValues("geoserver.services=wms,wfs").run(context -> assertThat(context)
                .hasBean("wmsAndWfsMarker"));
    }

    @Test
    void oneOfTwoServiceAnnotationsOnSameElementNotEnoughToMatch() {
        wmsAndWfsRunner.withPropertyValues("geoserver.services=wms").run(context -> assertThat(context)
                .doesNotHaveBean("wmsAndWfsMarker"));
        wmsAndWfsRunner.withPropertyValues("geoserver.services=wfs").run(context -> assertThat(context)
                .doesNotHaveBean("wmsAndWfsMarker"));
    }

    @Test
    void neitherOfTwoServiceAnnotationsOnSameElementMatch() {
        wmsAndWfsRunner.run(context -> assertThat(context).doesNotHaveBean("wmsAndWfsMarker"));
    }
}
