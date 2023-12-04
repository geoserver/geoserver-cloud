/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.web.tools;

import org.geoserver.cloud.autoconfigure.web.core.AbstractWebUIAutoConfiguration;
import org.geoserver.cloud.config.factory.FilteringXmlBeanDefinitionReader;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

/**
 * Configuration to enable the <a href=
 * "https://docs.geoserver.org/latest/en/user/configuration/tools/resource/browser.html">Resource
 * Browser</a> extension in the Web UI.
 *
 * @see ToolsAutoConfiguration
 */
@Configuration
@ConditionalOnClass(name = "org.geoserver.web.resources.PageResourceBrowser")
@ConditionalOnProperty( // enabled by default
        prefix = ToolsAutoConfiguration.CONFIG_PREFIX,
        name = "resource-browser",
        havingValue = "true",
        matchIfMissing = true)
@ImportResource(
        reader = FilteringXmlBeanDefinitionReader.class,
        locations = {"jar:gs-web-resource-.*!/applicationContext.xml"})
public class ResourceBrowserConfiguration extends AbstractWebUIAutoConfiguration {

    static final String CONFIG_PREFIX = ToolsAutoConfiguration.CONFIG_PREFIX + ".resource-browser";

    @Override
    public String getConfigPrefix() {
        return CONFIG_PREFIX;
    }
}
