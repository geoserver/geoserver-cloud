/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.web.demo;

import lombok.Getter;
import org.geoserver.cloud.autoconfigure.web.core.AbstractWebUIAutoConfiguration;
import org.geoserver.cloud.config.factory.FilteringXmlBeanDefinitionReader;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

@Configuration(proxyBeanMethods = true)
@ConditionalOnClass(name = "org.geoserver.wcs.web.demo.WCSRequestBuilder")
@ConditionalOnProperty( // enabled by default
    prefix = DemosAutoConfiguration.CONFIG_PREFIX,
    name = "wcs-request-builder",
    havingValue = "true",
    matchIfMissing = true
)
@ImportResource( //
    reader = FilteringXmlBeanDefinitionReader.class, //
    locations = { //
        "jar:gs-web-wcs-.*!/applicationContext.xml#name=wcsRequestBuilder"
    } //
)
public class WcsRequestBuilderConfiguration extends AbstractWebUIAutoConfiguration {

    static final String CONFIG_PREFIX =
            DemosAutoConfiguration.CONFIG_PREFIX + ".wcs-request-builder";

    private final @Getter String configPrefix = CONFIG_PREFIX;
}
