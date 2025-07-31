/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.configuration.community.graticule;

import org.geoserver.spring.config.annotations.TranspileXmlConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Configuration for Graticule extension that provides a data store for
 * graticule lines.
 *
 * <p>
 * This autoconfiguration class enables the Graticule extension WebUI components
 *
 * @since 2.27.0
 */
@Configuration(proxyBeanMethods = false)
@TranspileXmlConfig(locations = "jar:gs-graticule-.*!/applicationContext.xml")
@Import(GraticuleWebUIConfiguration_Generated.class)
public class GraticuleWebUIConfiguration {}
