/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.web.tools;

import org.geoserver.cloud.autoconfigure.web.core.AbstractWebUIAutoConfiguration;
import org.geoserver.cloud.config.factory.ImportFilteredResource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(name = "org.geoserver.web.catalogstresstool.CatalogStressTester")
@ConditionalOnProperty( // enabled by default
        prefix = ToolsAutoConfiguration.CONFIG_PREFIX,
        name = "reprojection-console",
        havingValue = "true",
        matchIfMissing = true)
@ImportFilteredResource("jar:gs-web-demo-.*!/applicationContext.xml#name=reprojectionConsole")
public class ReprojectionConsoleConfiguration extends AbstractWebUIAutoConfiguration {

    static final String CONFIG_PREFIX = ToolsAutoConfiguration.CONFIG_PREFIX + ".reprojection-console";

    @Override
    public String getConfigPrefix() {
        return CONFIG_PREFIX;
    }
}
