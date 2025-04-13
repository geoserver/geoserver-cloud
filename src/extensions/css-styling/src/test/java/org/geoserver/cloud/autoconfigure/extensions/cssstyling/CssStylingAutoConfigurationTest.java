/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.extensions.cssstyling;

import static org.assertj.core.api.Assertions.assertThat;

import org.geoserver.catalog.SLDHandler;
import org.geoserver.community.css.web.CssHandler;
import org.geoserver.config.GeoServer;
import org.geoserver.config.impl.GeoServerImpl;
import org.geoserver.platform.GeoServerExtensions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

/**
 * Test suite for {@link CssStylingAutoConfiguration}
 */
class CssStylingAutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            // dependency beans that are always available
            .withBean("extensions", GeoServerExtensions.class)
            .withBean("geoServer", GeoServer.class, GeoServerImpl::new)
            .withBean("sldHandler", SLDHandler.class)
            .withConfiguration(AutoConfigurations.of(CssStylingAutoConfiguration.class));

    @Test
    void testEnabledByDefault() {
        contextRunner.run(context -> assertThat(context).hasSingleBean(CssHandler.class));
    }

    @Test
    void testExplicitlyEnabled() {
        contextRunner
                .withPropertyValues("geoserver.extension.css-styling.enabled=true")
                .run(context -> assertThat(context).hasSingleBean(CssHandler.class));
    }

    @Test
    void testDisabledModuleStatus() {
        contextRunner
                .withPropertyValues("geoserver.extension.css-styling.enabled=false")
                .run(context -> {
                    assertThat(context)
                            .doesNotHaveBean(CssHandler.class)
                            .hasBean("cssDisabledModuleStatus")
                            .getBean("cssDisabledModuleStatus")
                            .hasFieldOrPropertyWithValue("enabled", false)
                            .hasFieldOrPropertyWithValue("available", true);
                });
    }
}
