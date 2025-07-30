/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.configuration.core.wfs;

import org.geoserver.spring.config.annotations.TranspileXmlConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Transpiled XML configuration from {@literal jar:gs-wfs-.*!/applicationContext.xml}
 */
@Configuration
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
@TranspileXmlConfig(locations = "jar:gs-wfs-.*!/applicationContext.xml", publicAccess = true)
@Import(WfsConfiguration_Generated.class)
public class WfsConfiguration {}
