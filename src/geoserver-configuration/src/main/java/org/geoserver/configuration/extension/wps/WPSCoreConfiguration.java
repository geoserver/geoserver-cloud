/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.configuration.extension.wps;

import org.geoserver.cloud.config.factory.ImportFilteredResource;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ImportFilteredResource({"jar:gs-wps-.*!/applicationContext.xml"})
public class WPSCoreConfiguration {}
