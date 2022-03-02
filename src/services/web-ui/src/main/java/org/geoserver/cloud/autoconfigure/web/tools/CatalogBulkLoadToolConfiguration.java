/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.web.tools;

import lombok.Getter;

import org.geoserver.cloud.autoconfigure.web.core.AbstractWebUIAutoConfiguration;
import org.geoserver.cloud.config.factory.FilteringXmlBeanDefinitionReader;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

@Configuration
@ConditionalOnClass(name = "org.geoserver.web.catalogstresstool.CatalogStressTester")
@ConditionalOnProperty( // enabled by default
        prefix = ToolsAutoConfiguration.CONFIG_PREFIX,
        name = "catalog-bulk-load",
        havingValue = "true",
        matchIfMissing = true)
@ImportResource(
        reader = FilteringXmlBeanDefinitionReader.class,
        locations = {"jar:gs-web-demo-.*!/applicationContext.xml#name=CatalogStresser"})
public class CatalogBulkLoadToolConfiguration extends AbstractWebUIAutoConfiguration {

    static final String CONFIG_PREFIX = ToolsAutoConfiguration.CONFIG_PREFIX + ".catalog-bulk-load";

    private final @Getter String configPrefix = CONFIG_PREFIX;
}
