/*
 * (c) 2025 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.web.extension.inspire;

import org.geoserver.cloud.autoconfigure.web.core.AbstractWebUIAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ConditionalOnClass(name = "org.geoserver.inspire.web.InspireAdminPanel")
@ConditionalOnProperty(
        prefix = InspireAutoConfiguration.CONFIG_PREFIX,
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
@Import(InspireConfiguration.class)
public class InspireAutoConfiguration extends AbstractWebUIAutoConfiguration {

    static final String CONFIG_PREFIX = "geoserver.extension.inspire";

    @Override
    public String getConfigPrefix() {
        return CONFIG_PREFIX;
    }
}
