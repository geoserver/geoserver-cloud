/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.configuration.core.web.wms;

import org.geoserver.spring.config.annotations.TranspileXmlConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration(proxyBeanMethods = false)
@TranspileXmlConfig(locations = "jar:gs-web-wms-.*!/applicationContext.xml")
@Import({WMSWebConfiguration_Generated.class})
public class WebWMSConfiguration {}
