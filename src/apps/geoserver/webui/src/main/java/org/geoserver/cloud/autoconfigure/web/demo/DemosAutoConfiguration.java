/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.web.demo;

import org.geoserver.cloud.autoconfigure.web.core.AbstractWebUIAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ConditionalOnClass(name = "org.geoserver.web.demo.DemoRequest")
@ConditionalOnProperty( // enabled by default
        prefix = DemosAutoConfiguration.CONFIG_PREFIX,
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
@Import({
    LayerPreviewConfiguration.class,
    DemoRequestsConfiguration.class,
    ReprojectionConsoleConfiguration.class,
    SrsListConfiguration.class,
    WcsRequestBuilderConfiguration.class,
    WpsRequestBuilderConfiguration.class
})
public class DemosAutoConfiguration extends AbstractWebUIAutoConfiguration {

    static final String CONFIG_PREFIX = "geoserver.web-ui.demos";

    @Override
    public String getConfigPrefix() {
        return CONFIG_PREFIX;
    }
}
