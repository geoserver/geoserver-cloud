/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.configuration.webui;

import org.geoserver.cloud.autoconfigure.web.resource.WebResourceBrowserAutoConfiguration;
import org.geoserver.cloud.core.FilteringXmlBeanDefinitionReader;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

/**
 * Configuration to enable the <a href=
 * "https://docs.geoserver.org/latest/en/user/configuration/tools/resource/browser.html">Resource
 * Browser</a> extension in the Web UI.
 *
 * @see WebResourceBrowserAutoConfiguration
 */
@Configuration
@ImportResource(
    reader = FilteringXmlBeanDefinitionReader.class,
    locations = {"jar:gs-web-resource-.*!/applicationContext.xml"}
)
public class WebResourceBrowserConfiguration {}
