/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.gwc.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import org.geoserver.cloud.autoconfigure.gwc.GeoWebCacheContextRunner;
import org.geowebcache.io.codec.ImageDecoderContainer;
import org.geowebcache.io.codec.ImageEncoderContainer;
import org.geowebcache.service.wms.WMSService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

/**
 * @since 2.28.5
 */
class WebMapServiceAutoConfigurationTest {

    @TempDir
    File tmpDir;

    WebApplicationContextRunner runner;

    @BeforeEach
    void setUp() {
        runner = GeoWebCacheContextRunner.newMinimalGeoWebCacheContextRunner(tmpDir)
                .withConfiguration(AutoConfigurations.of(WebMapServiceAutoConfiguration.class));
    }

    @Test
    void disabledByDefault() {
        runner.run(context -> assertThat(context).hasNotFailed().doesNotHaveBean(WMSService.class));
    }

    /**
     * The image codec beans upstream declares along with this service come from {@code
     * ImageCodecsConfiguration} instead, since GeoWebCache needs them either way.
     */
    @Test
    void enabledKeepsASingleImageCodecSet() {
        runner.withPropertyValues("gwc.services.wms=true").run(context -> assertThat(context)
                .hasNotFailed()
                .hasBean("gwcServiceWMSTarget")
                .hasSingleBean(ImageDecoderContainer.class)
                .hasSingleBean(ImageEncoderContainer.class));
    }
}
