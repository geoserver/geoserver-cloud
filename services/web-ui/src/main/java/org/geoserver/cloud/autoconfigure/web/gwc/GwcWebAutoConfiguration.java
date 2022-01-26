/*
 * (c) 2021 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.web.gwc;

import lombok.Getter;
import org.geoserver.cloud.autoconfigure.web.core.AbstractWebUIAutoConfiguration;
import org.geoserver.cloud.config.factory.FilteringXmlBeanDefinitionReader;
import org.geoserver.gwc.web.GWCSettingsPage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

@Configuration
@ConditionalOnClass(GWCSettingsPage.class)
@ConditionalOnProperty( // enabled by default
    prefix = GwcWebAutoConfiguration.CONFIG_PREFIX,
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
@ImportResource( //
    reader = FilteringXmlBeanDefinitionReader.class,
    locations = {"jar:gs-web-gwc-.*!/applicationContext.xml"}
)
public class GwcWebAutoConfiguration extends AbstractWebUIAutoConfiguration {

    static final String CONFIG_PREFIX = "geoserver.web-ui.gwc";

    private final @Getter String configPrefix = CONFIG_PREFIX;
}
