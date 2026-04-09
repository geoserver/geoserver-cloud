/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.configuration.core.wcs;

import org.geoserver.spring.config.annotations.TranspileXmlConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Transpiles {@literal gs-wcs2_0.jar!/applicationContext.xml} to {@link WCS2_0Configuration_Generated}
 *
 * @see WCS2_0Configuration_Generated
 */
@Configuration(proxyBeanMethods = false)
@Import(WCS20Configuration_Generated.class)
@TranspileXmlConfig(locations = "jar:gs-wcs2_0-.*!/applicationContext.xml")
@SuppressWarnings("java:S101")
public class WCS20Configuration {}
