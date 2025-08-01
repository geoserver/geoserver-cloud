/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.configuration.extension.resourcebrowser;

import org.geoserver.cloud.config.factory.ImportFilteredResource;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration to enable the <a href=
 * "https://docs.geoserver.org/latest/en/user/configuration/tools/resource/browser.html">Resource
 * Browser</a> extension in the Web UI.
 *
 * @see WebToolsAutoConfiguration
 */
@Configuration(proxyBeanMethods = false)
@ImportFilteredResource("jar:gs-web-resource-.*!/applicationContext.xml")
public class WebToolsResourceBrowserConfiguration {}
