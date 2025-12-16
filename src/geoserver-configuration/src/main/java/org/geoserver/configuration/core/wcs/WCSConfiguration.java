/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.configuration.core.wcs;

import org.geoserver.spring.config.annotations.TranspileXmlConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @see WCSCoreConfiguration
 * @see WCS1_0_Configuration
 * @see WCS1_1_Configuration
 * @see WCS2_0_Configuration
 */
@Configuration(proxyBeanMethods = false)
@TranspileXmlConfig(
        locations = "jar:gs-wcs-.*!/applicationContext.xml",
        targetClass = "WCSCoreConfiguration",
        publicAccess = true)
@TranspileXmlConfig(
        locations = "jar:gs-wcs1_0-.*!/applicationContext.xml",
        targetClass = "WCS1_0_Configuration",
        publicAccess = true)
@TranspileXmlConfig(
        locations = "jar:gs-wcs1_1-.*!/applicationContext.xml",
        targetClass = "WCS1_1_Configuration",
        publicAccess = true)
@TranspileXmlConfig(
        locations = "jar:gs-wcs2_0-.*!/applicationContext.xml",
        targetClass = "WCS2_0_Configuration",
        publicAccess = true)
@Import({WCSCoreConfiguration.class, WCS1_0_Configuration.class, WCS1_1_Configuration.class, WCS2_0_Configuration.class
})
public class WCSConfiguration {}
